package com.jedick.langcalc.ui.intro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jedick.langcalc.R
import com.jedick.langcalc.data.AppLinks
import com.jedick.langcalc.data.ModelConstants
import com.jedick.langcalc.ui.common.AppIcon
import com.jedick.langcalc.ui.common.AppTopBar
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private fun formatMegabytes(bytes: Long): String = "${bytes / (1024 * 1024)} MB"

/**
 * The intro screen has two uses:
 * - At startup (when [onBack] is null) it runs the check, download and load sequence for the
 *   model and calls [onReady] when the model is loaded.
 * - As the help page (when [onBack] is set) it shows the same text with the status of the model
 *   that is already loaded, under a title and a back button. It does not touch the model.
 */
@Composable
fun IntroScreen(state: IntroState, onSettingsClick: () -> Unit, onReady: () -> Unit, onBack: (() -> Unit)? = null) {
  val context = LocalContext.current
  val helpMode = onBack != null

  if (!helpMode) {
    LaunchedEffect(Unit) { state.checkInitialState() }
    LaunchedEffect(state.phase) {
      if (state.phase is IntroPhase.Ready) onReady()
    }
  }

  Scaffold(
    topBar = {
      if (helpMode) {
        AppTopBar(title = stringResource(R.string.intro_title), onBackClick = onBack)
      } else {
        AppTopBar(onSettingsClick = onSettingsClick)
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      AppIcon(size = 88.dp)
      Spacer(modifier = Modifier.height(20.dp))
      Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text(
        stringResource(R.string.intro_tagline),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        stringResource(R.string.intro_body),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      val modelInfo = if (helpMode) state.downloadedModelInfo() else null
      Text(
        stringResource(R.string.intro_model_name, formatMegabytes(modelInfo?.sizeBytes ?: ModelConstants.APPROX_SIZE_BYTES)),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      TextButton(
        onClick = {
          // Opens the user's browser; the app does not show web pages itself.
          try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PROJECT_URL)))
          } catch (e: ActivityNotFoundException) {
            // No browser installed. Nothing else to do.
          }
        }
      ) {
        Text(stringResource(R.string.intro_project_link))
      }
      Spacer(modifier = Modifier.height(24.dp))

      if (helpMode) {
        ModelStatus(modelInfo)
      } else {
        StartupStatus(state)
      }
    }
  }
}

/** Help page: the model is loaded, so show its status instead of the download controls. */
@Composable
private fun ModelStatus(info: DownloadedModelInfo?) {
  Text(stringResource(R.string.intro_status_loaded), style = MaterialTheme.typography.titleSmall)
  info?.downloadedAtMillis?.let { millis ->
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    Text(
      stringResource(R.string.intro_status_downloaded, date),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** Startup: the check, download and load sequence. */
@Composable
private fun StartupStatus(state: IntroState) {
  val scope = rememberCoroutineScope()
  when (val phase = state.phase) {
    is IntroPhase.Checking, is IntroPhase.LoadingModel -> {
      CircularProgressIndicator()
      Spacer(modifier = Modifier.height(12.dp))
      if (phase is IntroPhase.LoadingModel) {
        Text(stringResource(R.string.intro_loading_model), style = MaterialTheme.typography.bodyMedium)
      }
    }
    is IntroPhase.NotDownloaded -> {
      Text(
        stringResource(R.string.intro_wifi_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Button(onClick = { scope.launch { state.downloadAndLoad() } }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.intro_download_button))
      }
    }
    is IntroPhase.Downloading -> {
      Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
          progress = { phase.percent / 100f },
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          stringResource(R.string.intro_downloading_button, phase.percent),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
        )
      }
    }
    is IntroPhase.DownloadError -> ErrorBlock(stringResource(R.string.intro_download_error, phase.message)) { scope.launch { state.downloadAndLoad() } }
    is IntroPhase.LoadError -> ErrorBlock(stringResource(R.string.intro_load_error, phase.message)) { scope.launch { state.loadModel() } }
    is IntroPhase.Ready -> Unit // Handled by the LaunchedEffect in IntroScreen; nothing to render.
  }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = onRetry) { Text(stringResource(R.string.intro_retry_button)) }
  }
}
