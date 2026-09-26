package com.jedick.langcalc

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jedick.langcalc.data.AppSettings
import com.jedick.langcalc.data.LocaleHelper
import com.jedick.langcalc.ui.LangCalcApp
import com.jedick.langcalc.ui.theme.LangCalcTheme

class MainActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    val languageTag = AppSettings.languageTagBlocking(newBase)
    super.attachBaseContext(LocaleHelper.wrap(newBase, languageTag))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as LangCalcApplication

    setContent {
      LangCalcTheme {
        LangCalcApp(
          settings = app.settings,
          modelRepository = app.modelRepository,
          engine = app.engine,
          onLanguageChanged = { recreate() },
        )
      }
    }
  }
}
