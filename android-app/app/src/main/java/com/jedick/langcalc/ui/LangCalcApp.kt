package com.jedick.langcalc.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jedick.langcalc.data.AppSettings
import com.jedick.langcalc.data.ModelRepository
import com.jedick.langcalc.engine.LangCalcEngine
import com.jedick.langcalc.ui.chat.ChatScreen
import com.jedick.langcalc.ui.chat.ChatState
import com.jedick.langcalc.ui.intro.IntroScreen
import com.jedick.langcalc.ui.intro.IntroState
import com.jedick.langcalc.ui.settings.SettingsScreen

/** Which top-level screen is showing. Settings remembers where to return to on back. */
private sealed interface AppScreen {
  data object Intro : AppScreen
  data object Chat : AppScreen
  data class Settings(val previous: AppScreen) : AppScreen
}

/**
 * Root composable: gates access to [ChatScreen] behind the model being downloaded and loaded (see
 * [IntroScreen]), and hosts [SettingsScreen] as an overlay reachable from either one.
 *
 * Navigation state is plain `remember`, not `rememberSaveable`: this app relies on the Activity
 * surviving configuration changes (see android:configChanges in the manifest) rather than restoring
 * across process death, which keeps this simple for a first version. See the project README for
 * what a more robust version would add.
 */
@Composable
fun LangCalcApp(
  settings: AppSettings,
  modelRepository: ModelRepository,
  engine: LangCalcEngine,
  onLanguageChanged: () -> Unit,
) {
  var screenState by remember { mutableStateOf<AppScreen>(if (engine.isReady) AppScreen.Chat else AppScreen.Intro) }

  val introState = remember { IntroState(modelRepository, engine) }
  val chatState = remember { ChatState(engine) }

  // If the model was already loaded before this composable ran (e.g. we're back here after a
  // language-change recreate(), which only rebuilds the UI, not the Application-scoped engine),
  // skip straight to Chat instead of re-running the Intro/download flow.
  LaunchedEffect(Unit) {
    if (engine.isReady) screenState = AppScreen.Chat
  }

  when (val current = screenState) {
    is AppScreen.Intro ->
      IntroScreen(
        state = introState,
        onSettingsClick = { screenState = AppScreen.Settings(AppScreen.Intro) },
        onReady = { screenState = AppScreen.Chat },
      )
    is AppScreen.Chat ->
      ChatScreen(
        state = chatState,
        settings = settings,
        onSettingsClick = { screenState = AppScreen.Settings(AppScreen.Chat) },
      )
    is AppScreen.Settings ->
      SettingsScreen(
        settings = settings,
        modelRepository = modelRepository,
        onBack = { screenState = current.previous },
        onLanguageChanged = onLanguageChanged,
        onModelDeleted = {
          engine.close()
          screenState = AppScreen.Intro
        },
      )
  }
}
