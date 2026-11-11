package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.stitchedBorder(
    stitchColor: Color,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 6.dp,
    dashGap: Dp = 4.dp,
    inset: Dp = 5.dp,
    cornerRadius: Dp = 16.dp
): Modifier = this.drawBehind {
    val insetPx = inset.toPx()
    val strokePx = strokeWidth.toPx()
    val cornerPx = (cornerRadius.toPx() - insetPx).coerceAtLeast(0f)

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    offset = Offset(insetPx, insetPx),
                    size = size.copy(
                        width = (size.width - insetPx * 2).coerceAtLeast(0f),
                        height = (size.height - insetPx * 2).coerceAtLeast(0f)
                    )
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )
        )
    }

    drawPath(
        path = path,
        color = stitchColor,
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), dashGap.toPx()),
                0f
            )
        )
    )
}
