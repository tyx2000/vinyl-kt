package com.tyxu4459.expovinyl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VinylColorScheme = lightColorScheme(
  primary = MatchaDeep,
  onPrimary = SurfaceLight,
  primaryContainer = MatchaSoft,
  onPrimaryContainer = TextStrong,
  secondary = MatchaGreen,
  onSecondary = SurfaceLight,
  secondaryContainer = SurfaceTint,
  onSecondaryContainer = TextStrong,
  tertiary = MatchaLeaf,
  onTertiary = SurfaceLight,
  background = Cloud,
  onBackground = TextStrong,
  surface = SurfaceLight,
  onSurface = TextStrong,
  surfaceVariant = Ice,
  onSurfaceVariant = Color(0xFF5B745D),
  outline = OutlineSoft,
)

@Composable
fun VinylTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = VinylColorScheme,
    typography = Typography,
    content = content,
  )
}
