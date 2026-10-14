package com.jedick.langcalc.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jedick.langcalc.R
import com.jedick.langcalc.data.AppSettings
import com.jedick.langcalc.data.LocaleHelper
import com.jedick.langcalc.engine.CalcExpressionEvaluator.EvalErrorType
import com.jedick.langcalc.engine.CalcTurn
import com.jedick.langcalc.engine.CalcTurnStatus
import com.jedick.langcalc.speech.SpeechInput
import com.jedick.langcalc.speech.SpeechLanguages
import com.jedick.langcalc.ui.common.AppIcon
import com.jedick.langcalc.ui.common.AppTopBar
import com.jedick.langcalc.ui.common.copyToClipboard
import kotlinx.coroutines.launch

/**
 * The Android keyboard's own show/hide animation (InsetsController in AOSP): 285 ms with a strong
 * deceleration. The keypad uses the same timing so the two feel alike when switching.
 */
private const val KEYPAD_ANIMATION_MS = 285
private val KeypadEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val keypadEnter =
  slideInVertically(tween<IntOffset>(KEYPAD_ANIMATION_MS, easing = KeypadEasing)) { it } +
    expandVertically(tween<IntSize>(KEYPAD_ANIMATION_MS, easing = KeypadEasing))
private val keypadExit =
  slideOutVertically(tween<IntOffset>(KEYPAD_ANIMATION_MS, easing = KeypadEasing)) { it } +
    shrinkVertically(tween<IntSize>(KEYPAD_ANIMATION_MS, easing = KeypadEasing))

