package com.jedick.langcalc.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.jedick.langcalc.R

/**
 * Copies [text] to the clipboard. Android 13 and later show their own "Copied" confirmation when
 * an app sets the clipboard, so the app shows its own short message only on earlier versions.
 */
fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
  clipboard.setPrimaryClip(ClipData.newPlainText("LangCalc", text))
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    Toast.makeText(context, R.string.chat_copied, Toast.LENGTH_SHORT).show()
  }
}
