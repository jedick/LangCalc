# LangCalc: a Google AI Edge Gallery custom task

LangCalc is a voice-driven calculator that runs on-device.
This folder holds its plugin for the [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery)
Android app, which runs a small fine-tuned FunctionGemma model through the
[LiteRT-LM Kotlin API](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md).
The fine-tuned model is published on the Hugging Face Hub as
[jedick/functiongemma-langcalc-en.zh](https://huggingface.co/jedick/functiongemma-langcalc-en.zh)
and handles both English and Chinese requests.
The plugin also supports Gemma 4 as an alternative to FunctionGemma.

For the model, training data, and evaluation results, see the
[main LangCalc repository](https://github.com/jedick/LangCalc).

## What it does

A chat-style screen where every turn shows three fields:

```
User:       What is 2 times 20?
Calculator: 2 * 20        <- editable, tap to fix or type your own
Result:     40            <- computed locally by parsing Calculator
```

The model's only job is turning the request into a function call (`multiply(2, 20)`).
The app turns that into the Calculator expression and evaluates it itself.
The model's own natural-language reply is never shown.
Voice input reuses the Gallery's existing on-device speech recognition.

## Setting it up

These steps build the Gallery app with the LangCalc task and install it on an Android device.
They were written for Linux and use the command-line tools instead of Android Studio.
If you already use Android Studio, you can skip step 1.

1. **Set up the development environment.**
   - Install a JDK (check the Gallery's Gradle files for the required version)
     and `adb` from your OS package manager (the `adb` package is `android-tools` on Slackware).
   - Download the Android command line tools from <https://developer.android.com/studio/index.html>
     (look for "Command line tools only") and extract them.
     The SDK manager expects them in `cmdline-tools/latest/`
     (see <https://developer.android.com/tools/sdkmanager>):
     ```
     export ANDROID_HOME=~/.android
     cd $ANDROID_HOME
     unzip commandlinetools-linux-15859902_latest.zip
     mkdir -p cmdline-tools/latest
     mv cmdline-tools/{bin,lib,NOTICE.txt,source.properties} cmdline-tools/latest/
     ```
   - Install the Android SDK platform:
     ```
     cd $ANDROID_HOME/cmdline-tools/latest/bin
     ./sdkmanager --sdk_root=$ANDROID_HOME "platforms;android-36"
     ```
2. **Get the Gallery source and add the task.**
   Download the [1.0.18 release of the Google AI Edge Gallery](https://github.com/google-ai-edge/gallery/releases/tag/1.0.18)
   (the plugin was written against this release and may need changes for later ones).
   Then copy the `gallery-task/langcalc/` folder from this repository to:
   ```
   gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/langcalc/
   ```
3. **Connect your device.**
   Turn on wireless debugging: Settings - Developer options - Wireless debugging.
   Then connect with `adb` (the port numbers differ between the two commands):
   ```
   adb pair HOST[:PORT] [PAIRING CODE]
   adb connect HOST[:PORT]
   ```
4. **Push the custom model allowlist.**
   The Gallery gets its model list from an allowlist, and LangCalc finds its models there by name.
   This repository's `model_allowlist_test.json` adds the fine-tuned model,
   so the task can download it from the Hugging Face Hub.
   Run this from the `gallery-task/` folder:
   ```
   adb push model_allowlist_test.json /data/local/tmp/model_allowlist_test.json
   ```
   **Note:** while this file is on the device, it *replaces* the Gallery's entire allowlist for
   every task, not just LangCalc.
   The file is a full copy of the 1.0.18 allowlist with the LangCalc entry added,
   so the other tasks keep working.
   To go back to the standard allowlist, run
   `adb shell rm /data/local/tmp/model_allowlist_test.json`
   and restart the app.
5. **Build and install** (this takes about 8 minutes on my laptop):
   ```
   cd gallery/Android/src/
   export ANDROID_HOME=~/.android
   ./gradlew installDebug
   ```
6. **Try it.**
   Open the Gallery app and select "Voice Calculator" from the home screen (LLM category).
   Pick a model, wait for the download to finish, and ask it something.
   The LangCalc model is the fine-tuned FunctionGemma (about 285 MB).
   The Gemma 4 E2B model is a general-purpose alternative
   (about 2.6 GB, and the allowlist lists 8 GB of device memory as its minimum).
   If you already downloaded Gemma 4 E2B for another task, the app reuses that file.

## Files

Files in `langcalc/` (copied into the Gallery source in step 2):

- `LangCalcTask.kt`: the entry point; holds task metadata, per-model system prompts, and model init/cleanup
- `LangCalcScreen.kt`: the chat UI
- `LangCalcViewModel.kt`: chat state; calls the model and resolves each turn
- `LangCalcTools.kt`: the tools the model calls
- `CalcAction.kt`: the operators, and turning a recognized call into an expression string
- `CalculatorKeypad.kt`: the keypad shown while editing
- `CalcExpressionEvaluator.kt`: evaluates the expression (for model calls and hand edits)
- `CalcTurn.kt`: the state of one chat turn (User text, Calculator expression, Result, status)
- `LangCalcModule.kt`: boilerplate Hilt binding that makes the task show up on the home screen

Other files in this folder:

- `model_allowlist_test.json`: override file that extends the pre-packaged
  [allowlist](https://github.com/google-ai-edge/gallery/tree/main/model_allowlists)
  with the fine-tuned FunctionGemma for LangCalc (pushed to the device in step 4).
