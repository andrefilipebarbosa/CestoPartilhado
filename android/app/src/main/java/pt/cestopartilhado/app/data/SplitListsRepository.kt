package pt.cestopartilhado.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import pt.cestopartilhado.app.model.SplitItem
import pt.cestopartilhado.app.model.SplitList
import pt.cestopartilhado.app.model.SplitListWithItems

/**
 * Lógica das listas divididas. Qualquer membro pode adicionar/remover artigos
 * enquanto a lista estiver desbloqueada; só o dono marca/reverte pagamentos
 * (o que bloqueia/desbloqueia a lista) e só o dono a pode eliminar — tudo
 * também reforçado nas regras do Firestore, não só aqui.
 */
class SplitListsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    functionsRegion: String = "europe-west1",
) {
    private val functions = FirebaseFunctions.getInstance(functionsRegion)
    private fun listsRef() = firestore.collection("splitLists")
    private fun itemsRef(listId: String) = listsRef().document(listId).collection("items")

    fun observeLists(uid: String): Flow<List<SplitList>> = callbackFlow {
        val registration = listsRef()
            .whereArrayContains("memberIds", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(SplitList::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeListWithItems(listId: String): Flow<SplitListWithItems> {
        val listFlow = callbackFlow {
            val reg = listsRef().document(listId).addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snap?.toObject(SplitList::class.java)?.let { trySend(it) }
            }
            awaitClose { reg.remove() }
        }
        val itemsFlow = callbackFlow {
            val reg = itemsRef(listId).orderBy("createdAt").addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toObjects(SplitItem::class.java) ?: emptyList())
            }
            awaitClose { reg.remove() }
        }
        return listFlow.combine(itemsFlow) { list, items -> SplitListWithItems(list, items) }
    }

    suspend fun createList(name: String, ownerUid: String): String {
        val doc = listsRef().document()
        doc.set(
            mapOf(
                "name" to name,
                "ownerId" to ownerUid,
                "memberIds" to listOf(ownerUid),
                "pendingInvites" to emptyList<String>(),
                "paidMemberIds" to emptyList<String>(),
                "totalValue" to 0.0,
                "itemCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        return doc.id
    }

    suspend fun addItem(listId: String, name: String, value: Double, uid: String) {
        val batch = firestore.batch()
        batch.set(itemsRef(listId).document(), mapOf(
            "name" to name,
            "value" to value,
            "addedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        ))
        batch.update(listsRef().document(listId), mapOf(
            "itemCount" to FieldValue.increment(1),
            "totalValue" to FieldValue.increment(value),
            "updatedAt" to FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    suspend fun removeItem(listId: String, item: SplitItem) {
        val batch = firestore.batch()
        batch.delete(itemsRef(listId).document(item.id))
        batch.update(listsRef().document(listId), mapOf(
            "itemCount" to FieldValue.increment(-1),
            "totalValue" to FieldValue.increment(-item.value),
            "updatedAt" to FieldValue.serverTimestamp(),
        ))
        batch.commit().await()
    }

    /** Só o dono consegue com sucesso — reforçado nas regras do Firestore. */
    suspend fun setMemberPaid(listId: String, memberUid: String, paid: Boolean) {
        listsRef().document(listId).update(
            "paidMemberIds",
            if (paid) FieldValue.arrayUnion(memberUid) else FieldValue.arrayRemove(memberUid),
            "updatedAt", FieldValue.serverTimestamp(),
        ).await()
    }

    /** Só têm sucesso nas regras do Firestore se quem chama for o dono da lista. */
    suspend fun deleteList(listId: String) {
        val items = itemsRef(listId).get().await()
        val batch = firestore.batch()
        items.documents.forEach { batch.delete(it.reference) }
        batch.delete(listsRef().document(listId))
        batch.commit().await()
    }

    suspend fun inviteMemberByEmail(listId: String, email: String): String {
        val result = functions.getHttpsCallable("inviteMemberByEmail")
            .call(mapOf("listId" to listId, "email" to email, "collection" to "splitLists"))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
        return data?.get("status") as? String ?: "unknown"
    }
}
