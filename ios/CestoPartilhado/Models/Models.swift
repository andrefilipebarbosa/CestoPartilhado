import Foundation
import FirebaseFirestore

struct ShoppingList: Identifiable, Codable, Equatable {
    static let statusActive = "active"
    static let statusClosed = "closed"

    @DocumentID var id: String?
    var name: String = ""
    var ownerId: String = ""
    var memberIds: [String] = []
    var pendingInvites: [String] = []
    var status: String = ShoppingList.statusActive
    var storeCount: Int = 0
    var itemCount: Int = 0
    var boughtCount: Int = 0
    @ServerTimestamp var createdAt: Timestamp?
    @ServerTimestamp var updatedAt: Timestamp?
    var closedAt: Timestamp?
}

struct Store: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var name: String = ""
    var order: Int = 0
    @ServerTimestamp var createdAt: Timestamp?
}

struct ShoppingItem: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var name: String = ""
    var note: String = ""
    var bought: Bool = false
    var addedBy: String = ""
    var boughtBy: String?
    @ServerTimestamp var createdAt: Timestamp?
    @ServerTimestamp var updatedAt: Timestamp?
}

struct StoreCatalogEntry: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var name: String = ""
    var normalizedName: String = ""
    var usageCount: Int = 0
}

struct StoreWithItems: Identifiable, Equatable {
    var store: Store
    var items: [ShoppingItem]
    var id: String { store.id ?? UUID().uuidString }
    var boughtCount: Int { items.filter { $0.bought }.count }
}

struct ListWithStores: Equatable {
    var list: ShoppingList
    var stores: [StoreWithItems]
    var totalItems: Int { stores.reduce(0) { $0 + $1.items.count } }
    var boughtItems: Int { stores.reduce(0) { $0 + $1.boughtCount } }
}

/// Lista dividida: os artigos têm um valor, e cada membro deve uma parte igual
/// do total. Assim que `paidMemberIds` deixa de estar vazio a lista fica
/// bloqueada a alterações (reforçado nas regras do Firestore, não só aqui).
struct SplitList: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var name: String = ""
    var ownerId: String = ""
    var memberIds: [String] = []
    var pendingInvites: [String] = []
    var paidMemberIds: [String] = []
    var totalValue: Double = 0
    var itemCount: Int = 0
    @ServerTimestamp var createdAt: Timestamp?
    @ServerTimestamp var updatedAt: Timestamp?

    var isLocked: Bool { !paidMemberIds.isEmpty }
    var shareValue: Double { memberIds.isEmpty ? 0 : totalValue / Double(memberIds.count) }
}

struct SplitItem: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var name: String = ""
    var value: Double = 0
    var addedBy: String = ""
    @ServerTimestamp var createdAt: Timestamp?
    @ServerTimestamp var updatedAt: Timestamp?
}

struct SplitListWithItems: Equatable {
    var list: SplitList
    var items: [SplitItem]
}
