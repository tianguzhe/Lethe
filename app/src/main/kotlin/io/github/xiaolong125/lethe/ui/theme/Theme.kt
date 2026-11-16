package io.github.xiaolong125.lethe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
  primary = primaryLight, onPrimary = onPrimaryLight,
  primaryContainer = primaryContainerLight, onPrimaryContainer = onPrimaryContainerLight,
  secondary = secondaryLight, onSecondary = onSecondaryLight,
  secondaryContainer = secondaryContainerLight, onSecondaryContainer = onSecondaryContainerLight,
  tertiary = tertiaryLight, onTertiary = onTertiaryLight,
  tertiaryContainer = tertiaryContainerLight, onTertiaryContainer = onTertiaryContainerLight,
  error = errorLight, onError = onErrorLight, errorContainer = errorContainerLight, onErrorContainer = onErrorContainerLight,
  background = backgroundLight, onBackground = onBackgroundLight,
  surface = backgroundLight, onSurface = onBackgroundLight,
  surfaceVariant = surfaceVariantLight, onSurfaceVariant = onSurfaceVariantLight,
  outline = outlineLight, outlineVariant = outlineVariantLight,
  surfaceContainerLowest = surfaceContainerLowestLight, surfaceContainerLow = surfaceContainerLowLight,
  surfaceContainer = surfaceContainerLight, surfaceContainerHigh = surfaceContainerHighLight,
  surfaceContainerHighest = surfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = primaryDark, onPrimary = onPrimaryDark,
  primaryContainer = primaryContainerDark, onPrimaryContainer = onPrimaryContainerDark,
  secondary = secondaryDark, onSecondary = onSecondaryDark,
  secondaryContainer = secondaryContainerDark, onSecondaryContainer = onSecondaryContainerDark,
  tertiary = tertiaryDark, onTertiary = onTertiaryDark,
  tertiaryContainer = tertiaryContainerDark, onTertiaryContainer = onTertiaryContainerDark,
  error = errorDark, onError = onErrorDark, errorContainer = errorContainerDark, onErrorContainer = onErrorContainerDark,
  background = backgroundDark, onBackground = onBackgroundDark,
  surface = backgroundDark, onSurface = onBackgroundDark,
  surfaceVariant = surfaceVariantDark, onSurfaceVariant = onSurfaceVariantDark,
  outline = outlineDark, outlineVariant = outlineVariantDark,
  surfaceContainerLowest = surfaceContainerLowestDark, surfaceContainerLow = surfaceContainerLowDark,
  surfaceContainer = surfaceContainerDark, surfaceContainerHigh = surfaceContainerHighDark,
  surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun LetheTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
