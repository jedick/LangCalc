package com.jedick.langcalc.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jedick.langcalc.engine.PercentKey

/**
 * The calculator keyboard: the keypad plus a row of action buttons ([actions]), shown at the bottom
 * of the screen where the Android keyboard would be. The keys type into [target], at its cursor.
 */
@Composable
fun CalculatorPanel(target: TextFieldState, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit) {
  Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      CalculatorKeypad(
        onKeyPress = { key -> target.insertAtCursor(key) },
        onBackspace = { target.backspace() },
        onClear = { target.clearText() },
        percentEnabled = PercentKey.canToggle(target.text.toString()),
        onPercent = { target.togglePercent() },
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = androidx.compose.ui.Alignment.End),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        content = actions,
      )
    }
  }
}
