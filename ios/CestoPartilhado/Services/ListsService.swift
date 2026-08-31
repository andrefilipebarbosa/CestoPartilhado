import Foundation
import FirebaseFirestore
import FirebaseFunctions

/// Guarda os listeners de `observeListWithStores` (incluindo os de artigos, criados
/// de forma assíncrona à medida que as lojas chegam) para poderem ser todos removidos
/// de uma vez com `stop()`.
final class ListObservation {
    var listListener: ListenerRegistration?
    var storesListener: ListenerRegistration?
    var itemListeners: [String: ListenerRegistration] = [:]

    func stop() {
        listListener?.remove()
        storesListener?.remove()
        itemListeners.values.forEach { $0.remove() }
        itemListeners.removeAll()
    }
}

/// Toda a lógica de listas/lojas/artigos. Qualquer membro (dono ou convidado) pode
/// ler e escrever lojas/artigos — reforçado pelas regras do Firestore, não só aqui.
/// Só o dono pode eliminar a lista (também reforçado nas regras).
final class ListsService {
    private let db = Firestore.firestore()
    private lazy var functions = Functions.functions(region: "europe-west1")

    private func listsRef() -> CollectionReference { db.collection("lists") }
    private func storesRef(_ listId: String) -> CollectionReference {
        listsRef().document(listId).collection("stores")
    }
    private func itemsRef(_ listId: String, _ storeId: String) -> CollectionReference {
        storesRef(listId).document(storeId).collection("items")
    }

    func observeLists(uid: String, status: String, onChange: @escaping ([ShoppingList]) -> Void) -> ListenerRegistration {
        listsRef()
            .whereField("memberIds", arrayContains: uid)
            .whereField("status", isEqualTo: status)
            .order(by: "updatedAt", descending: true)
            .addSnapshotListener { snapshot, _ in
                let lists = snapshot?.documents.compactMap { try? $0.data(as: ShoppingList.self) } ?? []
                onChange(lists)
            }
    }

    /// Escuta a lista + todas as lojas + todos os artigos, emitindo um `ListWithStores`
    /// combinado sempre que algo muda. Chama `.stop()` no objeto devolvido para limpar
    /// tudo (incluindo os listeners de artigos, criados de forma assíncrona).
    func observeListWithStores(listId: String, onChange: @escaping (ListWithStores) -> Void) -> ListObservation {
        let observation = ListObservation()
        var currentList: ShoppingList?
        var stores: [Store] = []
        var itemsByStore: [String: [ShoppingItem]] = [:]

        func emit() {
            guard let list = currentList else { return }
            let combined = stores.map { store in
                StoreWithItems(store: store, items: itemsByStore[store.id ?? ""] ?? [])
            }
            onChange(ListWithStores(list: list, stores: combined))
        }

        observation.listListener = listsRef().document(listId).addSnapshotListener { snapshot, _ in
            currentList = try? snapshot?.data(as: ShoppingList.self)
            emit()
        }

        observation.storesListener = storesRef(listId).order(by: "order").addSnapshotListener { [weak self] snapshot, _ in
            guard let self else { return }
            stores = snapshot?.documents.compactMap { try? $0.data(as: Store.self) } ?? []
            let currentIds = Set(stores.compactMap { $0.id })

            for (storeId, listener) in observation.itemListeners where !currentIds.contains(storeId) {
                listener.remove()
                observation.itemListeners.removeValue(forKey: storeId)
                itemsByStore.removeValue(forKey: storeId)
            }

            for store in stores {
                guard let storeId = store.id, observation.itemListeners[storeId] == nil else { continue }
                observation.itemListeners[storeId] = self.itemsRef(listId, storeId).order(by: "createdAt").addSnapshotListener { itemsSnapshot, _ in
                    itemsByStore[storeId] = itemsSnapshot?.documents.compactMap { try? $0.data(as: ShoppingItem.self) } ?? []
                    emit()
                }
            }
            emit()
        }

        return observation
    }

