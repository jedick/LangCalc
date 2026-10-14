package com.jedick.langcalc.data

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
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
  /**
   * Looks up a string in the language of [languageTag] without touching the app's interface
   * language (unlike [wrap], this does not call Locale.setDefault). Used for text that follows
   * the language of the user's request instead of the interface language.
   */
  fun getString(context: Context, languageTag: String, @StringRes resId: Int, vararg args: Any): String {
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(languageTag))
    return context.createConfigurationContext(config).getString(resId, *args)
  }

  /**
   * The language of a request, as an [AppLanguage] tag that can be passed to [getString].
   *
   * A spoken request uses the speech-recognition language chosen for it ([speechLanguageTag], a
   * BCP-47 tag such as "zh-CN") if that is English or Chinese. Otherwise (typed requests, Auto, or
   * another language) this guesses from the text: Chinese if it contains any Han character,
   * English if not. The app's interface language is deliberately not used.
   */
  fun requestLanguageTag(text: String, speechLanguageTag: String? = null): String {
    when (speechLanguageTag?.let { Locale.forLanguageTag(it).language }) {
      AppLanguage.ENGLISH.tag -> return AppLanguage.ENGLISH.tag
      AppLanguage.CHINESE.tag -> return AppLanguage.CHINESE.tag
    }
    return guessLanguageTag(text)
  }

  private fun guessLanguageTag(text: String): String =
    if (text.codePoints().anyMatch { Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN }) {
      AppLanguage.CHINESE.tag
    } else {
      AppLanguage.ENGLISH.tag
    }

  fun wrap(context: Context, languageTag: String): Context {
    val locale = Locale.forLanguageTag(languageTag)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return context.createConfigurationContext(config)
  }
}
