package pt.cestopartilhado.app.ui.splitlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.SplitListsRepository

class NewSplitListViewModel(
    private val authRepository: AuthRepository,
    private val splitListsRepository: SplitListsRepository,
) : ViewModel() {
    var listName by mutableStateOf("")

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    fun createList(onCreated: (String) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        if (listName.isBlank()) return
        viewModelScope.launch {
            _isCreating.value = true
            val listId = splitListsRepository.createList(listName.trim(), uid)
            _isCreating.value = false
            onCreated(listId)
        }
    }
}
