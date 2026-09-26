package com.jedick.langcalc.engine

import android.util.Log
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

private const val TAG = "LangCalcTools"

/**
 * The four arithmetic tools FunctionGemma can call.
 *
 * Each function does two things when called:
 * 1. Reports the recognized operation back to the app via [onFunctionCalled]. This is what
 *    actually drives the UI -- see LangCalcEngine.kt.
 * 2. Computes and returns the real result, so the tool response fed back to the model for its
 *    (unused) follow-up turn is still accurate.
 *
 * Tool calling is left on automatic (the default for `ConversationConfig`), so LiteRT-LM invokes
 * these functions directly. The model's follow-up natural-language reply is never read or shown.
 *
 * Tool descriptions are intentionally in English regardless of the app's interface language: they
 * are prompt text for the (bilingual) model, not UI copy, and FunctionGemma was fine-tuned against
 * these exact strings.
 */
class LangCalcTools(val onFunctionCalled: (CalcAction) -> Unit) : ToolSet {
  @Tool(description = "Adds two numbers together (sum, total, plus).")
  fun add(
    @ToolParam(description = "The first number.") x: Double,
    @ToolParam(description = "The second number.") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "add($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.ADD, x, y))
    return mapOf("result" to x + y)
  }

  @Tool(description = "Subtracts one number from another (difference, minus).")
  fun subtract(
    @ToolParam(description = "The starting number.") x: Double,
    @ToolParam(description = "The number to subtract from x.") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "subtract($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.SUBTRACT, x, y))
    return mapOf("result" to x - y)
  }

  @Tool(description = "Multiplies two numbers together (product, times).")
  fun multiply(
    @ToolParam(description = "The first number.") x: Double,
    @ToolParam(description = "The second number.") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "multiply($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.MULTIPLY, x, y))
    return mapOf("result" to x * y)
  }

  @Tool(description = "Divides one number by another (quotient, over).")
  fun divide(
    @ToolParam(description = "The numerator.") x: Double,
    @ToolParam(description = "The denominator.") y: Double,
  ): Map<String, Double> {
    Log.d(TAG, "divide($x, $y)")
    onFunctionCalled(CalcAction(CalcOperator.DIVIDE, x, y))
    return mapOf("result" to if (y != 0.0) x / y else Double.NaN)
  }
}
