package dev.webviewide.ui

import android.content.Context
import android.content.res.Configuration
import android.view.View
import java.util.Locale

object AppLocale {
    private const val PREFS = "dev.webviewide.locale"
    private const val KEY = "language"

    fun save(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, language)
            .apply()
    }

    fun load(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "zh")
            ?.ifBlank { "zh" }
            ?: "zh"

    fun apply(context: Context): Context {
        val language = load(context)
        val locale = when (language) {
            "ar" -> Locale("ar")
            "ru" -> Locale("ru")
            else -> Locale("zh")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
