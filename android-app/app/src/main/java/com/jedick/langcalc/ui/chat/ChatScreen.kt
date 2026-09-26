package com.jedick.langcalc.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jedick.langcalc.R
import com.jedick.langcalc.data.AppSettings
import com.jedick.langcalc.engine.CalcExpressionEvaluator.EvalErrorType
import com.jedick.langcalc.engine.CalcTurn
import com.jedick.langcalc.engine.CalcTurnStatus
import com.jedick.langcalc.speech.SpeechInput
import com.jedick.langcalc.ui.common.AppTopBar
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(state: ChatState, settings: AppSettings, onSettingsClick: () -> Unit) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val listState = rememberLazyListState()
  var inputText by remember { mutableStateOf("") }
  var micPermissionGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  var showNoAsrDialog by remember { mutableStateOf(false) }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      micPermissionGranted = granted
    }

  val speechLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      SpeechInput.firstResult(result.data)?.let { transcript ->
        scope.launch { state.send(transcript) }
      }
    }

  LaunchedEffect(state.turns.size, state.processing) {
    if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.size - 1)
  }

  val send: (String) -> Unit = { text ->
    inputText = ""
    scope.launch { state.send(text) }
  }

  val startVoiceInput = {
    when {
      !micPermissionGranted -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      !SpeechInput.isAvailable(context) -> showNoAsrDialog = true
      else -> {
        val prompt = context.getString(R.string.chat_input_placeholder)
        speechLauncher.launch(SpeechInput.buildIntent(prompt, settings.asrLanguage))
      }
    }
  }

  Scaffold(topBar = { AppTopBar(title = stringResource(R.string.chat_welcome_title), onSettingsClick = onSettingsClick) }) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
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
              isEditing = state.editingTurnId == turn.id,
              onStartEdit = { state.setEditingTurn(turn.id) },
              onCommitEdit = { newExpression ->
                state.updateExpression(turn.id, newExpression)
                state.setEditingTurn(null)
              },
              onCancelEdit = { state.setEditingTurn(null) },
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

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
          singleLine = true,
          enabled = !state.processing,
        )
        FloatingActionButton(onClick = startVoiceInput, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.chat_mic_content_description))
        }
        IconButton(onClick = { if (inputText.isNotBlank()) send(inputText) }, enabled = !state.processing && inputText.isNotBlank()) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send_content_description))
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

@Composable
private fun CalcTurnRow(
  turn: CalcTurn,
  isEditing: Boolean,
  onStartEdit: () -> Unit,
  onCommitEdit: (String) -> Unit,
  onCancelEdit: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    // User bubble, right-aligned.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      Text(
        turn.userText,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier =
          Modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .widthIn(max = 280.dp),
      )
    }

    // Calculator field, left-aligned and always editable.
    Column(
      modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).widthIn(max = 320.dp)
    ) {
      CalcFieldLabel(stringResource(R.string.chat_calculator_label))
      if (isEditing) {
        EditableExpressionField(initialValue = turn.expression, onCommit = onCommitEdit, onCancel = onCancelEdit)
      } else {
        Text(
          turn.expression.ifEmpty { stringResource(R.string.chat_expression_placeholder) },
          style = MaterialTheme.typography.headlineSmall,
          color = if (turn.expression.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth().clickable(onClick = onStartEdit).padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
    }

    // Result field. Hidden while the Calculator field is open for editing.
    if (!isEditing) {
      Column(
        modifier =
          Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).widthIn(max = 320.dp)
      ) {
        CalcFieldLabel(stringResource(R.string.chat_result_label))
        Text(
          resultTextFor(turn),
          style = MaterialTheme.typography.headlineMedium,
          color = if (turn.status == CalcTurnStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
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
    CalcTurnStatus.NO_FUNCTION -> if (turn.expression.isEmpty()) stringResource(R.string.chat_no_function_result) else turn.resultText
    else -> turn.resultText
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
private fun EditableExpressionField(initialValue: String, onCommit: (String) -> Unit, onCancel: () -> Unit) {
  var text by remember(initialValue) { mutableStateOf(initialValue) }
  val focusRequester = remember { FocusRequester() }

  // Request focus as soon as this field appears so the cursor shows from the first tap. Combined
  // with readOnly = true below, this never triggers the system soft keyboard -- all edits come
  // through CalculatorKeypad instead.
  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  Column(modifier = Modifier.padding(12.dp)) {
    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      readOnly = true,
      singleLine = true,
      textStyle = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
    Spacer(modifier = Modifier.height(8.dp))
    CalculatorKeypad(
      onKeyPress = { key -> text += key },
      onBackspace = { if (text.isNotEmpty()) text = text.dropLast(1) },
      onClear = { text = "" },
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
      TextButton(onClick = onCancel) { Text(stringResource(R.string.chat_cancel_edit)) }
      Spacer(modifier = Modifier.width(8.dp))
      Button(onClick = { onCommit(text) }) { Text(stringResource(R.string.chat_done_edit)) }
    }
  }
}

@Composable
private fun ChatWelcome(modifier: Modifier = Modifier, processing: Boolean, onExampleClick: (String) -> Unit) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
      Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
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
  )
