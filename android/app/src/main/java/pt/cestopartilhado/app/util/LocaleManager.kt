package pt.cestopartilhado.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Por omissão a app segue o idioma do telefone automaticamente (é o comportamento
 * normal dos recursos do Android: values/ = inglês, values-pt/ = português — nada a
 * fazer aqui para isso). Esta classe trata da escolha manual nas Definições.
 *
 * Nota: tentámos primeiro `AppCompatDelegate.setApplicationLocales()` (a API
 * "per-app language" recomendada), mas falhava silenciosamente nesta app —
 * confirmado com logging (`getApplicationLocales()` continuava vazio depois de
 * chamar `setApplicationLocales()`). Por isso usamos a abordagem clássica e
 * garantida de embrulhar o Context da Activity com a Configuration certa.
 */
object LocaleManager {
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_PT = "pt"
    const val LANGUAGE_EN = "en"

    private const val PREFS_NAME = "cesto_prefs"
    private const val KEY_LANGUAGE = "language"

    fun currentSetting(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    /** "pt" ou "en" — resolve "system" para o idioma do telefone (só a app suporta estes dois). */
    fun resolvedLanguage(context: Context): String {
        val setting = currentSetting(context)
        if (setting != LANGUAGE_SYSTEM) return setting
        return if (Locale.getDefault().language == LANGUAGE_PT) LANGUAGE_PT else LANGUAGE_EN
    }

    /** Aplica o idioma guardado a um Context — chamar em `attachBaseContext`. */
    fun wrap(context: Context): Context {
        val language = currentSetting(context)
        if (language == LANGUAGE_SYSTEM) return context
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
