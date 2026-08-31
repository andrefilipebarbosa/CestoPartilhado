package pt.cestopartilhado.app.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.model.ShoppingList

class InviteViewModel(
    private val authRepository: AuthRepository,
    private val listsRepository: ListsRepository,
) : ViewModel() {
    private val _list = MutableStateFlow<ShoppingList?>(null)
    val list: StateFlow<ShoppingList?> = _list

    private val _inviteResult = MutableStateFlow<String?>(null)
    val inviteResult: StateFlow<String?> = _inviteResult

    private val _isInviting = MutableStateFlow(false)
    val isInviting: StateFlow<Boolean> = _isInviting

    val isOwner: Boolean get() = _list.value?.ownerId == authRepository.currentUser?.uid
    val currentUid: String? get() = authRepository.currentUser?.uid
    val displayName: String get() = authRepository.currentUserProfile()?.displayName ?: ""

    fun load(listId: String) {
        viewModelScope.launch {
            listsRepository.observeListWithStores(listId)
                .catch { }
                .collect { _list.value = it.list }
        }
    }

    fun inviteLink(): String = _list.value?.let { listsRepository.inviteLinkFor(it.id) } ?: ""

    fun invite(email: String) {
        val listId = _list.value?.id ?: return
        if (email.isBlank()) return
        viewModelScope.launch {
            _isInviting.value = true
            _inviteResult.value = runCatching { listsRepository.inviteMemberByEmail(listId, email.trim()) }
                .getOrElse { "error" }
            _isInviting.value = false
        }
    }

    fun clearResult() { _inviteResult.value = null }
}
