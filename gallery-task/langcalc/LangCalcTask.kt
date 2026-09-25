package com.google.ai.edge.gallery.customtasks.langcalc

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.Accelerator
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.createLlmChatConfigs
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Voice-driven calculator.
 *
 * The model's only job is to turn a spoken or typed request like "what's 2 times 20" into a
 * function call such as `multiply(2, 20)`. The app renders that as an editable "Calculator"
 * expression ("2 * 20") and computes the "Result" (40) itself -- the model's own natural-language
 * reply is never shown to the user. See LangCalcTools.kt for the tool definitions and
 * LangCalcViewModel.kt for how a call becomes a chat turn.
 *
 * Models come from two places (see `task` below):
 * - [customCheckpointModel], your own manually-pushed FunctionGemma checkpoint -- always
 *   available, no allowlist needed.
 * - `modelNames`, which asks the app's model allowlist system to attach whichever of Gemma 4 E2B,
 *   the Mobile Actions FunctionGemma fine-tune, and the Tiny Garden FunctionGemma fine-tune it
 *   already knows about -- reusing their existing download if AI Chat / Agent Skills / Mobile
 *   Actions / Tiny Garden already pulled them, no separate download or path-guessing required.
 *   See README.md's "Model allowlist" section for how this works and how to test it locally.
 *
 * Gemma 4 and FunctionGemma use different chat formats 
 * (system-vs-developer role, different tool call tokens). The LiteRT-LM Kotlin API abstracts that
 * away: LangCalcTools.kt's `@Tool` functions and the `ConversationConfig`/`tool()` plumbing are
 * unchanged for either model family. The one thing that *is* model-specific is the system prompt
 * text itself -- FunctionGemma needs its literal "Essential System Prompt" to activate tool
 * calling, while Gemma 4 just wants an ordinary instruction -- so that's chosen per-model in
 * [systemPromptFor] below.
 *
 * This follows the same plugin pattern as the other custom tasks under customtasks/ -- see
 * Function_Calling_Guide.md and customtasks/examplecustomtask for the general recipe.
 */
class LangCalcTask @Inject constructor(@ApplicationContext private val context: Context) :
  CustomTask {

  // Populated by LangCalcTools.onFunctionCalled while a tool-calling turn runs. Cleared before
  // each new prompt and read once inference for that prompt completes -- same pattern as
  // MobileActionsTask's `curActions`.
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
      models = mutableListOf(customCheckpointModel()),
      // Matched against the model allowlist by exact `name` -- see ModelManagerViewModel
      // .loadModelAllowlist()'s "Find models from allowlist if a task's `modelNames` field is
      // not empty" step. These three names are exactly what they're called in the production
      // allowlist as of 1_0_18.json. VOICE_CALC_TEST_MODEL_NAME is an extra slot for a
      // not-yet-published custom fine-tune -- see README.md.
      modelNames =
        listOf(
          "Gemma-4-E2B-it",
          "MobileActions-270M",
          "TinyGarden-270M",
          VOICE_CALC_TEST_MODEL_NAME,
        ),
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
 * The name to give your own not-yet-published fine-tune in a local test allowlist entry (see
 * README.md). Change this to whatever you actually name that entry, or leave it -- an unmatched
 * name is just logged as a warning and otherwise harmless.
 */
const val VOICE_CALC_TEST_MODEL_NAME = "LangCalc-270M"

/**
 * The system prompt to use, chosen per-model.
 *
 * FunctionGemma checkpoints require the literal "Essential System Prompt" 
 * from the fine-tuning notebook to activate function-calling mode.
 * Gemma 4 doesn't need a magic string -- an ordinary instruction is enough.
 * Matched by name prefix rather than one exact string so this also covers Gemma-4-E4B-it if you
 * add that to `modelNames` later.
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

/**
 * Your own FunctionGemma checkpoint, set up for a local file so you can start testing immediately
 * with `adb push` -- no hosting or allowlist required. This one's always in `task.models`
 * regardless of what the allowlist does or doesn't have.
 *
 * Push a `.litertlm` FunctionGemma checkpoint to `{ext_files_dir}/langcalc/model.litertlm`,
 * e.g.:
 * ```
 * adb push functiongemma-270m-it.litertlm \
 *   /sdcard/Android/data/com.google.ai.edge.gallery/files/langcalc/model.litertlm
 * ```
 * Once you've published a fine-tune to Hugging Face, you can retire this in favor of a proper
 * allowlist entry (matched via `VOICE_CALC_TEST_MODEL_NAME` in `modelNames` above) -- see
 * README.md.
 */
private fun customCheckpointModel() =
  Model(
    name = "Custom FunctionGemma checkpoint",
    info =
      "Expects a .litertlm FunctionGemma checkpoint manually pushed to " +
        "`{ext_files_dir}/langcalc/model.litertlm`. See the comment above " +
        "`customCheckpointModel()` in LangCalcTask.kt.",
    localFileRelativeDirPathOverride = "langcalc/",
    downloadFileName = "model.litertlm",
    // Without an explicit `configs`, LlmChatModelHelper.initialize() falls back to GPU
    // (Accelerator.GPU.label) as the default accelerator. The small FunctionGemma .litertlm
    // checkpoints (e.g. the litert-community ones) are built for CPU/XNNPACK, and loading one
    // on the GPU delegate throws "NOT_FOUND_ERROR: Input tensor not found" at engine creation.
    // CPU is listed first here so it's the default; GPU is left as a selectable option in case
    // your checkpoint does support it.
    configs = createLlmChatConfigs(accelerators = listOf(Accelerator.CPU, Accelerator.GPU)),
  )
