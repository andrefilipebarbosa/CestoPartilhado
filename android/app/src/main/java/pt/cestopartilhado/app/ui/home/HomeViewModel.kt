package pt.cestopartilhado.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.model.ShoppingList

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val listsRepository: ListsRepository,
) : ViewModel() {
    private val _activeLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val activeLists: StateFlow<List<ShoppingList>> = _activeLists

    val displayName: String get() = authRepository.currentUserProfile()?.displayName ?: ""

    init {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                listsRepository.observeLists(uid, ShoppingList.STATUS_ACTIVE)
                    .collect { _activeLists.value = it }
            }
        }
    }
}