@Composable
fun ChatScreen(state: ChatState, settings: AppSettings, onSettingsClick: () -> Unit, onHelpClick: () -> Unit) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  var micPermissionGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  var showNoAsrDialog by remember { mutableStateOf(false) }
  var asrTag by remember { mutableStateOf(SpeechLanguages.initialTag(context, settings.asrLanguageTag)) }
  var askFocused by remember { mutableStateOf(false) }
  // Set when the user switches keyboards, so the new input field takes focus (see KeypadTextField).
  var focusAfterSwitch by remember { mutableStateOf(false) }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      micPermissionGranted = granted
    }

  val speechLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      SpeechInput.firstResult(result.data)?.let { transcript ->
        scope.launch { state.send(transcript, speechLanguageTag = asrTag) }
      }
    }

  val editingTurn = state.turns.firstOrNull { it.id == state.editingTurnId }
  // The text being edited lives here, not in the turn's row, because the keypad that types into it
  // is at the bottom of the screen, outside the list.
  val editState = remember(editingTurn?.id) { TextFieldState(editingTurn?.expression ?: "") }

  val askUsesCalculator = state.keyboardMode == KeyboardMode.CALCULATOR
  val panelVisible = editingTurn != null || (askFocused && askUsesCalculator)

  // Like the Android keyboard, the keypad closes with the Back button.
  BackHandler(enabled = panelVisible) {
    if (editingTurn != null) state.setEditingTurn(null) else focusManager.clearFocus()
  }

  // Keep the turn being edited, or else the latest turn, in view when the keypad opens or closes.
  LaunchedEffect(state.turns.size, state.processing, panelVisible, state.editingTurnId) {
    val editingIndex = state.turns.indexOfFirst { it.id == state.editingTurnId }
    when {
      editingIndex >= 0 -> listState.animateScrollToItem(editingIndex)
      state.turns.isNotEmpty() -> listState.animateScrollToItem(state.turns.size - 1)
    }
  }

  val send: (String) -> Unit = { text ->
    if (text.isNotBlank() && !state.processing) {
      state.inputField.clearText()
      scope.launch { state.send(text) }
    }
  }

  val startVoiceInput = {
    when {
      !micPermissionGranted -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      !SpeechInput.isAvailable(context) -> showNoAsrDialog = true
      else -> {
        val prompt = context.getString(R.string.chat_input_placeholder)
        speechLauncher.launch(SpeechInput.buildIntent(prompt, asrTag))
      }
    }
  }

  Scaffold(
    topBar = { AppTopBar(title = stringResource(R.string.chat_welcome_title), onSettingsClick = onSettingsClick, onHelpClick = onHelpClick) }
  ) { padding ->
    // consumeWindowInsets: the bottom padding already covers the navigation bar, so imePadding only
    // adds the part of the keyboard that is above it.
    Column(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
      if (state.turns.isEmpty()) {
        ChatWelcome(modifier = Modifier.weight(1f), onExampleClick = send, processing = state.processing)
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(vertical = 16.dp),
        ) {
          items(items = state.turns, key = { it.id }) { turn ->
            CalcTurnRow(
              turn = turn,
              editState = if (state.editingTurnId == turn.id) editState else null,
              onStartEdit = { state.setEditingTurn(turn.id) },
              onCopy = { text -> copyToClipboard(context, text) },
            )
          }
          if (state.processing) {
            item(key = "loading") {
              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              }
            }
          }
        }
      }

      // The input row is replaced by the keypad's own buttons while a Calculator field is edited.
      if (editingTurn == null) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          KeypadTextField(
            state = state.inputField,
            useCalculatorKeypad = askUsesCalculator,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            placeholder = stringResource(R.string.chat_input_hint),
            requestFocusOnCreate = focusAfterSwitch,
            imeAction = ImeAction.Send,
            onImeAction = { send(state.inputField.text.toString()) },
            onFocusChange = { askFocused = it },
            trailing = {
              KeyboardSwitcher(
                calculatorActive = askUsesCalculator,
                onClick = {
                  focusAfterSwitch = true
                  state.keyboardMode = if (askUsesCalculator) KeyboardMode.SYSTEM else KeyboardMode.CALCULATOR
                },
              )
            },
          )
          VoiceLanguageButton(
            selectedTag = asrTag,
            onSelect = { tag ->
              asrTag = tag
              settings.asrLanguageTag = tag
            },
          )
          FloatingActionButton(onClick = startVoiceInput, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.chat_mic_content_description))
          }
        }
      }

      // The calculator keyboard, pinned below the chat like the Android keyboard. It slides in from
      // the bottom with the timing of the Android keyboard (see KEYPAD_ANIMATION_MS), and the chat
      // list above it shrinks in step. The two panels are separate so each keeps its own content
      // while it slides out.
      AnimatedVisibility(visible = editingTurn != null, enter = keypadEnter, exit = keypadExit) {
        CalculatorPanel(target = editState) {
          TextButton(onClick = { state.setEditingTurn(null) }) { Text(stringResource(R.string.chat_cancel_edit)) }
          Button(
            onClick = {
              editingTurn?.let { state.updateExpression(it.id, editState.text.toString()) }
              state.setEditingTurn(null)
            }
          ) {
            Text(stringResource(R.string.chat_done_edit))
          }
        }
      }
      AnimatedVisibility(visible = editingTurn == null && askFocused && askUsesCalculator, enter = keypadEnter, exit = keypadExit) {
        CalculatorPanel(target = state.inputField) {
          Button(
            onClick = { send(state.inputField.text.toString()) },
            enabled = !state.processing && state.inputField.text.isNotBlank(),
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.chat_send_content_description))
          }
        }
      }
    }
  }

  state.errorMessage?.let { message ->
    AlertDialog(
      onDismissRequest = { state.dismissError() },
      title = { Text(stringResource(R.string.chat_error_dialog_title)) },
      text = { Text(message) },
      confirmButton = { TextButton(onClick = { state.dismissError() }) { Text(stringResource(R.string.chat_error_dialog_ok)) } },
    )
  }

  if (showNoAsrDialog) {
    AlertDialog(
      onDismissRequest = { showNoAsrDialog = false },
      title = { Text(stringResource(R.string.chat_no_asr_title)) },
      text = { Text(stringResource(R.string.chat_no_asr_body)) },
      confirmButton = { TextButton(onClick = { showNoAsrDialog = false }) { Text(stringResource(R.string.generic_ok)) } },
    )
  }
}

