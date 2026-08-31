package pt.cestopartilhado.app.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ShoppingList(
    @DocumentId val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val pendingInvites: List<String> = emptyList(),
    val status: String = STATUS_ACTIVE,
    val storeCount: Long = 0,
    val itemCount: Long = 0,
    val boughtCount: Long = 0,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null,
    val closedAt: Date? = null,
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_CLOSED = "closed"
    }
}

data class Store(
    @DocumentId val id: String = "",
    val name: String = "",
    val order: Long = 0,
    @ServerTimestamp val createdAt: Date? = null,
)

data class ShoppingItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val note: String = "",
    val bought: Boolean = false,
    val addedBy: String = "",
    val boughtBy: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null,
)

data class StoreCatalogEntry(
    @DocumentId val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val usageCount: Long = 0,
)

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
)

/** Uma loja com as respetivas lojas/artigos carregados, usada no ecrã de detalhe. */
data class StoreWithItems(
    val store: Store,
    val items: List<ShoppingItem>,
) {
    val boughtCount get() = items.count { it.bought }
}

data class ListWithStores(
    val list: ShoppingList,
    val stores: List<StoreWithItems>,
) {
    val totalItems get() = stores.sumOf { it.items.size }
    val boughtItems get() = stores.sumOf { it.boughtCount }
}
