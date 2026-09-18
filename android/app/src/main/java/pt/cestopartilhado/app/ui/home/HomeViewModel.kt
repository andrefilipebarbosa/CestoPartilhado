package pt.cestopartilhado.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.data.SplitListsRepository
import pt.cestopartilhado.app.model.ShoppingList
import pt.cestopartilhado.app.model.SplitList

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val listsRepository: ListsRepository,
    private val splitListsRepository: SplitListsRepository,
) : ViewModel() {
    private val _activeLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val activeLists: StateFlow<List<ShoppingList>> = _activeLists

    private val _splitLists = MutableStateFlow<List<SplitList>>(emptyList())
    val splitLists: StateFlow<List<SplitList>> = _splitLists

    val displayName: String get() = authRepository.currentUserProfile()?.displayName ?: ""
    val currentUid: String? get() = authRepository.currentUser?.uid

    init {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch { authRepository.refreshPublicProfileIfSignedIn() }
            viewModelScope.launch {
                listsRepository.observeLists(uid, ShoppingList.STATUS_ACTIVE)
                    // Se a sessão terminar enquanto este ecrã ainda está a ouvir o
                    // Firestore, a subscrição passa a não ter permissão — ignoramos
                    // o erro em vez de deixar a exceção rebentar a app.
                    .catch { }
                    .collect { _activeLists.value = it }
            }
            viewModelScope.launch {
                splitListsRepository.observeLists(uid)
                    .catch { }
                    .collect { _splitLists.value = it }
            }
        }
    }
}
