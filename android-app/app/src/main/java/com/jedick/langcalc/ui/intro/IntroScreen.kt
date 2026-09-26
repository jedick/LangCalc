package com.jedick.langcalc.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jedick.langcalc.R
import com.jedick.langcalc.data.ModelConstants
import com.jedick.langcalc.ui.common.AppTopBar
import com.jedick.langcalc.ui.theme.LangCalcTeal
import kotlinx.coroutines.launch

private fun formatMegabytes(bytes: Long): String = "${bytes / (1024 * 1024)} MB"

@Composable
fun IntroScreen(state: IntroState, onSettingsClick: () -> Unit, onReady: () -> Unit) {
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) { state.checkInitialState() }
  LaunchedEffect(state.phase) {
    if (state.phase is IntroPhase.Ready) onReady()
  }

  Scaffold(topBar = { AppTopBar(onSettingsClick = onSettingsClick) }) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier = Modifier.size(88.dp).clip(CircleShape).background(LangCalcTeal),
        contentAlignment = Alignment.Center,
      ) {
        androidx.compose.material3.Icon(
          Icons.Filled.Calculate,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(44.dp),
        )
      }
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
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        stringResource(R.string.intro_model_name, formatMegabytes(ModelConstants.APPROX_SIZE_BYTES)),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(32.dp))

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
            stringResource(R.string.intro_wifi_hint, formatMegabytes(ModelConstants.APPROX_SIZE_BYTES)),
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
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        }
        is IntroPhase.DownloadError -> {
          ErrorBlock(stringResource(R.string.intro_download_error, phase.message)) {
            scope.launch { state.downloadAndLoad() }
          }
        }
        is IntroPhase.LoadError -> {
          ErrorBlock(stringResource(R.string.intro_load_error, phase.message)) { scope.launch { state.loadModel() } }
        }
        is IntroPhase.Ready -> Unit // Handled by the LaunchedEffect above; nothing to render.
      }
    }
  }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = onRetry) { Text(stringResource(R.string.intro_retry_button)) }
  }
}
