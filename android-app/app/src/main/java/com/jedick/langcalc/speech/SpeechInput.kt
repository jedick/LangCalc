package com.jedick.langcalc.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jedick.langcalc.data.AsrLanguage

/**
 * Builds the request for the system's on-device speech-recognition UI (the same
 * `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` dialog every Android app uses -- there is no reason
 * to hand-roll a custom `RecognitionListener`/audio-level UI when the system one already handles
 * permissions, noise, and the microphone animation).
 *
 * The one thing this app changes from the OS default is [AsrLanguage]: passing
 * `RecognizerIntent.EXTRA_LANGUAGE` explicitly is what lets a user pick Chinese recognition even
 * when the device's own recognition-language setting (in the Google app, Samsung's voice input, or
 * whichever ASR service is installed) defaults to English or otherwise won't switch on its own.
 * "Auto" leaves the extra unset and defers to that device default, for anyone who'd rather match
 * whatever they already configured system-wide.
 */
object SpeechInput {
  fun isAvailable(context: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

  fun buildIntent(promptText: String, asrLanguage: AsrLanguage): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
      asrLanguage.locale?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
    }

  /** Extracts the top transcription from the ACTION_RECOGNIZE_SPEECH activity result, if any. */
  fun firstResult(data: Intent?): String? =
    data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
}
