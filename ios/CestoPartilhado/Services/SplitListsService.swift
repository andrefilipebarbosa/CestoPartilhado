import Foundation
import FirebaseFirestore
import FirebaseFunctions

/// Toda a lógica das listas divididas. Qualquer membro pode adicionar/remover
/// artigos enquanto a lista estiver desbloqueada; só o dono marca/reverte
/// pagamentos (o que bloqueia/desbloqueia a lista) e só o dono a pode
/// eliminar — tudo também reforçado nas regras do Firestore, não só aqui.
final class SplitListsService {
    private let db = Firestore.firestore()
    private lazy var functions = Functions.functions(region: "europe-west1")

    private func listsRef() -> CollectionReference { db.collection("splitLists") }
    private func itemsRef(_ listId: String) -> CollectionReference {
        listsRef().document(listId).collection("items")
    }

    func observeLists(uid: String, onChange: @escaping ([SplitList]) -> Void) -> ListenerRegistration {
        listsRef()
            .whereField("memberIds", arrayContains: uid)
            .order(by: "updatedAt", descending: true)
            .addSnapshotListener { snapshot, _ in
                onChange(snapshot?.documents.compactMap { try? $0.data(as: SplitList.self) } ?? [])
            }
    }

    func observeListWithItems(listId: String, onChange: @escaping (SplitListWithItems) -> Void) -> ListObservation {
        let observation = ListObservation()
        var currentList: SplitList?
        var items: [SplitItem] = []

        func emit() {
            guard let list = currentList else { return }
            onChange(SplitListWithItems(list: list, items: items))
        }

        observation.listListener = listsRef().document(listId).addSnapshotListener { snap, _ in
            currentList = try? snap?.data(as: SplitList.self)
            emit()
        }
        observation.storesListener = itemsRef(listId).order(by: "createdAt").addSnapshotListener { snap, _ in
            items = snap?.documents.compactMap { try? $0.data(as: SplitItem.self) } ?? []
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
            "paidMemberIds": [],
            "totalValue": 0,
            "itemCount": 0,
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp(),
        ])
        return doc.documentID
    }

    func addItem(listId: String, name: String, value: Double, uid: String) async throws {
        let batch = db.batch()
        batch.setData([
            "name": name,
            "value": value,
            "addedBy": uid,
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: itemsRef(listId).document())
        batch.updateData([
            "itemCount": FieldValue.increment(Int64(1)),
            "totalValue": FieldValue.increment(value),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    func removeItem(listId: String, item: SplitItem) async throws {
        guard let itemId = item.id else { return }
        let batch = db.batch()
        batch.deleteDocument(itemsRef(listId).document(itemId))
        batch.updateData([
            "itemCount": FieldValue.increment(Int64(-1)),
            "totalValue": FieldValue.increment(-item.value),
            "updatedAt": FieldValue.serverTimestamp(),
        ], forDocument: listsRef().document(listId))
        try await batch.commit()
    }

    /// Só tem sucesso nas regras do Firestore se quem chama for o dono da lista.
    func setMemberPaid(listId: String, memberUid: String, paid: Bool) async throws {
        try await listsRef().document(listId).updateData([
            "paidMemberIds": paid ? FieldValue.arrayUnion([memberUid]) : FieldValue.arrayRemove([memberUid]),
            "updatedAt": FieldValue.serverTimestamp(),
        ])
    }

    /// Só tem sucesso nas regras do Firestore se quem chama for o dono da lista.
    func deleteList(listId: String) async throws {
        let itemsSnapshot = try await itemsRef(listId).getDocuments()
        let batch = db.batch()
        itemsSnapshot.documents.forEach { batch.deleteDocument($0.reference) }
        batch.deleteDocument(listsRef().document(listId))
        try await batch.commit()
    }

    func inviteMemberByEmail(listId: String, email: String) async throws -> String {
        let result = try await functions.httpsCallable("inviteMemberByEmail")
            .call(["listId": listId, "email": email, "collection": "splitLists"])
        let data = result.data as? [String: Any]
        return data?["status"] as? String ?? "unknown"
    }
}