    func createList(name: String, ownerUid: String) async throws -> String {
        let doc = listsRef().document()
        try await doc.setData([
            "name": name,
            "ownerId": ownerUid,
            "memberIds": [ownerUid],
            "pendingInvites": [],
            "status": ShoppingList.statusActive,
            "storeCount": 0,
            "itemCount": 0,
            "boughtCount": 0,
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp(),
        ])
        return doc.documentID
    }

    func addStore(listId: String, name: String, order: Int) async throws {
        let batch = db.batch()
        batch.setData([
            "name": name,
            "order": order,
            "createdAt": FieldValue.serverTimestamp(),
        ], forDocument: storesRef(listId).document())
        batch.updateData([
            "storeCount": FieldValue.increment(Int64(1)),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    func removeStore(listId: String, storeId: String) async throws {
        let itemsSnapshot = try await itemsRef(listId, storeId).getDocuments()
        let boughtInStore = itemsSnapshot.documents.filter { ($0.data()["bought"] as? Bool) == true }.count

        let batch = db.batch()
        itemsSnapshot.documents.forEach { batch.deleteDocument($0.reference) }
        batch.deleteDocument(storesRef(listId).document(storeId))
        batch.updateData([
            "storeCount": FieldValue.increment(Int64(-1)),
            "itemCount": FieldValue.increment(Int64(-itemsSnapshot.documents.count)),
            "boughtCount": FieldValue.increment(Int64(-boughtInStore)),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    func addItem(listId: String, storeId: String, name: String, note: String, uid: String) async throws {
        let batch = db.batch()
        batch.setData([
            "name": name,
            "note": note,
            "bought": false,
            "addedBy": uid,
            "boughtBy": NSNull(),
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: itemsRef(listId, storeId).document())
        batch.updateData([
            "itemCount": FieldValue.increment(Int64(1)),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    func setItemBought(listId: String, storeId: String, itemId: String, bought: Bool, uid: String) async throws {
        let batch = db.batch()
        batch.updateData([
            "bought": bought,
            "boughtBy": bought ? uid : NSNull(),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: itemsRef(listId, storeId).document(itemId))
        batch.updateData([
            "boughtCount": FieldValue.increment(Int64(bought ? 1 : -1)),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    func removeItem(listId: String, storeId: String, itemId: String, wasBought: Bool) async throws {
        let batch = db.batch()
        batch.deleteDocument(itemsRef(listId, storeId).document(itemId))
        batch.updateData([
            "itemCount": FieldValue.increment(Int64(-1)),
            "boughtCount": FieldValue.increment(Int64(wasBought ? -1 : 0)),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    func setListStatus(listId: String, status: String) async throws {
        var updates: [String: Any] = [
            "status": status,
            "updatedAt": FieldValue.serverTimestamp(),
        ]
        updates["closedAt"] = status == ShoppingList.statusClosed ? FieldValue.serverTimestamp() : NSNull()
        try await listsRef().document(listId).updateData(updates)
    }

    /// Só têm sucesso nas regras do Firestore se quem chama for o dono da lista.
    func deleteList(listId: String) async throws {
        let storesSnapshot = try await storesRef(listId).getDocuments()
        for storeDoc in storesSnapshot.documents {
            let itemsSnapshot = try await itemsRef(listId, storeDoc.documentID).getDocuments()
            let batch = db.batch()
            itemsSnapshot.documents.forEach { batch.deleteDocument($0.reference) }
            batch.deleteDocument(storeDoc.reference)
            try await batch.commit()
        }
        try await listsRef().document(listId).delete()
    }

    func inviteMemberByEmail(listId: String, email: String) async throws -> String {
        let result = try await functions.httpsCallable("inviteMemberByEmail").call(["listId": listId, "email": email])
        let data = result.data as? [String: Any]
        return data?["status"] as? String ?? "unknown"
    }

    func inviteLink(for listId: String) -> String {
        "cestopartilhado://join?listId=\(listId)"
    }
}
