package pt.cestopartilhado.app.ui.newlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.data.StoreCatalogRepository
import pt.cestopartilhado.app.model.StoreCatalogEntry

class NewListViewModel(
    private val authRepository: AuthRepository,
    private val listsRepository: ListsRepository,
    private val storeCatalogRepository: StoreCatalogRepository,
) : ViewModel() {
    var listName by mutableStateOf("")
    var storeQuery by mutableStateOf("")

    private val _suggestions = MutableStateFlow<List<StoreCatalogEntry>>(emptyList())
    val suggestions: StateFlow<List<StoreCatalogEntry>> = _suggestions

    private val _addedStores = MutableStateFlow<List<String>>(emptyList())
    val addedStores: StateFlow<List<String>> = _addedStores

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    fun onQueryChanged(query: String) {
        storeQuery = query
        viewModelScope.launch {
            _suggestions.value = storeCatalogRepository.search(query)
                .filter { it.name !in _addedStores.value }
        }
    }

    fun addStore(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed in _addedStores.value) return
        _addedStores.value = _addedStores.value + trimmed
        storeQuery = ""
        _suggestions.value = emptyList()
    }

    fun removeStore(name: String) {
        _addedStores.value = _addedStores.value - name
    }

    fun createList(onCreated: (String) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        if (listName.isBlank()) return
        viewModelScope.launch {
            _isCreating.value = true
            val listId = listsRepository.createList(listName.trim(), uid)
            _addedStores.value.forEachIndexed { index, storeName ->
                listsRepository.addStore(listId, storeName, index.toLong())
                storeCatalogRepository.registerUsage(storeName)
            }
            _isCreating.value = false
            onCreated(listId)
        }
    }
}
