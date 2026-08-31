package pt.cestopartilhado.app.ui.splitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.SplitListsRepository
import pt.cestopartilhado.app.model.SplitItem
import pt.cestopartilhado.app.model.SplitListWithItems

class SplitListDetailViewModel(
    private val authRepository: AuthRepository,
    private val splitListsRepository: SplitListsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SplitListWithItems?>(null)
    val state: StateFlow<SplitListWithItems?> = _state

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _inviteResult = MutableStateFlow<String?>(null)
    val inviteResult: StateFlow<String?> = _inviteResult

    val currentUid: String? get() = authRepository.currentUser?.uid
    val isOwner: Boolean get() = _state.value?.list?.ownerId == currentUid

    fun load(listId: String) {
        viewModelScope.launch {
            splitListsRepository.observeListWithItems(listId)
                .catch { _error.value = it.message ?: "error" }
                .collect { _state.value = it }
        }
    }

    fun addItem(name: String, value: Double) {
        val listId = _state.value?.list?.id ?: return
        val uid = currentUid ?: return
        viewModelScope.launch { splitListsRepository.addItem(listId, name, value, uid) }
    }

    fun removeItem(item: SplitItem) {
        val listId = _state.value?.list?.id ?: return
        viewModelScope.launch { splitListsRepository.removeItem(listId, item) }
    }

    /** Só têm sucesso nas regras do Firestore se quem chama for o dono da lista. */
    fun setMemberPaid(memberUid: String, paid: Boolean) {
        val listId = _state.value?.list?.id ?: return
        viewModelScope.launch { splitListsRepository.setMemberPaid(listId, memberUid, paid) }
    }

    fun invite(email: String) {
        val listId = _state.value?.list?.id ?: return
        if (email.isBlank()) return
        viewModelScope.launch {
            _inviteResult.value = runCatching { splitListsRepository.inviteMemberByEmail(listId, email.trim()) }
                .getOrElse { "error" }
        }
    }

    fun deleteList(onDeleted: () -> Unit) {
        val listId = _state.value?.list?.id ?: return
        if (!isOwner) return
        viewModelScope.launch {
            splitListsRepository.deleteList(listId)
            onDeleted()
        }
    }
}
