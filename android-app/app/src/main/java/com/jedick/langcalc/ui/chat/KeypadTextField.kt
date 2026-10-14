package com.jedick.langcalc.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation

/**
 * A single-line text field that can use either the app's calculator keypad or the Android keyboard.
 *
 * With [useCalculatorKeypad], the Android keyboard is never opened for this field, but the field
 * stays fully editable: the cursor is shown, the cursor can be moved and text selected, and
 * long-pressing offers Paste when the clipboard has text. The keypad itself (see CalculatorPanel)
 * types into [state]. Without it, the field behaves like an ordinary text field.
 *
 * Changing [useCalculatorKeypad] recreates the field (the text stays in [state]), because a
 * text-input session that is already open keeps using the keyboard it started with. Pass
 * [requestFocusOnCreate] to give the new field focus right away, which also opens the Android
 * keyboard when it is wanted.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun KeypadTextField(
  state: TextFieldState,
  useCalculatorKeypad: Boolean,
  textStyle: TextStyle,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  requestFocusOnCreate: Boolean = false,
  imeAction: ImeAction = ImeAction.Default,
  onImeAction: () -> Unit = {},
  onFocusChange: (Boolean) -> Unit = {},
  trailing: (@Composable () -> Unit)? = null,
) {
  key(useCalculatorKeypad) {
    val focusRequester = remember { FocusRequester() }
    // The real controller, before the no-op one below replaces it for the field.
    val keyboardController = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    // Both branches never return: the session either stays open without a keyboard or hands over
    // to the normal Android keyboard.
    val interceptor = remember {
      PlatformTextInputInterceptor { request, nextHandler ->
        if (useCalculatorKeypad) awaitCancellation() else nextHandler.startInputMethod(request)
      }
    }

    LaunchedEffect(Unit) {
      // Close an Android keyboard left open by the previous field when switching to the keypad.
      if (useCalculatorKeypad) keyboardController?.hide()
      if (requestFocusOnCreate) {
        focusRequester.requestFocus()
        if (!useCalculatorKeypad) keyboardController?.show()
      }
    }

    // Tapping a text field calls show() on the keyboard controller, separately from the text input
    // session that the interceptor blocks. After the Android keyboard has been used once, that call
    // opens it again over the keypad, so the keypad's fields get a controller that does nothing.
    WithoutKeyboardController(disabled = useCalculatorKeypad) {
    InterceptPlatformTextInput(interceptor = interceptor) {
      BasicTextField(
        state = state,
        modifier =
          modifier.focusRequester(focusRequester).onFocusChanged {
            focused = it.hasFocus
            onFocusChange(it.hasFocus)
          },
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        onKeyboardAction = KeyboardActionHandler { onImeAction() },
        decorator =
          TextFieldDecorator { innerTextField ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            ) {
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (placeholder != null && state.text.isEmpty()) {
                  Text(placeholder, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                innerTextField()
              }
              trailing?.invoke()
            }
          },
      )
    }
    }
  }
}

@Composable
private fun WithoutKeyboardController(disabled: Boolean, content: @Composable () -> Unit) {
  if (disabled) {
    CompositionLocalProvider(LocalSoftwareKeyboardController provides NoKeyboardController, content = content)
  } else {
    content()
  }
}

/** Stands in for the Android keyboard controller on fields that use the calculator keypad. */
private object NoKeyboardController : SoftwareKeyboardController {
  override fun show() {}

  override fun hide() {}
}
