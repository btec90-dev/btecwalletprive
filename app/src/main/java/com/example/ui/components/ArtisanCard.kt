package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalArtisanTheme

@Composable
fun ArtisanCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    containerColor: Color = LocalArtisanTheme.current.surfaceColor,
    contentColor: Color = LocalArtisanTheme.current.onPrimaryContainer,
    elevation: Dp = 2.dp,
    showStitching: Boolean = true,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val effectiveBorder = border ?: BorderStroke(1.dp, theme.cardBorderColor.copy(alpha = 0.6f))

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = effectiveBorder
    ) {
        val stitchModifier = if (showStitching) {
            Modifier.stitchedBorder(
                stitchColor = theme.stitchingColor.copy(alpha = 0.55f),
                strokeWidth = 1.2.dp,
                dashLength = 5.dp,
                dashGap = 3.5.dp,
                inset = 4.dp,
                cornerRadius = 18.dp
            )
        } else {
            Modifier
        }

        Box(modifier = stitchModifier) {
            content()
        }
    }
}
