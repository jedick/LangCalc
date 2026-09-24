package com.google.ai.edge.gallery.customtasks.langcalc

import java.util.concurrent.atomic.AtomicLong

private val turnIdGenerator = AtomicLong(0L)

/** Generates a unique, stable id for a new chat turn (used as the LazyColumn item key). */
fun nextCalcTurnId(): Long = turnIdGenerator.incrementAndGet()

enum class CalcTurnStatus {
  /** Waiting for FunctionGemma to produce a function call. */
  PENDING,
  /** A function call was recognized (or the user has since edited the expression) and it evaluates cleanly. */
  RESOLVED,
  /** No function call was recognized; the Calculator field is left blank for manual entry. */
  NO_FUNCTION,
  /** The current expression fails to evaluate (bad edit, divide-by-zero, or an inference error). */
  ERROR,
}

/**
 * One turn of the chat: the user's query, and the Calculator/Result pair derived from it.
 *
 * The Calculator field ([expression]) is always editable, regardless of [status] -- even a
 * NO_FUNCTION turn shows an empty, tappable expression the user can fill in by hand.
 */
data class CalcTurn(
  val id: Long = nextCalcTurnId(),
  val userText: String,
  val status: CalcTurnStatus,
  val expression: String = "",
  val resultText: String = "",
  val errorMessage: String = "",
)

/** Re-evaluates [expression] and returns a copy of this turn with status/result updated to match. */
fun CalcTurn.withExpression(expression: String): CalcTurn =
  CalcExpressionEvaluator.evaluate(expression)
    .fold(
      onSuccess = { value ->
        copy(
          expression = expression,
          status = CalcTurnStatus.RESOLVED,
          resultText = formatResult(value),
          errorMessage = "",
        )
      },
      onFailure = { error ->
        copy(
          expression = expression,
          status = CalcTurnStatus.ERROR,
          resultText = "",
          errorMessage = error.message ?: "Couldn't evaluate that",
        )
      },
    )
