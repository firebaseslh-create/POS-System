package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalPrimaryDark,
    onPrimary = NaturalOnPrimaryDark,
    primaryContainer = NaturalPrimaryContainerDark,
    onPrimaryContainer = NaturalOnPrimaryContainerDark,
    secondary = NaturalSecondaryDark,
    onSecondary = NaturalOnSecondaryDark,
    secondaryContainer = NaturalSecondaryContainerDark,
    onSecondaryContainer = NaturalOnSecondaryContainerDark,
    tertiary = NaturalTertiaryDark,
    onTertiary = NaturalOnTertiaryDark,
    tertiaryContainer = NaturalTertiaryContainerDark,
    onTertiaryContainer = NaturalOnTertiaryContainerDark,
    background = NaturalBgDark,
    onBackground = NaturalTextPrimaryDark,
    surface = NaturalSurfaceDark,
    onSurface = NaturalTextPrimaryDark,
    surfaceVariant = NaturalSurfaceVariantDark,
    onSurfaceVariant = NaturalTextSecondaryDark,
    surfaceContainer = NaturalSurfaceCardDark,
    surfaceContainerHigh = Color(0xFF2B2830),
    surfaceContainerLowest = Color(0xFF0F0D13),
    outline = NaturalBorderDark,
    outlineVariant = NaturalBorderSubtleDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalPrimary,
    onPrimary = NaturalOnPrimary,
    primaryContainer = NaturalPrimaryContainer,
    onPrimaryContainer = NaturalOnPrimaryContainer,
    secondary = NaturalSecondary,
    onSecondary = Color.White,
    secondaryContainer = NaturalSecondaryContainer,
    onSecondaryContainer = NaturalOnSecondaryContainer,
    tertiary = NaturalTertiary,
    onTertiary = Color.White,
    tertiaryContainer = NaturalTertiaryContainer,
    onTertiaryContainer = NaturalOnTertiaryContainer,
    background = NaturalBgLight,
    onBackground = NaturalTextPrimary,
    surface = NaturalSurfaceLight,
    onSurface = NaturalTextPrimary,
    surfaceVariant = NaturalSurfaceVariantLight,
    onSurfaceVariant = NaturalTextSecondary,
    surfaceContainer = Color(0xFFF7F2FA),
    surfaceContainerHigh = Color(0xFFECE6F0),
    outline = NaturalBorderLight,
    outlineVariant = NaturalBorderSubtle,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For branded POS styling, default to false so brand emerald color stays sharp
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

  val configuration = LocalConfiguration.current
  val currentDensity = LocalDensity.current
  val screenWidthDp = configuration.screenWidthDp.coerceAtLeast(320)
  // Automatically scale font size smoothly based on screen width and clamp extreme system font scales
  // so every screen's body text and header titles look well-proportioned and readable without clipping
  val widthScaleFactor = (screenWidthDp.toFloat() / 390f).coerceIn(0.88f, 1.10f)
  val clampedSystemFontScale = currentDensity.fontScale.coerceIn(0.90f, 1.06f)
  val autoFontScale = (widthScaleFactor * clampedSystemFontScale).coerceIn(0.85f, 1.08f)

  val adaptiveTypography = Typography.copy(
    headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * widthScaleFactor),
    headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * widthScaleFactor),
    headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * widthScaleFactor),
    titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * widthScaleFactor),
    titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * widthScaleFactor),
    titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * widthScaleFactor),
    bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * widthScaleFactor),
    bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * widthScaleFactor),
    bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * widthScaleFactor),
    labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * widthScaleFactor),
    labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * widthScaleFactor),
    labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * widthScaleFactor)
  )

  val adaptiveDensity = Density(
    density = currentDensity.density,
    fontScale = autoFontScale
  )

  CompositionLocalProvider(LocalDensity provides adaptiveDensity) {
    MaterialTheme(colorScheme = colorScheme, typography = adaptiveTypography, content = content)
  }
}

