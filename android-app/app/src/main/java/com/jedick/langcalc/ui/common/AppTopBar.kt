package com.jedick.langcalc.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jedick.langcalc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String = "", onSettingsClick: (() -> Unit)? = null, onBackClick: (() -> Unit)? = null) {
  CenterAlignedTopAppBar(
    title = { Text(title) },
    navigationIcon = {
      if (onBackClick != null) {
        IconButton(onClick = onBackClick) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back_content_description))
        }
      }
    },
    actions = {
      if (onSettingsClick != null) {
        IconButton(onClick = onSettingsClick) {
          Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_gear_content_description))
        }
      }
    },
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
  )
}
