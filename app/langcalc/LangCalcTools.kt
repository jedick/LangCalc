package com.google.ai.edge.gallery.customtasks.langcalc

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
 *    actually drives the UI -- see LangCalcTask.kt and LangCalcViewModel.kt.
 * 2. Computes and returns the real result, so the tool response fed back to the model for its
 *    (unused) follow-up turn is still accurate.
 *
 * Tool calling is left on automatic (the default for `ConversationConfig`), so LiteRT-LM invokes
 * these functions directly -- there's no separate manual dispatch step to write. The model's
 * follow-up natural-language reply is simply never read or shown; see the comment in
 * LangCalcViewModel.send() for where that's ignored. If you'd rather skip generating that
 * follow-up turn entirely (a small latency win), see "Manual Tool Calling" in the LiteRT-LM
 * Kotlin API guide and set `automaticToolCalling = false` on the `ConversationConfig` built in
 * LangCalcTask.kt.
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
