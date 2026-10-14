package com.jedick.langcalc.engine

/**
 * The four arithmetic operators the calculator understands.
 * [symbol] is what gets written into the editable "Calculator" field, e.g. "2 * 20".
 */
enum class CalcOperator(val symbol: String) {
  ADD("+"),
  SUBTRACT("-"),
  MULTIPLY("*"),
  DIVIDE("/"),
}

/** What the model asked the app to do with a tool call. */
sealed interface ModelAction

/**
 * A recognized calculation: one operator and its two operands.
 *
 * [percent] is true for the percent functions, where the second operand is a percentage of the
 * first: add_percent is (ADD, percent), subtract_percent is (SUBTRACT, percent) and percent_of is
 * (MULTIPLY, percent). There is no percent form of DIVIDE.
 */
data class CalcAction(
  val operator: CalcOperator,
  val x: Double,
  val y: Double,
  val percent: Boolean = false,
) : ModelAction {
  /** Renders this call as an editable expression for the Calculator field, e.g. "2 * 20" or "50 + 30%". */
  fun toExpression(): String =
    "${formatOperand(x)} ${operator.symbol} ${formatOperand(y)}" + if (percent) "%" else ""
}

/** The model declined the request (decline function). [reason] is null if none was given. */
data class DeclineAction(val reason: String?) : ModelAction

/** Formats a Double without a trailing ".0" for whole numbers, e.g. 20.0 -> "20". */
fun formatOperand(value: Double): String =
  if (value.isFinite() && value == value.toLong().toDouble()) {
    value.toLong().toString()
  } else {
    value.toString()
  }

/** Formats a computed result the same way as [formatOperand], for the Result field. */
fun formatResult(value: Double): String = formatOperand(value)
