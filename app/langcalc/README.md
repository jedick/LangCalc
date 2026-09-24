# LangCalc — a Google AI Edge Gallery custom task

This is a voice calculator plugin for the [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery)
Android app, using FunctionGemma and the [LiteRT-LM Kotlin API](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md).
It also supports Gemma 4 as an alternative to FunctionGemma.

## What it does

A chat-style screen where every turn shows three fields:

```
User:       What is 2 times 20?
Calculator: 2 * 20        <- editable, tap to fix or type your own
Result:     40            <- computed locally by parsing Calculator
```

The model's only job is turning the request into a function call (`multiply(2, 20)`).
The app turns that into the Calculator expression and evaluates it itself —
the model's own natural-language reply is never shown.
Voice input reuses the Gallery's existing on-device speech recognition.

## Setting it up

1. **Cloud requirements:**
   Ensure the fine-tuned model is available on the HF Hub in LiteRT-LM format
   (see `model/scripts/convert_to_litertlm.py` in the LangCalc repo).
2. **Local development environment:**
   You can probably get all this (and more) by installing Android Studio, but I prefer more control over what's on my system.
   - Download and extract the Android command line tools from <https://developer.android.com/studio/index.html>
     (look for "Command line tools only").
     ```
     cd ~/.android
     unzip commandlinetools-linux-15859902_latest.zip
     ```
   - Follow the instructions for moving files into <sdk>/cmdline-tools/latest/ (see <https://developer.android.com/tools/sdkmanager>).
     Then, download the android SDK with `sdkmanager`:
     ```
     cd ~/.android/cmdline-tools/latest/bin
     ./sdkmanager "platforms;android-36"
     ```
   - Install the `adb` tool from your OS package manager (it's `android-tools` on Slackware).
3. Grab the [1.0.18 release of the Google AI Edge Gallery](https://github.com/google-ai-edge/gallery/releases/tag/1.0.18)
   and copy this whole `langcalc/` folder to:
   ```
   gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/langcalc/
   ```
4. Start wireless debugging on your device: Settings - Developer options - Wireless debugging.
   Connect to your device with `adb` (note: the port numbers may be different in each step):
   ```  
   adb pair HOST[:PORT] [PAIRING CODE]
   adb connect HOST[:PORT]
   ```
5. Build and install (this takes a while - about 8 minutes on my laptop).
   ```
   cd gallery/Android/src/
   export ANDROID_HOME=~/.android
   ./gradlew installDebug
   ```
6. **Important:** Push the custom allowlist so the LangCalc task can download the fine-tuned model from Hugging Face:
   ```
   adb push model_allowlist_test.json /data/local/tmp/model_allowlist_test.json
   ```
7. Open "Voice Calculator" from the home screen (LLM category), pick a model, and ask it something.

## Files

- `CalcAction.kt` — the four operators, and turning a recognized call into an expression string.
- `CalcExpressionEvaluator.kt` — small dependency-free arithmetic parser (+ − × ÷, parens,
  decimals) used both to resolve a model call and to re-evaluate hand edits.
- `CalcTurn.kt` — one chat turn's state (User text + Calculator expression + Result + status).
- `LangCalcTools.kt` — the `ToolSet` the model calls into (model-agnostic).
- `LangCalcTask.kt` — the `CustomTask` plugin entry point: task metadata, the custom-checkpoint
  model entry, `modelNames` for allowlist-sourced models, per-model system prompt selection, and
  model init/cleanup.
- `LangCalcViewModel.kt` — drives the chat state; calls the model and resolves each turn.
- `LangCalcScreen.kt` — the chat UI: message list, editable Calculator/Result bubbles, voice/text
  input bar.
- `CalculatorKeypad.kt` — a small plain-Compose numeric keypad shown while editing a Calculator
  field (stock Jetpack Compose buttons — no extra library needed).
- `LangCalcModule.kt` — Hilt binding that makes the task show up on the home screen.
- `model_allowlist_test.json` — override file that extends the pre-packaged list
   (see <https://github.com/google-ai-edge/gallery/tree/main/model_allowlists>)
   with the fine-tuned FunctionGemma for LangCalc.
