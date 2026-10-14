package com.jedick.langcalc.speech

import android.content.Context
import android.content.res.Resources

/**
 * The speech-recognition languages the app offers, as BCP-47 tags for RecognizerIntent.EXTRA_LANGUAGE.
 *
 * The list is fixed because the speech dialog belongs to the system (see [SpeechInput]) and its
 * recognizer does not report which language packs are installed.
 */
object SpeechLanguages {
  const val ENGLISH = "en-US"
  const val CHINESE = "zh-CN"

  val LANGUAGES = listOf(ENGLISH, CHINESE)

  /**
   * The language to use until the user picks one: Chinese if the app's interface language or the
   * phone's first language is Chinese, English otherwise.
   */
  fun defaultTag(context: Context): String {
    val appLanguage = context.resources.configuration.locales[0].language
    val phoneLanguage = Resources.getSystem().configuration.locales[0].language
    return if (appLanguage == "zh" || phoneLanguage == "zh") CHINESE else ENGLISH
  }

  /** [saved] if it is one of the offered languages (an old or unknown value is not), else the default. */
  fun initialTag(context: Context, saved: String?): String = saved?.takeIf { it in LANGUAGES } ?: defaultTag(context)
}
