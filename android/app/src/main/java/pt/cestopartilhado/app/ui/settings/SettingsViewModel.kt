package pt.cestopartilhado.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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

    private val _notificationPrefs = MutableStateFlow(mapOf("added" to true, "closed" to true, "edited" to false))
    val notificationPrefs: StateFlow<Map<String, Boolean>> = _notificationPrefs

    val accountEmail: String get() = authRepository.currentUserProfile()?.email ?: ""

    init {
        authRepository.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                authRepository.observeNotificationPrefs(uid)
                    .catch { }
                    .collect { _notificationPrefs.value = it }
            }
        }
    }

    fun currentLanguage(context: Context): String = LocaleManager.currentSetting(context)

    fun setLanguage(context: Context, language: String) {
        LocaleManager.setLanguage(context, language)
        viewModelScope.launch { authRepository.setPreferredLanguage(LocaleManager.resolvedLanguage(context)) }
    }

    fun setNotificationPref(type: String, enabled: Boolean) {
        viewModelScope.launch { authRepository.setNotificationPref(type, enabled) }
    }

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
