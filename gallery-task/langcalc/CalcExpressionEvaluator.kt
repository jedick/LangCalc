package com.google.ai.edge.gallery.customtasks.langcalc

/**
 * A small, dependency-free evaluator for the arithmetic expressions typed, spoken, or edited into
 * the "Calculator" field -- e.g. "2 * 20", "3 + 4 * 2", "(1 + 2) / 3", "-5 + 2".
 */
object CalcExpressionEvaluator {

  /** Evaluates [expression] and returns the numeric result, or a failure with a readable message. */
  fun evaluate(expression: String): Result<Double> =
    try {
      val parser = Parser(expression)
      val value = parser.parseExpression()
      parser.expectEnd()
      Result.success(value)
    } catch (e: EvalException) {
      Result.failure(e)
    }

  class EvalException(message: String) : Exception(message)

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
            if (divisor == 0.0) throw EvalException("Can't divide by zero")
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
          if (peek() != ')') throw EvalException("Missing closing parenthesis")
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
      if (pos == start) {
        val c = peek()
        throw EvalException(if (c == null) "Expected a number" else "Unexpected character '$c'")
      }
      return input.substring(start, pos).toDoubleOrNull()
        ?: throw EvalException("Invalid number '${input.substring(start, pos)}'")
    }

    fun expectEnd() {
      skipWhitespace()
      if (pos != input.length) throw EvalException("Unexpected character '${peek()}'")
    }

    private fun skipWhitespace() {
      while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char? = if (pos < input.length) input[pos] else null
  }
}
