package com.jedick.langcalc.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jedick.langcalc.engine.CalcTurn
import com.jedick.langcalc.engine.CalcTurnStatus
import com.jedick.langcalc.engine.LangCalcEngine
import com.jedick.langcalc.engine.SendResult
import com.jedick.langcalc.engine.nextCalcTurnId
import com.jedick.langcalc.engine.withExpression

/**
 * Chat list state and the send/edit logic. A plain class (not an androidx ViewModel) for the same
 * reason as [com.jedick.langcalc.ui.intro.IntroState]: it's `remember`-ed once at the top of the
 * navigation graph and this app relies on the Activity surviving configuration changes rather than
 * on ViewModel's rotation-survival guarantees.
 */
class ChatState(private val engine: LangCalcEngine) {
  var turns by mutableStateOf<List<CalcTurn>>(emptyList())
    private set

  var processing by mutableStateOf(false)
    private set

  var editingTurnId by mutableStateOf<Long?>(null)
    private set

  var errorMessage by mutableStateOf<String?>(null)

  /** Sends [userText] to the model and appends a new turn once it resolves. */
  suspend fun send(userText: String) {
    if (userText.isBlank() || processing) return

    val turnId = nextCalcTurnId()
    turns = turns + CalcTurn(id = turnId, userText = userText, status = CalcTurnStatus.PENDING)
    processing = true
    editingTurnId = null

    when (val result = engine.send(userText)) {
      is SendResult.Resolved -> {
        turns = turns.map { turn -> if (turn.id == turnId) turn.withExpression(result.action.toExpression()) else turn }
      }
      is SendResult.NoFunction -> {
        turns = turns.map { turn -> if (turn.id == turnId) turn.copy(status = CalcTurnStatus.NO_FUNCTION) else turn }
      }
      is SendResult.Error -> {
        turns = turns.map { turn -> if (turn.id == turnId) turn.copy(status = CalcTurnStatus.ERROR, errorType = null) else turn }
        errorMessage = result.message
      }
    }
    processing = false
  }

  /** Called when the user edits (or manually fills in) the Calculator field for [turnId]. */
  fun updateExpression(turnId: Long, expression: String) {
    turns = turns.map { turn -> if (turn.id == turnId) turn.withExpression(expression) else turn }
  }

  fun setEditingTurn(turnId: Long?) {
    editingTurnId = turnId
  }

  fun dismissError() {
    errorMessage = null
  }
}
