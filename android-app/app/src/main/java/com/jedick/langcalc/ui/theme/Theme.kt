package com.jedick.langcalc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Matches langcalc_icon_android.svg.
val LangCalcTeal = Color(0xFF12896B)
val LangCalcTealDark = Color(0xFF0B5C48)

private val LightColors =
  lightColorScheme(
    primary = LangCalcTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6EDDB),
    onPrimaryContainer = Color(0xFF00201A),
    secondaryContainer = Color(0xFFE6F4EF),
  )

private val DarkColors =
  darkColorScheme(
    primary = Color(0xFF6FDBBB),
    onPrimary = Color(0xFF00382C),
    primaryContainer = LangCalcTealDark,
    onPrimaryContainer = Color(0xFFB6EDDB),
  )

@Composable
fun LangCalcTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
  val context = LocalContext.current
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      darkTheme -> DarkColors
      else -> LightColors
    }
  MaterialTheme(colorScheme = colorScheme, content = content)
}
