package com.google.ai.edge.gallery.customtasks.langcalc

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

/** A recognized function call: one operator and its two operands. */
data class CalcAction(val operator: CalcOperator, val x: Double, val y: Double) {
  /** Renders this call as an editable expression for the Calculator field, e.g. "2 * 20". */
  fun toExpression(): String = "${formatOperand(x)} ${operator.symbol} ${formatOperand(y)}"
}

/** Formats a Double without a trailing ".0" for whole numbers, e.g. 20.0 -> "20". */
fun formatOperand(value: Double): String =
  if (value.isFinite() && value == value.toLong().toDouble()) {
    value.toLong().toString()
  } else {
    value.toString()
  }

/** Formats a computed result the same way as [formatOperand], for the Result field. */
fun formatResult(value: Double): String = formatOperand(value)
