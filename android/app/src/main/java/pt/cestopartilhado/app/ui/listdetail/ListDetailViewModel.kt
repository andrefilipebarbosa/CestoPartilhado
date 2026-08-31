package pt.cestopartilhado.app.ui.listdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.model.ListWithStores
import pt.cestopartilhado.app.model.ShoppingList

class ListDetailViewModel(
    private val authRepository: AuthRepository,
    private val listsRepository: ListsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<ListWithStores?>(null)
    val state: StateFlow<ListWithStores?> = _state

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val currentUid: String? get() = authRepository.currentUser?.uid
    val isOwner: Boolean get() = _state.value?.list?.ownerId == currentUid

    fun load(listId: String) {
        viewModelScope.launch {
            listsRepository.observeListWithStores(listId)
                .catch { _error.value = it.message ?: "error" }
                .collect { _state.value = it }
        }
    }

    fun addStore(name: String) {
        val listId = _state.value?.list?.id ?: return
        val nextOrder = (_state.value?.stores?.size ?: 0).toLong()
        viewModelScope.launch { listsRepository.addStore(listId, name, nextOrder) }
    }

    fun removeStore(storeId: String) {
        val listId = _state.value?.list?.id ?: return
        viewModelScope.launch { listsRepository.removeStore(listId, storeId) }
    }

    fun addItem(storeId: String, name: String, note: String) {
        val listId = _state.value?.list?.id ?: return
        val uid = currentUid ?: return
        viewModelScope.launch { listsRepository.addItem(listId, storeId, name, note, uid) }
    }

    fun toggleItem(storeId: String, itemId: String, bought: Boolean) {
        val listId = _state.value?.list?.id ?: return
        val uid = currentUid ?: return
        viewModelScope.launch { listsRepository.setItemBought(listId, storeId, itemId, bought, uid) }
    }

    fun removeItem(storeId: String, itemId: String, wasBought: Boolean) {
        val listId = _state.value?.list?.id ?: return
        viewModelScope.launch { listsRepository.removeItem(listId, storeId, itemId, wasBought) }
    }

    fun closeList() {
        val listId = _state.value?.list?.id ?: return
        viewModelScope.launch { listsRepository.setListStatus(listId, ShoppingList.STATUS_CLOSED) }
    }

    fun reopenList() {
        val listId = _state.value?.list?.id ?: return
        viewModelScope.launch { listsRepository.setListStatus(listId, ShoppingList.STATUS_ACTIVE) }
    }

    /** As regras do Firestore também impedem isto — aqui só evitamos mostrar o botão a quem não é dono. */
    fun deleteList(onDeleted: () -> Unit) {
        val listId = _state.value?.list?.id ?: return
        if (!isOwner) return
        viewModelScope.launch {
            listsRepository.deleteList(listId)
            onDeleted()
        }
    }
}
