package com.tyxu4459.expovinyl.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.fillMaxSize()) {
    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFFF9FCF7),
            Color(0xFFF5FAF2),
            Color(0xFFF0F7EC),
          ),
        ),
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(
            Color(0xFF8FCB98).copy(alpha = 0.09f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.18f, size.height * 0.16f),
          radius = size.minDimension * 0.48f,
        ),
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(
            Color(0xFF7BAE7F).copy(alpha = 0.08f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.82f, size.height * 0.26f),
          radius = size.minDimension * 0.54f,
        ),
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(
            Color(0xFFDDEED8).copy(alpha = 0.14f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.52f, size.height * 0.74f),
          radius = size.minDimension * 0.62f,
        ),
    )

    drawRect(
      brush =
        Brush.linearGradient(
          colors = listOf(
            Color(0xFF7BAE7F).copy(alpha = 0.025f),
            Color.Transparent,
            Color(0xFF5E8F63).copy(alpha = 0.03f),
          ),
          start = Offset(size.width * 0.04f, size.height * 0.18f),
          end = Offset(size.width * 0.92f, size.height * 0.86f),
        ),
    )
  }
}
