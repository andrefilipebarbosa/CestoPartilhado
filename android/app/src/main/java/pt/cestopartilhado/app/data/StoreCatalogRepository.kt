package pt.cestopartilhado.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.cestopartilhado.app.model.StoreCatalogEntry
import java.text.Normalizer

/** Catálogo global de lojas partilhado por todos os utilizadores, para autocompletar. */
class StoreCatalogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun ref() = firestore.collection("storeCatalog")

    fun normalize(name: String): String {
        val noAccents = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return noAccents
    }

    suspend fun search(query: String): List<StoreCatalogEntry> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = normalize(query)
        val snapshot = ref()
            .orderBy("normalizedName")
            .startAt(normalizedQuery)
            .endAt(normalizedQuery + "")
            .limit(10)
            .get()
            .await()
        return snapshot.toObjects(StoreCatalogEntry::class.java)
    }

    /** Regista/atualiza uma loja no catálogo global quando é usada numa lista. */
    suspend fun registerUsage(name: String) {
        val normalized = normalize(name)
        if (normalized.isEmpty()) return
        val doc = ref().document(normalized)
        val existing = doc.get().await()
        if (existing.exists()) {
            doc.update("usageCount", FieldValue.increment(1)).await()
        } else {
            doc.set(
                mapOf(
                    "name" to name.trim(),
                    "normalizedName" to normalized,
                    "usageCount" to 1L,
                )
            ).await()
        }
    }
}
