package com.jedick.langcalc.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jedick.langcalc.R
import com.jedick.langcalc.speech.SpeechLanguages
import java.util.Locale

/**
 * The speech-recognition language picker shown next to the mic button. It always shows the chosen
 * language (a BCP-47 tag such as "en-US") and lists the languages in [SpeechLanguages].
 */
@Composable
fun VoiceLanguageButton(selectedTag: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(start = 8.dp, end = 2.dp)) {
      Text(selectedTag, style = MaterialTheme.typography.labelLarge)
      Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.chat_voice_language_content_description))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      for (tag in SpeechLanguages.LANGUAGES) {
        DropdownMenuItem(
          text = { Text(Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault())) },
          leadingIcon = { if (tag == selectedTag) Icon(Icons.Filled.Check, contentDescription = null) },
          onClick = {
            onSelect(tag)
            expanded = false
          },
        )
      }
    }
  }
}
