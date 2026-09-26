package com.jedick.langcalc.engine

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

private const val TAG = "LangCalcEngine"

/**
 * The exact system prompt FunctionGemma was fine-tuned against. It has to be sent verbatim to
 * activate function-calling behavior; see the LangCalc training notebook in the main repo.
 */
private const val SYSTEM_PROMPT = "You are a model that can do function calling with the following functions"

/** Sampling and output-length defaults, matching this checkpoint's entry in model_allowlist_test.json. */
private const val TOP_K = 64
private const val TOP_P = 0.95
private const val TEMPERATURE = 0.0
private const val MAX_OUTPUT_TOKENS = 1024

/** What happened when a user request was sent to the model. */
sealed interface SendResult {
  data class Resolved(val action: CalcAction) : SendResult
  data object NoFunction : SendResult
  data class Error(val message: String) : SendResult
}

/**
 * Owns the LiteRT-LM [Engine] and its current [Conversation] for the lifetime of the process.
 *
 * Mirrors the structure of the original Gallery task (LangCalcTask.kt + LangCalcViewModel.kt) but
 * without the Gallery's model-manager/Hilt scaffolding: this app only ever has one model, so there
 * is nothing to select between.
 *
 * Not thread-safe beyond what a single caller using it sequentially needs -- all public functions
 * are suspend functions meant to be called from one coroutine at a time (see ChatState.kt).
 */
class LangCalcEngine {
  // Filled by LangCalcTools.onFunctionCalled while a tool-calling turn runs. Cleared before every
  // prompt and read once inference for that prompt completes.
  private val curActions = mutableListOf<CalcAction>()

  private var engine: Engine? = null
  private var conversation: Conversation? = null

  val isReady: Boolean
    get() = conversation != null

  /** Loads [modelPath] into a fresh [Engine]. Call from a coroutine; this can take several seconds. */
  suspend fun initialize(modelPath: String): Result<Unit> =
    withContext(Dispatchers.Default) {
      try {
        close()
        val newEngine =
          Engine(
            EngineConfig(
              modelPath = modelPath,
              backend = Backend.CPU(), // This checkpoint's allowlist entry lists "cpu" only.
            )
          )
        newEngine.initialize()
        engine = newEngine
        conversation = newEngine.createConversation(newConversationConfig())
        Result.success(Unit)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize engine", e)
        close()
        Result.failure(e)
      }
    }

  /**
   * Sends [userText] and returns the recognized [CalcAction], or [SendResult.NoFunction] /
   * [SendResult.Error]. Always starts the next turn with a clean context, since each calculation
   * is independent (same approach as the Gallery task).
   */
  suspend fun send(userText: String): SendResult =
    withContext(Dispatchers.Default) {
      val currentConversation = conversation ?: return@withContext SendResult.Error("Model not loaded")
      curActions.clear()
      var failure: Throwable? = null

      currentConversation
        .sendMessageAsync(userText)
        .catch { e -> failure = e }
        // The model's own natural-language follow-up streams through here too, once the tool call
        // above has been auto-executed, but the calculator only cares about the CalcAction
        // captured in curActions, so the streamed text is intentionally never read.
        .collect {}

      // Fresh context for the next turn.
      resetConversation()

      val error = failure
      when {
        error != null -> {
          Log.e(TAG, "Inference failed", error)
          SendResult.Error(error.message ?: "Unknown error")
        }
        curActions.isNotEmpty() -> SendResult.Resolved(curActions.first())
        else -> SendResult.NoFunction
      }
    }

  private fun resetConversation() {
    val currentEngine = engine ?: return
    conversation?.close()
    conversation = currentEngine.createConversation(newConversationConfig())
  }

  private fun newConversationConfig(): ConversationConfig =
    ConversationConfig(
      systemInstruction = Contents.of(SYSTEM_PROMPT),
      samplerConfig = SamplerConfig(topK = TOP_K, topP = TOP_P, temperature = TEMPERATURE),
      maxOutputToken = MAX_OUTPUT_TOKENS,
      tools = listOf(tool(LangCalcTools(onFunctionCalled = { curActions.add(it) }))),
    )

  /** Releases native resources. Safe to call multiple times. */
  fun close() {
    conversation?.close()
    conversation = null
    engine?.close()
    engine = null
  }
}
