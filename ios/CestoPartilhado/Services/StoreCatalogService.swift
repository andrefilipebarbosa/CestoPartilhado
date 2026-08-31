import Foundation
import FirebaseFirestore

/// Catálogo global de lojas partilhado por todos os utilizadores, para autocompletar.
final class StoreCatalogService {
    private let db = Firestore.firestore()
    private func ref() -> CollectionReference { db.collection("storeCatalog") }

    func normalize(_ name: String) -> String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .folding(options: .diacriticInsensitive, locale: .current)
    }

    func search(_ query: String) async throws -> [StoreCatalogEntry] {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else { return [] }
        let normalizedQuery = normalize(query)
        let snapshot = try await ref()
            .order(by: "normalizedName")
            .start(at: [normalizedQuery])
            .end(at: [normalizedQuery + "\u{f8ff}"])
            .limit(to: 10)
            .getDocuments()
        return snapshot.documents.compactMap { try? $0.data(as: StoreCatalogEntry.self) }
    }

    /// Regista/atualiza uma loja no catálogo global quando é usada numa lista.
    func registerUsage(_ name: String) async throws {
        let normalized = normalize(name)
        guard !normalized.isEmpty else { return }
        let doc = ref().document(normalized)
        let existing = try await doc.getDocument()
        if existing.exists {
            try await doc.updateData(["usageCount": FieldValue.increment(Int64(1))])
        } else {
            try await doc.setData([
                "name": name.trimmingCharacters(in: .whitespacesAndNewlines),
                "normalizedName": normalized,
                "usageCount": 1,
            ])
        }
    }
}
