package pt.cestopartilhado.app.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.model.ShoppingItem
import pt.cestopartilhado.app.model.ShoppingList
import pt.cestopartilhado.app.model.Store
import pt.cestopartilhado.app.util.LocaleManager
import java.text.DateFormat
import java.util.Date

/**
 * Constrói um relatório em texto simples com TODAS as listas a que o utilizador
 * tem acesso (próprias e partilhadas), incluindo lojas e artigos — para a opção
 * "Exportar os meus dados" nas Definições.
 */
class ExportRepository(
    private val appContext: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    // Reembrulhado a cada exportação (não só no arranque da app) para respeitar
    // uma troca de idioma feita a meio da sessão, sem precisar de reiniciar a app.
    private val context: Context get() = LocaleManager.wrap(appContext)

    suspend fun buildExportText(uid: String, accountEmail: String): String {
        val activeLists = fetchLists(uid, ShoppingList.STATUS_ACTIVE)
        val closedLists = fetchLists(uid, ShoppingList.STATUS_CLOSED)

        val sb = StringBuilder()
        sb.appendLine(context.getString(R.string.export_header))
        sb.appendLine(context.getString(R.string.export_generated_at, DateFormat.getDateTimeInstance().format(Date())))
        sb.appendLine(context.getString(R.string.export_account, accountEmail))
        sb.appendLine()

        if (activeLists.isEmpty() && closedLists.isEmpty()) {
            sb.appendLine(context.getString(R.string.export_no_lists))
            return sb.toString()
        }

        if (activeLists.isNotEmpty()) {
            sb.appendLine("== ${context.getString(R.string.export_active_lists)} ==")
            activeLists.forEach { appendList(sb, it, uid) }
        }
        if (closedLists.isNotEmpty()) {
            sb.appendLine("== ${context.getString(R.string.export_closed_lists)} ==")
            closedLists.forEach { appendList(sb, it, uid) }
        }
        return sb.toString()
    }

    private suspend fun appendList(sb: StringBuilder, list: ShoppingList, uid: String) {
        val ownershipLabel = if (list.ownerId == uid)
            context.getString(R.string.export_list_owner)
        else
            context.getString(R.string.export_list_shared)
        sb.appendLine()
        sb.appendLine("• ${list.name} $ownershipLabel")

        val stores = firestore.collection("lists").document(list.id).collection("stores")
            .orderBy("order").get().await().toObjects(Store::class.java)

        for (store in stores) {
            val items = firestore.collection("lists").document(list.id)
                .collection("stores").document(store.id).collection("items")
                .orderBy("createdAt").get().await().toObjects(ShoppingItem::class.java)
            val bought = items.count { it.bought }
            sb.appendLine("  - ${store.name} (${context.getString(R.string.export_store_progress, bought, items.size)})")
            items.forEach { item ->
                val status = if (item.bought)
                    context.getString(R.string.export_item_bought)
                else
                    context.getString(R.string.export_item_pending)
                val note = if (item.note.isNotBlank()) " · ${item.note}" else ""
                sb.appendLine("      $status ${item.name}$note")
            }
        }
    }

    private suspend fun fetchLists(uid: String, status: String): List<ShoppingList> =
        firestore.collection("lists")
            .whereArrayContains("memberIds", uid)
            .whereEqualTo("status", status)
            .get()
            .await()
            .toObjects(ShoppingList::class.java)
}
