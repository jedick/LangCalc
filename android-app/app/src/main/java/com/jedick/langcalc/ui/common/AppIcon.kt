package com.jedick.langcalc.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.jedick.langcalc.R

/** The app icon (speech bubble on a teal rounded square), drawn at [size]. */
@Composable
fun AppIcon(size: Dp, modifier: Modifier = Modifier) {
  Image(painter = painterResource(R.drawable.ic_app_mark), contentDescription = null, modifier = modifier.size(size))
}
