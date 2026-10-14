package com.jedick.langcalc.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Builds the request for the system's on-device speech-recognition UI (the same
 * `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` dialog every Android app uses -- there is no reason
 * to hand-roll a custom `RecognitionListener`/audio-level UI when the system one already handles
 * permissions, noise, and the microphone animation).
 *
 * The one thing this app changes from the OS default is the language: passing
 * `RecognizerIntent.EXTRA_LANGUAGE` explicitly is what lets a user pick, say, Chinese recognition
 * even when the device's own recognition-language setting defaults to English and won't switch on
 * its own. A null tag ("Auto") leaves the extra unset and defers to that device default. The
 * choice is made in the chat screen (see VoiceLanguageButton); the languages offered come from
 * [SpeechLanguages].
 */
object SpeechInput {
  private const val TAG = "SpeechInput"

  fun isAvailable(context: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

  fun buildIntent(promptText: String, languageTag: String?): Intent {
    // Logged so the exact request can be checked with `adb logcat -s SpeechInput`.
    Log.d(TAG, "speech request: language=$languageTag, model=free_form")
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
      languageTag?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
    }
  }

  /** Extracts the top transcription from the ACTION_RECOGNIZE_SPEECH activity result, if any. */
  fun firstResult(data: Intent?): String? =
    data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
}
