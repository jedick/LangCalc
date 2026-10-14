package com.jedick.langcalc.engine

import android.util.Log
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

private const val TAG = "LangCalcTools"

/** Default for decline(reason). Same as the default in tools.py of the Python prototype. */
private const val DEFAULT_DECLINE_REASON = "not given"

/**
 * The tools FunctionGemma can call: four arithmetic functions, three percent functions, and
 * decline() for requests that are not calculations.
 *
 * Each function does two things when called:
 * 1. Reports the recognized call back to the app via [onFunctionCalled]. This is what actually
 *    drives the UI -- see LangCalcEngine.kt.
 * 2. Computes and returns the real result, so the tool response fed back to the model for its
 *    (unused) follow-up turn is still accurate.
 *
 * Tool calling is left on automatic (the default for `ConversationConfig`), so LiteRT-LM invokes
 * these functions directly. The model's follow-up natural-language reply is never read or shown.
 *
 * Tool and parameter descriptions are intentionally in English regardless of the app's interface
 * language: they are prompt text for the (bilingual) model, not UI copy. They are copied from the
 * docstrings in tools.py of the Python prototype, which the training notebook also uses, and must
 * stay identical to them (including capitalization and punctuation). Keep the function order the
 * same as `tools` in tools.py too, since the order determines the order of the declarations in
 * the prompt.
 */
class LangCalcTools(val onFunctionCalled: (ModelAction) -> Unit) : ToolSet {
  @Tool(description = "Adds two numbers together (sum, total, plus).")
  fun add(
    @ToolParam(description = "the first number") x: Double,
    @ToolParam(description = "the second number") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "add($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.ADD, x, y))
    return mapOf("result" to x + y)
  }

  @Tool(description = "Subtracts one number from another (difference, minus).")
  fun subtract(
    @ToolParam(description = "the starting number") x: Double,
    @ToolParam(description = "the number to subtract from x") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "subtract($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.SUBTRACT, x, y))
    return mapOf("result" to x - y)
  }

  @Tool(description = "Multiplies two numbers together (product, times).")
  fun multiply(
    @ToolParam(description = "the first number") x: Double,
    @ToolParam(description = "the second number") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "multiply($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.MULTIPLY, x, y))
    return mapOf("result" to x * y)
  }

  @Tool(description = "Divides one number by another (quotient, over).")
  fun divide(
    @ToolParam(description = "the numerator") x: Double,
    @ToolParam(description = "the denominator") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "divide($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.DIVIDE, x, y))
    return mapOf("result" to if (y != 0.0) x / y else Double.NaN)
  }

  @Tool(description = "Increases a number by a percentage (markup, tax added, raise, increase, up by).")
  fun add_percent(
    @ToolParam(description = "the starting number") x: Double,
    @ToolParam(description = "the percent to add, as written (30 for 30%)") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "add_percent($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.ADD, x, y, percent = true))
    return mapOf("result" to x * (1 + y / 100))
  }

  @Tool(description = "Decreases a number by a percentage (discount, off, drop, reduction, down by).")
  fun subtract_percent(
    @ToolParam(description = "the starting number") x: Double,
    @ToolParam(description = "the percent to subtract, as written (30 for 30%)") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "subtract_percent($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.SUBTRACT, x, y, percent = true))
    return mapOf("result" to x * (1 - y / 100))
  }

  @Tool(description = "Calculates a percentage of a number (20% of 2300, the tax amount, the share).")
  fun percent_of(
    @ToolParam(description = "the number to take a percentage of") x: Double,
    @ToolParam(description = "the percent to take, as written (20 for 20%)") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "percent_of($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.MULTIPLY, x, y, percent = true))
    return mapOf("result" to x * y / 100)
  }

  // The description has a line break in the middle because the docstring in tools.py does, and
  // the prototype's prompt contains it. The parameter is optional (it has a default), so it is
  // left out of the declaration's "required" list, as in the prototype's prompt.
  @Tool(
    description =
      "Declines a request that is not an add, subtract, multiply, divide, or percent\n" +
        "calculation on two numbers (abstain, cannot answer, not a calculation)."
  )
  fun decline(
    @ToolParam(
      description =
        "a few words on why, in the language of the request (for example \"small talk\" or \"闲聊\")"
    )
    reason: String = DEFAULT_DECLINE_REASON
  ): String {
    Log.d(TAG, "decline($reason)")
    // The default means the model gave no reason. The UI then shows "Declined request" alone.
    val shown = reason.trim().takeIf { it.isNotEmpty() && it != DEFAULT_DECLINE_REASON }
    onFunctionCalled(DeclineAction(shown))
    return reason
  }
}
