# LangCalc: a standalone Android app

LangCalc is a voice-driven calculator that runs on-device.
This is a standalone Android app, built with Kotlin and Jetpack Compose, that runs a small
fine-tuned FunctionGemma model through the
[LiteRT-LM Kotlin API](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md).
The fine-tuned model is published on the Hugging Face Hub as
[jedick/functiongemma-langcalc-en.zh](https://huggingface.co/jedick/functiongemma-langcalc-en.zh)
and handles both English and Chinese requests.

This is the standalone counterpart to the
[Google AI Edge Gallery custom task](../gallery-task/README.md) version of LangCalc. Unlike the
Gallery task, this app only supports the fine-tuned FunctionGemma model (no Gemma 4 option), and
doesn't depend on the Gallery app being installed at all.

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

On first launch, the app shows an intro screen with a button to download the model
(about 285 MB, fetched directly from the Hugging Face Hub). Once it's downloaded and loaded, the
app opens straight into the chat screen. A settings screen (gear icon, top right) lets you:

- switch the app's interface language between English and Chinese (this doesn't affect the model,
  which understands both either way)
- set the language used for voice input, separately from the interface language, since Android's
  on-device speech recognizer doesn't reliably follow the device's own language settings
- check for a newer version of the model, or delete it to free up space without uninstalling the
  app

## Setting it up

These steps build the app and install it on an Android device, using the command-line tools
instead of Android Studio. If you already use Android Studio, you can skip step 1 and just open
the `android-app/` folder as a project; it manages the SDK and Gradle wrapper for you.

1. **Set up the development environment.**
   - Install a JDK (17 or newer) and `adb` from your OS package manager (the `adb` package is
     `android-tools` on Slackware).
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
2. **Get the source.**
   Clone this repository, or just this subfolder if you're only interested in the app:
   ```
   git clone https://github.com/jedick/LangCalc
   cd LangCalc/android-app
   ```
3. **Connect your device.**
   Turn on wireless debugging: Settings - Developer options - Wireless debugging.
   Then connect with `adb` (the port numbers differ between the two commands):
   ```
   adb pair HOST[:PORT] [PAIRING CODE]
   adb connect HOST[:PORT]
   ```
4. **Build the APK.**
   ```
   export ANDROID_HOME=~/.android
   ./gradlew assembleDebug
   ```
   This produces `app/build/outputs/apk/debug/app-debug.apk`, which you can keep, hand to someone
   else, or install on more than one device.
5. **Install it.**
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
6. **Try it.**
   Open the LangCalc app, tap "Download model" on the intro screen, and wait for the ~285 MB
   download and load to finish. Then ask it something, by voice or by typing.

## Files

- `gradlew`, `gradlew.bat`, `gradle/wrapper/`: the Gradle wrapper. Lets anyone building this project
  use the exact Gradle version it was written against, without installing Gradle separately. Only
  needs regenerating if you want to move to a newer Gradle version, with
  `./gradlew wrapper --gradle-version <version>`.
- `LangCalcApplication.kt`: holds the app-lifetime singletons (settings, model repository, the
  inference engine)
- `MainActivity.kt`: the single Activity; applies the chosen interface language before any UI is
  created

`engine/` -- the calculator itself, independent of any UI:

- `LangCalcEngine.kt`: wraps the LiteRT-LM `Engine`/`Conversation` -- loads the model, sends a
  request, and resets the conversation between turns
- `LangCalcTools.kt`: the tools the model calls
- `CalcAction.kt`: the operators, and turning a recognized call into an expression string
- `CalcExpressionEvaluator.kt`: evaluates the expression (for model calls and hand edits)
- `CalcTurn.kt`: the state of one chat turn (User text, Calculator expression, Result, status)

`data/` -- settings, and getting the model onto the device:

- `AppSettings.kt`: the app's persisted settings (interface language, voice-input language, the
  downloaded model's version)
- `ModelRepository.kt`: downloads, deletes, and checks for updates to the model file, straight from
  the Hugging Face Hub
- `ModelConstants.kt`: the one model this app uses (repo, filename, size)
- `LocaleHelper.kt`: applies the chosen interface language to the app's resources

`speech/`:

- `SpeechInput.kt`: builds the system speech-recognition request, with an explicit language rather
  than relying on the device's default

`ui/`:

- `LangCalcApp.kt`: top-level navigation between the intro, chat, and settings screens
- `intro/`: the first-run screen (app info, download progress, model loading)
- `chat/`: the chat screen, including `CalculatorKeypad.kt` for editing the Calculator field
- `settings/`: the settings screen (language, voice-input language, model updates/deletion)
- `common/`: the shared top app bar (title, back button, settings gear)
- `theme/`: Material 3 color scheme
