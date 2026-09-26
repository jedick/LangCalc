package com.jedick.langcalc.engine

/**
 * A small, dependency-free evaluator for the arithmetic expressions typed, spoken, or edited into
 * the "Calculator" field -- e.g. "2 * 20", "3 + 4 * 2", "(1 + 2) / 3", "-5 + 2".
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

  private class Parser(private val input: String) {
    private var pos = 0

    // expression := term (('+' | '-') term)*
    fun parseExpression(): Double {
      var value = parseTerm()
      while (true) {
        skipWhitespace()
        when (peek()) {
          '+' -> {
            pos++
            value += parseTerm()
          }
          '-' -> {
            pos++
            value -= parseTerm()
          }
          else -> return value
        }
      }
    }

    // term := factor (('*' | '/') factor)*
    private fun parseTerm(): Double {
      var value = parseFactor()
      while (true) {
        skipWhitespace()
        when (peek()) {
          '*' -> {
            pos++
            value *= parseFactor()
          }
          '/' -> {
            pos++
            val divisor = parseFactor()
            if (divisor == 0.0) throw EvalException(EvalErrorType.DIVIDE_BY_ZERO)
            value /= divisor
          }
          else -> return value
        }
      }
    }

    // factor := ('-' | '+') factor | '(' expression ')' | number
    private fun parseFactor(): Double {
      skipWhitespace()
      return when (peek()) {
        '-' -> {
          pos++
          -parseFactor()
        }
        '+' -> {
          pos++
          parseFactor()
        }
        '(' -> {
          pos++
          val value = parseExpression()
          skipWhitespace()
          if (peek() != ')') throw EvalException(EvalErrorType.SYNTAX)
          pos++
          value
        }
        else -> parseNumber()
      }
    }

    private fun parseNumber(): Double {
      skipWhitespace()
      val start = pos
      while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
      if (pos == start) throw EvalException(EvalErrorType.SYNTAX)
      return input.substring(start, pos).toDoubleOrNull() ?: throw EvalException(EvalErrorType.SYNTAX)
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
