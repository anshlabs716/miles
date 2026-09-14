package com.example.miles.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isGlass = LocalLiquidGlassEnabled.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    val bgModifier = if (isGlass) {
        val glassBg = surfaceColor.copy(alpha = 0.68f)
        val glassBorderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                primaryColor.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 600f)
        )

        modifier
            .clip(shape)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.09f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.4f
                    )
                )
            }
            .background(glassBg, shape)
            .border(BorderStroke(borderWidth, glassBorderBrush), shape)
    } else {
        modifier
            .clip(shape)
            .background(surfaceColor, shape)
            .border(BorderStroke(borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), shape)
    }

    Box(modifier = bgModifier, content = content)
}

@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = shape,
        borderWidth = 0.8.dp,
        content = content
    )
}
