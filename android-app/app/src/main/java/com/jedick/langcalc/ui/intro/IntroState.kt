package com.jedick.langcalc.ui.intro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jedick.langcalc.data.DownloadState
import com.jedick.langcalc.data.ModelRepository
import com.jedick.langcalc.engine.LangCalcEngine

sealed interface IntroPhase {
  /** Deciding whether the model file is already on disk. Brief; shown as a spinner. */
  data object Checking : IntroPhase
  data object NotDownloaded : IntroPhase
  data class Downloading(val percent: Int) : IntroPhase
  data object LoadingModel : IntroPhase
  data object Ready : IntroPhase
  data class DownloadError(val message: String) : IntroPhase
  data class LoadError(val message: String) : IntroPhase
}

/** What the intro screen shows about a model that is already on the device. */
data class DownloadedModelInfo(val sizeBytes: Long, val downloadedAtMillis: Long?)

/**
 * Holds the intro screen's state and the download-then-load sequence. A plain class rather than an
 * androidx ViewModel: it's `remember`-ed once in [com.jedick.langcalc.ui.LangCalcApp] and the
 * Activity survives configuration changes (see android:configChanges in the manifest), so there's
 * no rotation-survival need a ViewModel would otherwise justify.
 */
class IntroState(private val modelRepository: ModelRepository, private val engine: LangCalcEngine) {
  var phase by mutableStateOf<IntroPhase>(IntroPhase.Checking)
    private set

  /** Size and download date of the model on disk, or null if there is none. */
  fun downloadedModelInfo(): DownloadedModelInfo? =
    if (modelRepository.isDownloaded()) {
      DownloadedModelInfo(modelRepository.modelSizeBytes(), modelRepository.downloadedAtMillis())
    } else {
      null
    }

  suspend fun checkInitialState() {
    // Clears cache files from earlier versions of the app and partial downloads.
    modelRepository.removeLeftoverFiles()
    if (modelRepository.isDownloaded()) {
      loadModel()
    } else {
      phase = IntroPhase.NotDownloaded
    }
  }

  suspend fun downloadAndLoad() {
    phase = IntroPhase.Downloading(0)
    modelRepository.download().collect { state ->
      when (state) {
        is DownloadState.Progress -> phase = IntroPhase.Downloading(state.percent)
        is DownloadState.Success -> loadModel()
        is DownloadState.Error -> phase = IntroPhase.DownloadError(state.message)
      }
    }
  }

  suspend fun loadModel() {
    phase = IntroPhase.LoadingModel
    engine
      .initialize(modelRepository.modelFile.absolutePath)
      .fold(
        onSuccess = { phase = IntroPhase.Ready },
        onFailure = { phase = IntroPhase.LoadError(it.message ?: "Unknown error") },
      )
  }
}
