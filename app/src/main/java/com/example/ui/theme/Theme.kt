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
    primary = Color(0xFF9ECAFF),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    tertiary = Color(0xFFBFC8D2),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = Color(0xFFC1C7CE)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ProfPrimary,
    primaryContainer = ProfPrimaryContainer,
    onPrimaryContainer = ProfOnPrimaryContainer,
    secondary = ProfSecondary,
    tertiary = ProfTertiary,
    background = ProfBackgroundLight,
    onBackground = ProfOnBackgroundLight,
    surface = ProfSurfaceLight,
    onSurface = ProfOnBackgroundLight,
    surfaceVariant = ProfSurfaceVariantLight,
    onSurfaceVariant = ProfOnSurfaceVariantLight,
    outline = ProfOutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Prefer our custom polished brand colors over generic material dynamic colors
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
