package com.google.ai.edge.gallery.customtasks.langcalc

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.getTaskIconColor
import com.google.ai.edge.gallery.ui.common.textandvoiceinput.HoldToDictateViewModel
import com.google.ai.edge.gallery.ui.common.textandvoiceinput.TextAndVoiceInput
import com.google.ai.edge.gallery.ui.common.textandvoiceinput.VoiceRecognizerOverlay
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.ToolProvider

/**
 * The Voice Calculator screen: a scrolling chat of (User / Calculator / Result) turns, with a
 * text-or-voice input bar at the bottom -- structurally similar to Ask Image or Agent Skills, but
 * with our own turn rendering instead of the generic ChatView, since a turn here is three linked
 * fields rather than a single message. Tapping a Calculator field re-opens it for editing; editing
 * always recomputes the Result immediately, without going back to the model.
 */
@Composable
fun LangCalcScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  bottomPadding: Dp,
  setAppBarControlsDisabled: (Boolean) -> Unit,
  curActions: SnapshotStateList<CalcAction>,
  tools: List<ToolProvider>,
  viewModel: LangCalcViewModel = hiltViewModel(),
) {
  var recordAudioPermissionGranted by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val recordAudioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      recordAudioPermissionGranted = granted
    }

  LaunchedEffect(Unit) {
    when (PackageManager.PERMISSION_GRANTED) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
        recordAudioPermissionGranted = true
      }
      else -> recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  if (recordAudioPermissionGranted) {
    LangCalcMainUi(
      task = task,
      modelManagerViewModel = modelManagerViewModel,
      bottomPadding = bottomPadding,
      setAppBarControlsDisabled = setAppBarControlsDisabled,
      curActions = curActions,
      tools = tools,
      viewModel = viewModel,
    )
  }
}

