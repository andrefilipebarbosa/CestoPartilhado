package pt.cestopartilhado.app.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.model.ShoppingList
import java.util.concurrent.TimeUnit

class ArchivedViewModel(
    private val authRepository: AuthRepository,
    private val listsRepository: ListsRepository,
) : ViewModel() {
    private val _closedLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val closedLists: StateFlow<List<ShoppingList>> = _closedLists

    val currentUid: String? get() = authRepository.currentUser?.uid

    init {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                listsRepository.observeLists(uid, ShoppingList.STATUS_CLOSED)
                    // Ver comentário equivalente em HomeViewModel: evita crash se a
                    // sessão terminar enquanto este ecrã ainda está a ouvir o Firestore.
                    .catch { }
                    .collect { _closedLists.value = it }
            }
        }
    }

    fun daysUntilDeletion(list: ShoppingList): Int {
        val closedAt = list.closedAt?.time ?: return 30
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - closedAt)
        return (30 - elapsedDays).toInt().coerceAtLeast(0)
    }

    fun recover(listId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch { listsRepository.setListStatus(listId, ShoppingList.STATUS_ACTIVE, uid) }
    }

    fun deleteNow(listId: String) {
        viewModelScope.launch { listsRepository.deleteList(listId) }
    }
}
