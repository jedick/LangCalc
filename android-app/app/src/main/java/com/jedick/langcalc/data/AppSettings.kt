package com.jedick.langcalc.data

import android.content.Context
import android.content.SharedPreferences

/** The app's interface language. Does not affect the model, which is bilingual either way. */
enum class AppLanguage(val tag: String) {
  ENGLISH("en"),
  CHINESE("zh");

  companion object {
    fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
  }
}

/**
 * Small SharedPreferences-backed settings store. Reads are synchronous and cheap (SharedPreferences
 * keeps its backing file cached in memory after the first load), which matters for
 * [languageTagBlocking]: it's called from Activity.attachBaseContext(), which cannot suspend.
 */
class AppSettings(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  var language: AppLanguage
    get() = AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null))
    set(value) = prefs.edit().putString(KEY_LANGUAGE, value.tag).apply()

  /**
   * The language passed to the on-device speech recognizer (RecognizerIntent.EXTRA_LANGUAGE), as a
   * BCP-47 tag such as "en-US", or null for Auto, which leaves EXTRA_LANGUAGE unset so the
   * recognizer uses its own default. That default tends to stay on English on some devices even
   * after a Chinese language pack is installed, so picking a language explicitly is more reliable.
   */
  var asrLanguageTag: String?
    get() {
      // An empty string means Auto was chosen explicitly. A missing key may mean the setting was
      // saved by an earlier version, which stored "en", "zh" or "auto" under another key.
      if (prefs.contains(KEY_ASR_LANGUAGE_TAG)) {
        return prefs.getString(KEY_ASR_LANGUAGE_TAG, null)?.takeIf { it.isNotEmpty() }
      }
      return when (prefs.getString(KEY_LEGACY_ASR_LANGUAGE, null)) {
        "en" -> "en-US"
        "zh" -> "zh-CN"
        else -> null
      }
    }
    set(value) = prefs.edit().putString(KEY_ASR_LANGUAGE_TAG, value ?: "").apply()

  /** The ETag Hugging Face returned for the currently-downloaded model file, if any. */
  var downloadedModelETag: String?
    get() = prefs.getString(KEY_MODEL_ETAG, null)
    set(value) = prefs.edit().putString(KEY_MODEL_ETAG, value).apply()

  companion object {
    private const val PREFS_NAME = "langcalc_settings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_LEGACY_ASR_LANGUAGE = "asr_language"
    private const val KEY_ASR_LANGUAGE_TAG = "asr_language_tag"
    private const val KEY_MODEL_ETAG = "model_etag"

    /** Synchronous read for use in Activity.attachBaseContext(), before any UI exists. */
    fun languageTagBlocking(context: Context): String =
      context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_LANGUAGE, null) ?: AppLanguage.ENGLISH.tag
  }
}
