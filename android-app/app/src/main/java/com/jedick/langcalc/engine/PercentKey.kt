package com.jedick.langcalc.engine

/**
 * Logic for the keypad's % key, which toggles between a plain operation and its percent form on
 * the last operand of the expression:
 *
 *   "50 + 30" <-> "50 + 30%"   (add <-> add_percent)
 *   "50 - 30" <-> "50 - 30%"   (subtract <-> subtract_percent)
 *   "50 * 30" <-> "50 * 30%"   (multiply <-> percent_of)
 *   "50 / 30"      (no percent form; the key is disabled)
 *
 * The key also works on the last operation of a longer expression, e.g. "3 * 4 + 5" -> "3 * 4 + 5%".
 */
object PercentKey {
  // The last operand with the operator in front of it and an optional % after it.
  private val LAST_OPERAND = Regex("""([+\-*/])\s*(\d+(?:\.\d*)?|\.\d+)(%?)\s*$""")

  private class Target(val operator: Char, val numberEnd: Int, val percentRange: IntRange?)

  /** Finds the operand the % key would act on, or null if there is no binary operation to act on. */
  private fun find(expression: String): Target? {
    val match = LAST_OPERAND.find(expression) ?: return null
    // Something must stand to the left of the operator, so "-5" and "5 - -3" are not operations.
    val left = expression.substring(0, match.range.first).trimEnd()
    if (left.isEmpty() || !(left.last().isDigit() || left.last() == ')' || left.last() == '%')) return null
    val percent = match.groups[3]!!
    return Target(
      operator = match.groupValues[1].single(),
      numberEnd = match.groups[2]!!.range.last + 1,
      percentRange = if (percent.value.isEmpty()) null else percent.range,
    )
  }

  /** Whether the % key should be enabled for [expression]. */
  fun canToggle(expression: String): Boolean {
    val target = find(expression) ?: return false
    // A "/" operation has no percent form, but an existing % can still be removed.
    return target.percentRange != null || target.operator != '/'
  }

  /** Adds or removes the % after the last operand. Returns [expression] unchanged if not [canToggle]. */
  fun toggle(expression: String): String {
    if (!canToggle(expression)) return expression
    val target = find(expression) ?: return expression
    return if (target.percentRange != null) {
      expression.removeRange(target.percentRange)
    } else {
      expression.substring(0, target.numberEnd) + "%" + expression.substring(target.numberEnd)
    }
  }
}