@Composable
private fun LangCalcMainUi(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  bottomPadding: Dp,
  setAppBarControlsDisabled: (Boolean) -> Unit,
  curActions: SnapshotStateList<CalcAction>,
  tools: List<ToolProvider>,
  viewModel: LangCalcViewModel,
  holdToDictateViewModel: HoldToDictateViewModel = hiltViewModel(),
) {
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val model = modelManagerUiState.selectedModel
  val uiState by viewModel.uiState.collectAsState()
  val holdToDictateUiState by holdToDictateViewModel.uiState.collectAsState()
  val listState = rememberLazyListState()
  var curAmplitude by remember { mutableIntStateOf(0) }
  var clearInputTextTrigger by remember { mutableLongStateOf(0L) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  setAppBarControlsDisabled(uiState.processing)

  // Auto-scroll to the newest turn as it's added or resolved.
  LaunchedEffect(uiState.turns.size, uiState.processing) {
    if (uiState.turns.isNotEmpty()) {
      listState.animateScrollToItem(uiState.turns.size - 1)
    }
  }

  if (!modelManagerUiState.isModelInitialized(model = model)) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = 3.dp,
        modifier = Modifier.size(24.dp),
      )
    }
    return
  }

  val send: (String) -> Unit = { text ->
    clearInputTextTrigger = System.currentTimeMillis()
    viewModel.send(
      model = model,
      tools = tools,
      userText = text,
      curActions = curActions,
      onError = { errorMessage = it },
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            bottom =
              if (WindowInsets.ime.getBottom(LocalDensity.current) == 0) bottomPadding else 8.dp
          )
          .imePadding()
    ) {
      if (uiState.turns.isEmpty()) {
        LangCalcWelcome(task = task, modifier = Modifier.weight(1f))
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(vertical = 16.dp),
        ) {
          items(items = uiState.turns, key = { it.id }) { turn ->
            CalcTurnRow(
              turn = turn,
              isEditing = uiState.editingTurnId == turn.id,
              onStartEdit = { viewModel.setEditingTurn(turn.id) },
              onCommitEdit = { newExpression ->
                viewModel.updateExpression(turn.id, newExpression)
                viewModel.setEditingTurn(null)
              },
              onCancelEdit = { viewModel.setEditingTurn(null) },
            )
          }
          if (uiState.processing) {
            item(key = "loading") { CalcLoadingRow() }
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // A couple of example prompts -- one straightforward, one that needs more interpretation.
        Row(
          modifier =
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).graphicsLayer {
              alpha = if (uiState.processing) 0.5f else 1f
            },
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Spacer(modifier = Modifier.width(12.dp))
          for (item in CALC_PROMPT_EXAMPLES) {
            Text(
              item,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelLarge,
              modifier =
                Modifier.clip(RoundedCornerShape(12.dp))
                  .clickable(enabled = !uiState.processing) { send(item) }
                  .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .padding(all = 12.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextAndVoiceInput(
            task = task,
            processing = uiState.processing,
            holdToDictateViewModel = holdToDictateViewModel,
            onDone = send,
            onAmplitudeChanged = { curAmplitude = it },
            clearTextTrigger = clearInputTextTrigger,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    AnimatedVisibility(holdToDictateUiState.recognizing) {
      VoiceRecognizerOverlay(
        task = task,
        viewModel = holdToDictateViewModel,
        curAmplitude = curAmplitude,
        bottomPadding = bottomPadding,
      )
    }
  }

  errorMessage?.let { message ->
    AlertDialog(
      onDismissRequest = { errorMessage = null },
      title = { Text("Error") },
      text = { Text(message) },
      confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
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
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // User bubble, right-aligned -- "User: What is 2 times 20?"
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

    // Calculator field, left-aligned and always editable -- "Calculator: 2 * 20"
    Column(
      modifier =
        Modifier.clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .widthIn(max = 320.dp)
    ) {
      CalcFieldLabel("Calculator")
      if (isEditing) {
        EditableExpressionField(
          initialValue = turn.expression,
          onCommit = onCommitEdit,
          onCancel = onCancelEdit,
        )
      } else {
        Text(
          turn.expression.ifEmpty { "Tap to enter an expression" },
          style = MaterialTheme.typography.headlineSmall,
          color =
            if (turn.expression.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.fillMaxWidth()
              .clickable(onClick = onStartEdit)
              .padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
    }

    // Result field -- "Result: 40". Hidden while the Calculator field is open for editing.
    if (!isEditing) {
      Column(
        modifier =
          Modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .widthIn(max = 320.dp)
      ) {
        CalcFieldLabel("Result")
        Text(
          when (turn.status) {
            CalcTurnStatus.ERROR -> turn.errorMessage.ifEmpty { "Couldn't evaluate that" }
            CalcTurnStatus.NO_FUNCTION -> if (turn.expression.isEmpty()) "—" else turn.resultText
            else -> turn.resultText
          },
          style = MaterialTheme.typography.headlineMedium,
          color =
            if (turn.status == CalcTurnStatus.ERROR) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
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
private fun EditableExpressionField(
  initialValue: String,
  onCommit: (String) -> Unit,
  onCancel: () -> Unit,
) {
  var text by remember(initialValue) { mutableStateOf(initialValue) }
  val focusRequester = remember { FocusRequester() }

  // Request focus as soon as this field appears, so the cursor is visible from the first tap on
  // the Calculator field rather than requiring a second tap. Combined with `readOnly = true`
  // below, focusing never triggers the system soft keyboard -- all edits come through
  // CalculatorKeypad instead, so there's no need for both keyboards on screen at once.
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
      TextButton(onClick = onCancel) { Text("Cancel") }
      Spacer(modifier = Modifier.width(8.dp))
      Button(onClick = { onCommit(text) }) { Text("Done") }
    }
  }
}

@Composable
private fun CalcLoadingRow() {
  Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
  }
}

@Composable
private fun LangCalcWelcome(task: Task, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
      Text(
        "Voice Calculator",
        style = MaterialTheme.typography.headlineLarge,
        color = getTaskIconColor(task = task),
      )
      Text(
        "Talk to a calculator",
        style = MaterialTheme.typography.bodyMedium,
        color = getTaskIconColor(task = task),
      )
      Column {
        Text(
          "Supported calculations",
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(top = 64.dp, bottom = 8.dp).graphicsLayer { alpha = 0.7f },
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (item in CALC_OPERATION_ITEMS) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier =
                Modifier.size(24.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surfaceContainerHighest),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                item.symbol,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              item.label,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

/** One row in the welcome screen's "Supported calculations" list. */
private data class CalcOperationItem(val symbol: String, val label: String)

// Rendered as a text glyph in a small circle rather than a vector Icon: Add/Remove/Close cover
// +/-/x reasonably, but there's no standard Material icon for division, so a uniform text badge
// keeps all four visually consistent instead of mixing icons and glyphs.
private val CALC_OPERATION_ITEMS =
  listOf(
    CalcOperationItem(symbol = "+", label = "Add"),
    CalcOperationItem(symbol = "\u2212", label = "Subtract"),
    CalcOperationItem(symbol = "\u00D7", label = "Multiply"),
    CalcOperationItem(symbol = "\u00F7", label = "Divide"),
  )

/** Example prompts shown as tappable chips above the input bar -- one easy, one harder. */
private val CALC_PROMPT_EXAMPLES = listOf("2 plus 20", "take twenty away from two")
