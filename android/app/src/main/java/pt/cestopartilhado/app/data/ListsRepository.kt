package pt.cestopartilhado.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pt.cestopartilhado.app.model.ListWithStores
import pt.cestopartilhado.app.model.ShoppingItem
import pt.cestopartilhado.app.model.ShoppingList
import pt.cestopartilhado.app.model.Store
import pt.cestopartilhado.app.model.StoreWithItems

/**
 * Toda a lógica de listas/lojas/artigos. Qualquer membro (dono ou convidado) pode
 * ler e escrever lojas/artigos — reforçado pelas regras do Firestore, não só aqui.
 * Só o dono pode eliminar a lista (também reforçado nas regras).
 */
class ListsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    functionsRegion: String = "europe-west1",
) {
    private val functions = FirebaseFunctions.getInstance(functionsRegion)
    private fun listsRef() = firestore.collection("lists")
    private fun storesRef(listId: String) = listsRef().document(listId).collection("stores")
    private fun itemsRef(listId: String, storeId: String) =
        storesRef(listId).document(storeId).collection("items")

    fun observeLists(uid: String, status: String): Flow<List<ShoppingList>> = callbackFlow {
        val registration = listsRef()
            .whereArrayContains("memberIds", uid)
            .whereEqualTo("status", status)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(ShoppingList::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /** Uma lista, com as lojas e respetivos artigos, tudo em tempo real. */
    fun observeListWithStores(listId: String): Flow<ListWithStores> {
        val listFlow = callbackFlow {
            val reg = listsRef().document(listId).addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snap?.toObject(ShoppingList::class.java)?.let { trySend(it) }
            }
            awaitClose { reg.remove() }
        }
        val storesFlow = observeStoresWithItems(listId)
        return listFlow.combine(storesFlow) { list, stores -> ListWithStores(list, stores) }
    }

    private fun observeStoresWithItems(listId: String): Flow<List<StoreWithItems>> = callbackFlow {
        val itemRegs = mutableMapOf<String, ListenerRegistration>()
        val itemsByStore = mutableMapOf<String, List<ShoppingItem>>()
        var stores: List<Store> = emptyList()

        fun emitCombined() {
            trySend(stores.map { StoreWithItems(it, itemsByStore[it.id] ?: emptyList()) })
        }

        val storesReg = storesRef(listId)
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                stores = snapshot?.toObjects(Store::class.java) ?: emptyList()
                val currentIds = stores.map { it.id }.toSet()

                // Para de escutar lojas removidas.
                val toRemove = itemRegs.keys - currentIds
                toRemove.forEach { itemRegs.remove(it)?.remove(); itemsByStore.remove(it) }

                // Começa a escutar lojas novas.
                stores.filter { it.id !in itemRegs }.forEach { store ->
                    itemRegs[store.id] = itemsRef(listId, store.id)
                        .orderBy("createdAt")
                        .addSnapshotListener { itemsSnap, itemsError ->
                            if (itemsError != null) return@addSnapshotListener
                            itemsByStore[store.id] = itemsSnap?.toObjects(ShoppingItem::class.java) ?: emptyList()
                            emitCombined()
                        }
                }
                emitCombined()
            }

        awaitClose {
            storesReg.remove()
            itemRegs.values.forEach { it.remove() }
        }
    }

    suspend fun createList(name: String, ownerUid: String): String {
        val doc = listsRef().document()
        doc.set(
            mapOf(
                "name" to name,
                "ownerId" to ownerUid,
                "memberIds" to listOf(ownerUid),
                "pendingInvites" to emptyList<String>(),
                "status" to ShoppingList.STATUS_ACTIVE,
                "storeCount" to 0L,
                "itemCount" to 0L,
                "boughtCount" to 0L,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        ).await()
        return doc.id
    }

    // storeCount/itemCount/boughtCount em `lists/{listId}` são contadores denormalizados,
    // mantidos aqui a par das subcoleções — evita ter de escutar todas as lojas/artigos
    // só para mostrar um resumo no ecrã principal.
    private fun listDoc(listId: String) = listsRef().document(listId)

    suspend fun addStore(listId: String, name: String, order: Long) {
        val batch = firestore.batch()
        batch.set(storesRef(listId).document(), mapOf(
            "name" to name,
            "order" to order,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.update(listDoc(listId), mapOf(
            "storeCount" to com.google.firebase.firestore.FieldValue.increment(1),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    suspend fun removeStore(listId: String, storeId: String) {
        val items = itemsRef(listId, storeId).get().await()
        val boughtInStore = items.documents.count { it.getBoolean("bought") == true }

        val batch = firestore.batch()
        items.documents.forEach { batch.delete(it.reference) }
        batch.delete(storesRef(listId).document(storeId))
        batch.update(listDoc(listId), mapOf(
            "storeCount" to com.google.firebase.firestore.FieldValue.increment(-1),
            "itemCount" to com.google.firebase.firestore.FieldValue.increment(-items.size().toLong()),
            "boughtCount" to com.google.firebase.firestore.FieldValue.increment(-boughtInStore.toLong()),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    suspend fun addItem(listId: String, storeId: String, name: String, note: String, uid: String) {
        val batch = firestore.batch()
        batch.set(itemsRef(listId, storeId).document(), mapOf(
            "name" to name,
            "note" to note,
            "bought" to false,
            "addedBy" to uid,
            "boughtBy" to null,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.update(listDoc(listId), mapOf(
            "itemCount" to com.google.firebase.firestore.FieldValue.increment(1),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    suspend fun setItemBought(listId: String, storeId: String, itemId: String, bought: Boolean, uid: String) {
        val batch = firestore.batch()
        batch.update(itemsRef(listId, storeId).document(itemId), mapOf(
            "bought" to bought,
            "boughtBy" to if (bought) uid else null,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.update(listDoc(listId), mapOf(
            "boughtCount" to com.google.firebase.firestore.FieldValue.increment(if (bought) 1 else -1),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    suspend fun removeItem(listId: String, storeId: String, itemId: String, wasBought: Boolean) {
        val batch = firestore.batch()
        batch.delete(itemsRef(listId, storeId).document(itemId))
        batch.update(listDoc(listId), mapOf(
            "itemCount" to com.google.firebase.firestore.FieldValue.increment(-1),
            "boughtCount" to com.google.firebase.firestore.FieldValue.increment(if (wasBought) -1 else 0),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    suspend fun setListStatus(listId: String, status: String) {
        val updates = mutableMapOf<String, Any?>(
            "status" to status,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        )
        updates["closedAt"] = if (status == ShoppingList.STATUS_CLOSED)
            com.google.firebase.firestore.FieldValue.serverTimestamp() else null
        listsRef().document(listId).update(updates).await()
    }

    /** Só têm sucesso nas regras do Firestore se quem chama for o dono da lista. */
    suspend fun deleteList(listId: String) {
        val stores = storesRef(listId).get().await()
        for (storeDoc in stores.documents) {
            val items = itemsRef(listId, storeDoc.id).get().await()
            val batch = firestore.batch()
            items.documents.forEach { batch.delete(it.reference) }
            batch.delete(storeDoc.reference)
            batch.commit().await()
        }
        listsRef().document(listId).delete().await()
    }

    suspend fun inviteMemberByEmail(listId: String, email: String): String {
        val result = functions.getHttpsCallable("inviteMemberByEmail")
            .call(mapOf("listId" to listId, "email" to email))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
        return data?.get("status") as? String ?: "unknown"
    }

    fun inviteLinkFor(listId: String): String = "cestopartilhado://join?listId=$listId"
}
