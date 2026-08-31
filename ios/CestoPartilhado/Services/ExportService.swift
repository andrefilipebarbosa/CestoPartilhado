import Foundation
import FirebaseFirestore

/// Constrói um relatório em texto simples com TODAS as listas a que o utilizador
/// tem acesso (próprias e partilhadas), incluindo lojas e artigos — para a opção
/// "Exportar os meus dados" nas Definições.
final class ExportService {
    private let db = Firestore.firestore()

    func buildExportText(uid: String, accountEmail: String) async throws -> String {
        let activeLists = try await fetchLists(uid: uid, status: ShoppingList.statusActive)
        let closedLists = try await fetchLists(uid: uid, status: ShoppingList.statusClosed)

        var lines: [String] = []
        lines.append(L("export_header"))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        lines.append(String(format: L("export_generated_at"), formatter.string(from: Date())))
        lines.append(String(format: L("export_account"), accountEmail))
        lines.append("")

        if activeLists.isEmpty && closedLists.isEmpty {
            lines.append(L("export_no_lists"))
            return lines.joined(separator: "\n")
        }

        if !activeLists.isEmpty {
            lines.append("== \(L("export_active_lists")) ==")
            for list in activeLists { try await lines.append(contentsOf: appendList(list, uid: uid)) }
        }
        if !closedLists.isEmpty {
            lines.append("== \(L("export_closed_lists")) ==")
            for list in closedLists { try await lines.append(contentsOf: appendList(list, uid: uid)) }
        }
        return lines.joined(separator: "\n")
    }

    private func appendList(_ list: ShoppingList, uid: String) async throws -> [String] {
        guard let listId = list.id else { return [] }
        var lines: [String] = [""]
        let ownershipLabel = list.ownerId == uid ? L("export_list_owner") : L("export_list_shared")
        lines.append("• \(list.name) \(ownershipLabel)")

        let storesSnapshot = try await db.collection("lists").document(listId).collection("stores")
            .order(by: "order").getDocuments()
        let stores = storesSnapshot.documents.compactMap { try? $0.data(as: Store.self) }

        for store in stores {
            guard let storeId = store.id else { continue }
            let itemsSnapshot = try await db.collection("lists").document(listId)
                .collection("stores").document(storeId).collection("items")
                .order(by: "createdAt").getDocuments()
            let items = itemsSnapshot.documents.compactMap { try? $0.data(as: ShoppingItem.self) }
            let bought = items.filter { $0.bought }.count
            lines.append("  - \(store.name) (\(String(format: L("export_store_progress"), bought, items.count)))")
            for item in items {
                let status = item.bought ? L("export_item_bought") : L("export_item_pending")
                let note = item.note.isEmpty ? "" : " · \(item.note)"
                lines.append("      \(status) \(item.name)\(note)")
            }
        }
        return lines
    }

    private func fetchLists(uid: String, status: String) async throws -> [ShoppingList] {
        let snapshot = try await db.collection("lists")
            .whereField("memberIds", arrayContains: uid)
            .whereField("status", isEqualTo: status)
            .getDocuments()
        return snapshot.documents.compactMap { try? $0.data(as: ShoppingList.self) }
    }
}
