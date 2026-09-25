package com.google.ai.edge.gallery.customtasks.langcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val KEY_ROWS =
  listOf(
    listOf("(", ")", "C", "⌫"),
    listOf("7", "8", "9", "/"),
    listOf("4", "5", "6", "*"),
    listOf("1", "2", "3", "-"),
    listOf("0", ".", "+", ""),
  )

/**
 * A minimal numeric keypad for editing the Calculator field -- plain Compose buttons, no extra
 * libraries required.
 */
@Composable
fun CalculatorKeypad(onKeyPress: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    for (row in KEY_ROWS) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (key in row) {
          if (key.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            continue
          }
          OutlinedButton(
            onClick = {
              when (key) {
                "C" -> onClear()
                "⌫" -> onBackspace()
                else -> onKeyPress(key)
              }
            },
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.weight(1f),
          ) {
            if (key == "⌫") {
              Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Backspace")
            } else {
              Text(key, style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }
    }
  }
}
