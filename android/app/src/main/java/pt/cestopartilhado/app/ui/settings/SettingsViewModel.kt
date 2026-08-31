package pt.cestopartilhado.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ExportRepository
import pt.cestopartilhado.app.util.LocaleManager

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    val currentLanguage: String get() = LocaleManager.currentLanguage()
    val accountEmail: String get() = authRepository.currentUserProfile()?.email ?: ""

    fun setLanguage(language: String) = LocaleManager.applyLanguage(language)

    fun exportData(onReady: (String) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _isExporting.value = true
            val text = exportRepository.buildExportText(uid, accountEmail)
            _isExporting.value = false
            onReady(text)
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}
