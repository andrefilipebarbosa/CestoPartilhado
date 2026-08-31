package pt.cestopartilhado.app.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn

    fun signInIntent(): Intent = authRepository.signInIntent()

    fun onSignInResult(data: Intent?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSigningIn.value = true
            val result = authRepository.handleSignInResult(data)
            _isSigningIn.value = false
            result.onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message }
        }
    }
}
