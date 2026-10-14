package com.jedick.langcalc.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class KeyKind {
  DIGIT,
  OPERATOR,
  OTHER,
}

/** A key: what it shows, what it types (if different), and how it is drawn. */
private data class Key(val label: String, val kind: KeyKind, val text: String = label)

private const val BACKSPACE = "\u232B"

private val KEY_ROWS =
  listOf(
    listOf(Key("(", KeyKind.OTHER), Key(")", KeyKind.OTHER), Key("C", KeyKind.OTHER), Key(BACKSPACE, KeyKind.OTHER)),
    // The operator keys show the usual symbols but type the characters the evaluator reads.
    listOf(Key("7", KeyKind.DIGIT), Key("8", KeyKind.DIGIT), Key("9", KeyKind.DIGIT), Key("\u00F7", KeyKind.OPERATOR, "/")),
    listOf(Key("4", KeyKind.DIGIT), Key("5", KeyKind.DIGIT), Key("6", KeyKind.DIGIT), Key("\u00D7", KeyKind.OPERATOR, "*")),
    listOf(Key("1", KeyKind.DIGIT), Key("2", KeyKind.DIGIT), Key("3", KeyKind.DIGIT), Key("\u2212", KeyKind.OPERATOR, "-")),
    listOf(Key("0", KeyKind.DIGIT), Key(".", KeyKind.DIGIT), Key("+", KeyKind.OPERATOR), Key("%", KeyKind.OPERATOR)),
  )

/**
 * A minimal numeric keypad for editing the Calculator field -- plain Compose buttons, no extra
 * libraries required.
 *
 * The % key does not type a character. It calls [onPercent] to toggle the percent form of the
 * last operation (see PercentKey) and is disabled when [percentEnabled] is false.
 */
@Composable
fun CalculatorKeypad(
  onKeyPress: (String) -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  percentEnabled: Boolean,
  onPercent: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    for (row in KEY_ROWS) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (key in row) {
          // Digits are drawn in the plain text color so that the operators, in the accent color
          // and larger, stand out from them.
          val isOperator = key.kind == KeyKind.OPERATOR
          val color = if (isOperator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          val enabled = key.label != "%" || percentEnabled
          OutlinedButton(
            onClick = {
              when (key.label) {
                "C" -> onClear()
                BACKSPACE -> onBackspace()
                "%" -> onPercent()
                else -> onKeyPress(key.text)
              }
            },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            border = BorderStroke(if (isOperator && enabled) 1.5.dp else 1.dp, if (isOperator && enabled) color else MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.weight(1f),
          ) {
            if (key.label == BACKSPACE) {
              Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Backspace")
            } else {
              Text(key.label, style = labelStyle(key), fontWeight = if (isOperator || key.label == ".") FontWeight.Bold else null)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun labelStyle(key: Key): TextStyle =
  when {
    key.kind == KeyKind.OPERATOR -> MaterialTheme.typography.headlineMedium
    key.label == "." -> MaterialTheme.typography.headlineMedium
    key.kind == KeyKind.DIGIT -> MaterialTheme.typography.titleLarge
    else -> MaterialTheme.typography.titleMedium
  }
