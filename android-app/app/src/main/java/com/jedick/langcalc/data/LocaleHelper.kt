package com.jedick.langcalc.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the app's chosen interface language to a Context by wrapping its resources
 * configuration, so `stringResource()` calls throughout the UI resolve against values/ or
 * values-zh/ accordingly.
 *
 * This app deliberately does its own locale wrapping in Activity.attachBaseContext() instead of
 * androidx.appcompat's per-app language APIs (AppCompatDelegate.setApplicationLocales), since that
 * mechanism needs an AppCompatActivity to auto-persist the locale below API 33, and this app is a
 * plain ComponentActivity built entirely with Compose. The trade-off: changing the language calls
 * Activity.recreate() explicitly (see SettingsScreen.kt) rather than relying on the framework to
 * do it.
 */
object LocaleHelper {
  fun wrap(context: Context, languageTag: String): Context {
    val locale = Locale.forLanguageTag(languageTag)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return context.createConfigurationContext(config)
  }
}
