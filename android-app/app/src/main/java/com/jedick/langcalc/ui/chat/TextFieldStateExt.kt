package com.jedick.langcalc.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import com.jedick.langcalc.engine.PercentKey

/** Types [text] at the cursor, replacing the selection if there is one. */
@OptIn(ExperimentalFoundationApi::class)
fun TextFieldState.insertAtCursor(text: String) {
  edit {
    val start = selection.min
    replace(start, selection.max, text)
    selection = TextRange(start + text.length)
  }
}

/** Deletes the selection, or the character before the cursor. (Uses replace: this version of Compose has no public delete.) */
@OptIn(ExperimentalFoundationApi::class)
fun TextFieldState.backspace() {
  edit {
    if (!selection.collapsed) {
      val start = selection.min
      replace(start, selection.max, "")
      selection = TextRange(start)
    } else if (selection.start > 0) {
      val position = selection.start
      replace(position - 1, position, "")
      selection = TextRange(position - 1)
    }
  }
}

/** The % key: see [PercentKey]. The cursor moves to the end of the text. */
@OptIn(ExperimentalFoundationApi::class)
fun TextFieldState.togglePercent() {
  setTextAndPlaceCursorAtEnd(PercentKey.toggle(text.toString()))
}
