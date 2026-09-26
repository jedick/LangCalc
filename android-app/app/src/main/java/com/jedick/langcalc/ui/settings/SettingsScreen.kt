package com.jedick.langcalc.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jedick.langcalc.BuildConfig
import com.jedick.langcalc.R
import com.jedick.langcalc.data.AppLanguage
import com.jedick.langcalc.data.AppSettings
import com.jedick.langcalc.data.AsrLanguage
import com.jedick.langcalc.data.ModelRepository
import com.jedick.langcalc.data.UpdateCheckResult
import com.jedick.langcalc.ui.common.AppTopBar
import kotlinx.coroutines.launch

private const val SOURCE_URL = "https://github.com/jedick/LangCalc"

private sealed interface UpdateUiState {
  data object Idle : UpdateUiState
  data object Checking : UpdateUiState
  data object UpToDate : UpdateUiState
  data object UpdateAvailable : UpdateUiState
  data class Error(val message: String) : UpdateUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  modelRepository: ModelRepository,
  onBack: () -> Unit,
  onLanguageChanged: () -> Unit,
  onModelDeleted: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var language by remember { mutableStateOf(settings.language) }
  var asrLanguage by remember { mutableStateOf(settings.asrLanguage) }
  var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  val isDownloaded = modelRepository.isDownloaded()
  val modelSizeMb = modelRepository.modelSizeBytes() / (1024 * 1024)

  Scaffold(topBar = { AppTopBar(title = stringResource(R.string.settings_title), onBackClick = onBack) }) { padding ->
    Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(horizontal = 20.dp, vertical = 8.dp)) {
      // --- App language ---
      SettingsSectionTitle(stringResource(R.string.settings_section_language))
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = language == AppLanguage.ENGLISH,
          onClick = {
            if (language != AppLanguage.ENGLISH) {
              language = AppLanguage.ENGLISH
              settings.language = AppLanguage.ENGLISH
              onLanguageChanged()
            }
          },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
          Text(stringResource(R.string.settings_lang_en))
        }
        SegmentedButton(
          selected = language == AppLanguage.CHINESE,
          onClick = {
            if (language != AppLanguage.CHINESE) {
              language = AppLanguage.CHINESE
              settings.language = AppLanguage.CHINESE
              onLanguageChanged()
            }
          },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
          Text(stringResource(R.string.settings_lang_zh))
        }
      }
      HintText(stringResource(R.string.settings_language_hint))

      Spacer(modifier = Modifier.height(24.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(24.dp))

      // --- Voice input language ---
      SettingsSectionTitle(stringResource(R.string.settings_section_voice))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        VoiceLanguageOption(
          label = stringResource(R.string.settings_voice_auto),
          selected = asrLanguage == AsrLanguage.AUTO,
          onClick = {
            asrLanguage = AsrLanguage.AUTO
            settings.asrLanguage = AsrLanguage.AUTO
          },
        )
        VoiceLanguageOption(
          label = stringResource(R.string.settings_voice_en),
          selected = asrLanguage == AsrLanguage.ENGLISH,
          onClick = {
            asrLanguage = AsrLanguage.ENGLISH
            settings.asrLanguage = AsrLanguage.ENGLISH
          },
        )
        VoiceLanguageOption(
          label = stringResource(R.string.settings_voice_zh),
          selected = asrLanguage == AsrLanguage.CHINESE,
          onClick = {
            asrLanguage = AsrLanguage.CHINESE
            settings.asrLanguage = AsrLanguage.CHINESE
          },
        )
      }
      HintText(stringResource(R.string.settings_voice_hint))

      Spacer(modifier = Modifier.height(24.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(24.dp))

      // --- Model ---
      SettingsSectionTitle(stringResource(R.string.settings_section_model))
      Text(
        if (isDownloaded) stringResource(R.string.settings_model_status_downloaded, "$modelSizeMb MB")
        else stringResource(R.string.settings_model_status_not_downloaded),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        enabled = isDownloaded && updateState != UpdateUiState.Checking,
        onClick = {
          updateState = UpdateUiState.Checking
          scope.launch {
            updateState =
              when (val result = modelRepository.checkForUpdate()) {
                is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheckResult.UpdateAvailable -> UpdateUiState.UpdateAvailable
                is UpdateCheckResult.Error -> UpdateUiState.Error(result.message)
              }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (updateState == UpdateUiState.Checking) {
          CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.height(0.dp))
          Text(" " + stringResource(R.string.settings_checking_updates))
        } else {
          Text(stringResource(R.string.settings_check_updates))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(enabled = isDownloaded, onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_delete_model), color = MaterialTheme.colorScheme.error)
      }
      if (!isDownloaded) {
        HintText(stringResource(R.string.settings_delete_model_disabled_hint))
      }

      Spacer(modifier = Modifier.height(24.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(24.dp))

      // --- About ---
      SettingsSectionTitle(stringResource(R.string.settings_section_about))
      Text(
        stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      TextButton(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) },
        contentPadding = PaddingValues(0.dp),
      ) {
        Text(stringResource(R.string.settings_source_link))
      }
    }
  }

  when (val state = updateState) {
    UpdateUiState.UpToDate ->
      InfoDialog(text = stringResource(R.string.settings_up_to_date)) { updateState = UpdateUiState.Idle }
    UpdateUiState.UpdateAvailable ->
      AlertDialog(
        onDismissRequest = { updateState = UpdateUiState.Idle },
        title = { Text(stringResource(R.string.settings_update_available_title)) },
        text = { Text(stringResource(R.string.settings_update_available_body)) },
        confirmButton = {
          TextButton(
            onClick = {
              updateState = UpdateUiState.Idle
              modelRepository.delete()
              onModelDeleted()
            }
          ) {
            Text(stringResource(R.string.settings_update_download))
          }
        },
        dismissButton = { TextButton(onClick = { updateState = UpdateUiState.Idle }) { Text(stringResource(R.string.settings_cancel)) } },
      )
    is UpdateUiState.Error ->
      InfoDialog(text = stringResource(R.string.settings_update_check_failed, state.message)) { updateState = UpdateUiState.Idle }
    else -> Unit
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
      text = { Text(stringResource(R.string.settings_delete_confirm_body, "$modelSizeMb MB")) },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirm = false
            modelRepository.delete()
            onModelDeleted()
          }
        ) {
          Text(stringResource(R.string.settings_delete_confirm_button), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.settings_cancel)) } },
    )
  }
}

@Composable
private fun SettingsSectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun HintText(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp),
  )
}

@Composable
private fun VoiceLanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
  if (selected) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
  } else {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
  }
}

@Composable
private fun InfoDialog(text: String, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    text = { Text(text) },
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_ok)) } },
  )
}
