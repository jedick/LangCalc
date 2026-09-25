package com.google.ai.edge.gallery.customtasks.langcalc

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Entry point for the LangCalc voice calculator, a custom task for the Google AI Edge Gallery.
 *
 * The model only translates a spoken or typed request (e.g. "what's 2 times 20") into a function
 * call such as `multiply(2, 20)`. The app shows the call as an editable Calculator expression
 * ("2 * 20") and computes the Result (40) itself. The model's text reply is never displayed.
 *
 * This class defines the task metadata, selects the models, and initializes and cleans up the
 * model. Tool definitions are in LangCalcTools.kt, and LangCalcViewModel.kt turns each tool call
 * into a chat turn.
 *
 * Models are not defined here. `modelNames` selects entries from the model allowlist by name
 * (see README.md for the allowlist setup).
 *
 * See Function_Calling_Guide.md and customtasks/examplecustomtask for the general plugin pattern.
 */
class LangCalcTask @Inject constructor(@ApplicationContext private val context: Context) :
  CustomTask {

  // Filled by LangCalcTools.onFunctionCalled while a tool-calling turn runs. Cleared before each
  // new prompt and read once inference for that prompt completes (same pattern as
  // MobileActionsTask's `curActions`).
  val curActions = mutableStateListOf<CalcAction>()

  private val tools = listOf(tool(LangCalcTools(onFunctionCalled = { curActions.add(it) })))

  override val task =
    Task(
      id = "llm_langcalc",
      label = "Voice Calculator",
      category = Category.LLM,
      icon = Icons.Outlined.Calculate,
      description =
        "Ask for a calculation by voice or text. The model turns your request into a function " +
          "call, which fills in an editable Calculator expression and its Result.",
      shortDescription = "Talk to a calculator",
      docUrl =
        "https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md",
      sourceCodeUrl =
        "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/langcalc",
      // Empty on purpose: the allowlist supplies the models (see `modelNames`).
      models = mutableListOf(),
      // Matched by exact `name` against the model allowlist.
      // LangCalc-270M isn't listed here because it attaches through its taskTypes entry in
      // model_allowlist_test.json (separately pushed to the device, see README.md).
      modelNames = listOf("Gemma-4-E2B-it"),
      experimental = true,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    curActions.clear()
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      taskId = task.id,
      supportImage = false,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = systemPromptFor(model),
      tools = tools,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    curActions.clear()
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    LangCalcScreen(
      task = task,
      modelManagerViewModel = customTaskData.modelManagerViewModel,
      bottomPadding = customTaskData.bottomPadding,
      setAppBarControlsDisabled = customTaskData.setAppBarControlsDisabled,
      curActions = curActions,
      tools = tools,
    )
  }
}

/**
 * Returns the system prompt for the given model.
 *
 * Gemma 4 and FunctionGemma use different chat formats (system vs. developer role, different tool
 * call tokens). The LiteRT-LM Kotlin API handles that difference, so the `@Tool` functions in
 * LangCalcTools.kt and the `tool()` setup above work unchanged for both. Only the system prompt
 * text differs. FunctionGemma needs its literal "Essential System Prompt" from the fine-tuning
 * notebook to activate function calling. Gemma 4 accepts an ordinary instruction.
 *
 * The match is on the name prefix, so other Gemma 4 variants (e.g. Gemma-4-E4B-it) also work if
 * added to `modelNames`.
 */
fun systemPromptFor(model: Model): Contents =
  if (model.name.startsWith("Gemma-4")) getGemma4SystemPrompt() else getFunctionGemmaSystemPrompt()

/** The essential FunctionGemma system prompt. */
fun getFunctionGemmaSystemPrompt(): Contents =
  Contents.of(
    listOf(Content.Text("You are a model that can do function calling with the following functions"))
  )

/** Gemma 4's system prompt. */
fun getGemma4SystemPrompt(): Contents =
  Contents.of(
    listOf(Content.Text("You are a model that can do function calling for a calculator app."))
  )