/** The button inside the input field that switches between the calculator and Android keyboards. */
@Composable
private fun KeyboardSwitcher(calculatorActive: Boolean, onClick: () -> Unit) {
  val switchToSystemDescription = stringResource(R.string.chat_keyboard_to_system)
  Box(
    modifier = Modifier.size(width = 48.dp, height = 36.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    if (calculatorActive) {
      Text(
        "ABC",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { contentDescription = switchToSystemDescription },
      )
    } else {
      Icon(
        Icons.Filled.Calculate,
        contentDescription = stringResource(R.string.chat_keyboard_to_calculator),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcTurnRow(turn: CalcTurn, editState: TextFieldState?, onStartEdit: () -> Unit, onCopy: (String) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    // User bubble, right-aligned. Tap to copy the request.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      Text(
        turn.userText,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier =
          Modifier.clip(RoundedCornerShape(16.dp))
            .clickable { onCopy(turn.userText) }
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .widthIn(max = 280.dp),
      )
    }

    // Calculator field, left-aligned and always editable. Tap to edit, long-tap to copy.
    Column(
      modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).widthIn(max = 320.dp)
    ) {
      CalcFieldLabel(stringResource(R.string.chat_calculator_label))
      if (editState != null) {
        // The keypad that types into this field is at the bottom of the screen (see ChatScreen).
        KeypadTextField(
          state = editState,
          useCalculatorKeypad = true,
          textStyle = MaterialTheme.typography.headlineSmall,
          requestFocusOnCreate = true,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
      } else {
        Text(
          turn.expression.ifEmpty { stringResource(R.string.chat_expression_placeholder) },
          style = MaterialTheme.typography.headlineSmall,
          color = if (turn.expression.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.fillMaxWidth()
              .combinedClickable(
                onClick = onStartEdit,
                onLongClick = { if (turn.expression.isNotEmpty()) onCopy(turn.expression) },
              )
              .padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
    }

    // Result field. Hidden while the Calculator field is open for editing. Tap to copy a result.
    if (editState == null) {
      Column(
        modifier =
          Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).widthIn(max = 320.dp)
      ) {
        CalcFieldLabel(stringResource(R.string.chat_result_label))
        Text(
          resultTextFor(turn),
          style =
            if (turn.status == CalcTurnStatus.DECLINED) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.headlineMedium,
          color =
            when (turn.status) {
              CalcTurnStatus.ERROR -> MaterialTheme.colorScheme.error
              CalcTurnStatus.DECLINED -> MaterialTheme.colorScheme.onSurfaceVariant
              else -> MaterialTheme.colorScheme.onSurface
            },
          modifier =
            Modifier.fillMaxWidth()
              .clickable(enabled = turn.status == CalcTurnStatus.RESOLVED) { onCopy(turn.resultText) }
              .padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun resultTextFor(turn: CalcTurn): String =
  when (turn.status) {
    CalcTurnStatus.ERROR ->
      when (turn.errorType) {
        EvalErrorType.DIVIDE_BY_ZERO -> stringResource(R.string.chat_divide_by_zero)
        EvalErrorType.SYNTAX -> stringResource(R.string.chat_eval_error)
        null -> stringResource(R.string.chat_inference_error)
      }
    CalcTurnStatus.DECLINED -> declinedText(turn)
    CalcTurnStatus.NO_FUNCTION -> if (turn.expression.isEmpty()) stringResource(R.string.chat_no_function_result) else turn.resultText
    else -> turn.resultText
  }

/**
 * "Declined request: <reason>" (or just "Declined request" without a reason). This follows the
 * language of the request, not the interface language, since the reason itself comes from the
 * model in the request's language. For a spoken request that is the speech-recognition language
 * when it is English or Chinese; otherwise it is guessed from the text (see LocaleHelper).
 */
@Composable
private fun declinedText(turn: CalcTurn): String {
  val context = LocalContext.current
  return remember(context, turn.userText, turn.speechLanguageTag, turn.declineReason) {
    val tag = LocaleHelper.requestLanguageTag(turn.userText, turn.speechLanguageTag)
    val reason = turn.declineReason
    if (reason == null) {
      LocaleHelper.getString(context, tag, R.string.chat_declined_request)
    } else {
      LocaleHelper.getString(context, tag, R.string.chat_declined_request_reason, reason)
    }
  }
}

@Composable
private fun CalcFieldLabel(text: String) {
  Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 14.dp, top = 8.dp),
  )
}

@Composable
private fun ChatWelcome(modifier: Modifier = Modifier, processing: Boolean, onExampleClick: (String) -> Unit) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
      AppIcon(size = 64.dp)
      Spacer(modifier = Modifier.height(8.dp))
      Text(stringResource(R.string.chat_welcome_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
      Text(
        stringResource(R.string.chat_welcome_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Column(modifier = Modifier.padding(top = 40.dp)) {
        Text(
          stringResource(R.string.chat_supported_ops),
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(bottom = 8.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (item in calcOperationItems()) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
              contentAlignment = Alignment.Center,
            ) {
              Text(item.second, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.first, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
      Spacer(modifier = Modifier.height(32.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        for (example in listOf(stringResource(R.string.chat_example_1), stringResource(R.string.chat_example_2))) {
          Text(
            example,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !processing) { onExampleClick(example) }
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(12.dp))
                .padding(all = 12.dp),
          )
        }
      }
    }
  }
}

/** (label, symbol) pairs for the welcome screen's "Supported calculations" list. */
@Composable
private fun calcOperationItems(): List<Pair<String, String>> =
  listOf(
    stringResource(R.string.chat_op_add) to "+",
    stringResource(R.string.chat_op_subtract) to "\u2212",
    stringResource(R.string.chat_op_multiply) to "\u00D7",
    stringResource(R.string.chat_op_divide) to "\u00F7",
    stringResource(R.string.chat_op_percent) to "%",
  )
