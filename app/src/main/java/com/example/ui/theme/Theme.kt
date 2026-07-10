package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberBlue,
    secondary = NeonTeal,
    tertiary = CyberGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onPrimary = Color.White,
    onSecondary = Color(0xFF121212),
    onTertiary = Color(0xFF121212)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CyberBlue,
    secondary = NeonTeal,
    tertiary = CyberGold,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Custom branded colors look best, let's keep dynamicColor option
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = androidx.compose.ui.platform.LocalView.current
  if (!view.isInEditMode) {
    androidx.compose.runtime.SideEffect {
      val window = (view.context as? android.app.Activity)?.window
      window?.statusBarColor = android.graphics.Color.TRANSPARENT
      window?.navigationBarColor = android.graphics.Color.TRANSPARENT
      val viewHelper = androidx.core.view.WindowCompat.getInsetsController(window ?: return@SideEffect, view)
      viewHelper.isAppearanceLightStatusBars = !darkTheme
      viewHelper.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
