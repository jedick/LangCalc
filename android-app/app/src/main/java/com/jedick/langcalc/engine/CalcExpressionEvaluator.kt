package com.jedick.langcalc.engine

/**
 * A small, dependency-free evaluator for the arithmetic expressions typed, spoken, or edited into
 * the "Calculator" field -- e.g. "2 * 20", "3 + 4 * 2", "(1 + 2) / 3", "-5 + 2", "50 + 30%".
 *
 * Errors are reported as an [EvalErrorType] rather than a hardcoded message string so the UI layer
 * can show a localized message (see CalcTurn.kt).
 */
object CalcExpressionEvaluator {

  /** Evaluates [expression] and returns the numeric result, or a failure with a typed error. */
  fun evaluate(expression: String): Result<Double> =
    try {
      val parser = Parser(expression)
      val value = parser.parseExpression()
      parser.expectEnd()
      Result.success(value)
    } catch (e: EvalException) {
      Result.failure(e)
    }

  enum class EvalErrorType {
    DIVIDE_BY_ZERO,
    SYNTAX,
  }

  class EvalException(val type: EvalErrorType) : Exception()

  /**
   * A parsed operand. [value] is the number as written; for a percent literal such as "30%" it is
   * 30 and [percent] is true. Use [fraction] where a percent literal is just a number (0.3).
   */
  private class Operand(val value: Double, val percent: Boolean = false) {
    val fraction: Double
      get() = if (percent) value / 100 else value
  }

  /**
   * Percent literals follow the same rules as the percent functions in tools.py:
   *   x + y%  ->  x * (1 + y / 100)   (add_percent)
   *   x - y%  ->  x * (1 - y / 100)   (subtract_percent)
   *   x * y%  ->  x * y / 100         (percent_of)
   * where y% must be the whole right-hand term, so "50 + 30% * 2" adds 60 (30% * 2 = 0.6), not 30%
   * of 50. In any other position (including "x / y%"), y% means y / 100.
   */
  private class Parser(private val input: String) {
    private var pos = 0

    // expression := term (('+' | '-') term)*
    fun parseExpression(): Double {
      var value = parseTerm().fraction
      while (true) {
        skipWhitespace()
        when (peek()) {
          '+' -> {
            pos++
            val term = parseTerm()
            value = if (term.percent) value * (1 + term.value / 100) else value + term.value
          }
          '-' -> {
            pos++
            val term = parseTerm()
            value = if (term.percent) value * (1 - term.value / 100) else value - term.value
          }
          else -> return value
        }
      }
    }

    // term := factor (('*' | '/') factor)*
    // A term that is a single percent literal stays a percent Operand, so the caller can tell.
    private fun parseTerm(): Operand {
      var term = parseFactor()
      while (true) {
        skipWhitespace()
        when (peek()) {
          '*' -> {
            pos++
            val factor = parseFactor()
            term =
              Operand(if (factor.percent) term.fraction * factor.value / 100 else term.fraction * factor.value)
          }
          '/' -> {
            pos++
            val divisor = parseFactor().fraction
            if (divisor == 0.0) throw EvalException(EvalErrorType.DIVIDE_BY_ZERO)
            term = Operand(term.fraction / divisor)
          }
          else -> return term
        }
      }
    }

    // factor := ('-' | '+') factor | '(' expression ')' | number ['%']
    private fun parseFactor(): Operand {
      skipWhitespace()
      return when (peek()) {
        '-' -> {
          pos++
          Operand(-parseFactor().fraction)
        }
        '+' -> {
          pos++
          Operand(parseFactor().fraction)
        }
        '(' -> {
          pos++
          val value = parseExpression()
          skipWhitespace()
          if (peek() != ')') throw EvalException(EvalErrorType.SYNTAX)
          pos++
          Operand(value)
        }
        else -> parseNumber()
      }
    }

    private fun parseNumber(): Operand {
      skipWhitespace()
      val start = pos
      while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
      if (pos == start) throw EvalException(EvalErrorType.SYNTAX)
      val value = input.substring(start, pos).toDoubleOrNull() ?: throw EvalException(EvalErrorType.SYNTAX)
      // The % must directly follow the digits, as the % key writes it.
      if (peek() == '%') {
        pos++
        return Operand(value, percent = true)
      }
      return Operand(value)
    }

    fun expectEnd() {
      skipWhitespace()
      if (pos != input.length) throw EvalException(EvalErrorType.SYNTAX)
    }

    private fun skipWhitespace() {
      while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char? = if (pos < input.length) input[pos] else null
  }
}
