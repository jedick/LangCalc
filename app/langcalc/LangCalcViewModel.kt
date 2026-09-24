package com.google.ai.edge.gallery.customtasks.langcalc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ToolProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LangCalcViewModel"

/** UI state for the Voice Calculator chat list. */
data class LangCalcUiState(
  val turns: List<CalcTurn> = listOf(),
  val processing: Boolean = false,
  val editingTurnId: Long? = null,
)

@HiltViewModel
class LangCalcViewModel @Inject constructor() : ViewModel() {
  private val _uiState = MutableStateFlow(LangCalcUiState())
  val uiState = _uiState.asStateFlow()

  /**
   * Sends [userText] to FunctionGemma and appends a new turn once a function call (or the lack of
   * one) is resolved.
   *
   * [curActions] is the task-level list that `LangCalcTools.onFunctionCalled` appends to as a
   * side effect of the model's tool call (see LangCalcTask.kt); it's cleared before every prompt.
   */
  fun send(
    model: Model,
    tools: List<ToolProvider>,
    userText: String,
    curActions: MutableList<CalcAction>,
    onError: (String) -> Unit,
  ) {
    if (userText.isBlank() || model.instance == null) return

    val turnId = nextCalcTurnId()
    _uiState.update {
      it.copy(
        turns =
          it.turns + CalcTurn(id = turnId, userText = userText, status = CalcTurnStatus.PENDING),
        processing = true,
        editingTurnId = null,
      )
    }
    curActions.clear()

    viewModelScope.launch(Dispatchers.Default) {
      var failed = false
      val instance = model.instance as LlmModelInstance

      instance.conversation
        .sendMessageAsync(Contents.of(listOf(Content.Text(userText))))
        .catch { e ->
          failed = true
          Log.e(TAG, "Inference failed", e)
          onError(e.message ?: "Unknown error")
        }
        // The model's follow-up natural-language reply streams through here too, once the tool
        // call above has been auto-executed, but we intentionally never read it -- the calculator
        // only cares about the CalcAction captured in curActions. See LangCalcTools.kt for how to
        // skip generating this follow-up turn entirely if you want the latency back.
        .collect {}

      if (failed) {
        markTurnError(turnId, "Inference failed. Try resetting the model from the app bar.")
      } else {
        resolveTurn(turnId, curActions)
      }

      // Each calculation is independent, so start the next turn with a clean context -- same
      // approach as MobileActionsViewModel.
      LlmChatModelHelper.resetConversation(
        model = model,
        supportImage = false,
        supportAudio = false,
        systemInstruction = systemPromptFor(model),
        tools = tools,
      )
    }
  }

  private fun resolveTurn(turnId: Long, curActions: List<CalcAction>) {
    val action = curActions.firstOrNull()
    _uiState.update { state ->
      state.copy(
        processing = false,
        turns =
          state.turns.map { turn ->
            when {
              turn.id != turnId -> turn
              action != null -> turn.withExpression(action.toExpression())
              else -> turn.copy(status = CalcTurnStatus.NO_FUNCTION)
            }
          },
      )
    }
  }

  private fun markTurnError(turnId: Long, message: String) {
    _uiState.update { state ->
      state.copy(
        processing = false,
        turns =
          state.turns.map { turn ->
            if (turn.id == turnId) {
              turn.copy(status = CalcTurnStatus.ERROR, errorMessage = message)
            } else {
              turn
            }
          },
      )
    }
  }

  /** Called when the user edits (or manually fills in) the Calculator field for [turnId]. */
  fun updateExpression(turnId: Long, expression: String) {
    _uiState.update { state ->
      state.copy(
        turns =
          state.turns.map { turn ->
            if (turn.id == turnId) turn.withExpression(expression) else turn
          }
      )
    }
  }

  fun setEditingTurn(turnId: Long?) {
    _uiState.update { it.copy(editingTurnId = turnId) }
  }

  fun reset() {
    _uiState.update { LangCalcUiState() }
  }
}
