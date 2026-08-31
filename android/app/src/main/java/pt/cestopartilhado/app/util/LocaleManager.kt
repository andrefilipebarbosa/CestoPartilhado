package pt.cestopartilhado.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Por omissão a app segue o idioma do telefone automaticamente (é o comportamento
 * normal dos recursos do Android: values/ = inglês, values-pt/ = português — nada a
 * fazer aqui para isso). Esta classe só trata da escolha manual nas Definições, que
 * substitui esse comportamento automático enquanto estiver definida.
 */
object LocaleManager {
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_PT = "pt"
    const val LANGUAGE_EN = "en"

    fun applyLanguage(language: String) {
        val locales = if (language == LANGUAGE_SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return LANGUAGE_SYSTEM
        return locales[0]?.language ?: LANGUAGE_SYSTEM
    }
}
