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
    strokeWidth: Dp = 1.4.dp,
    dashLength: Dp = 5.5.dp,
    dashGap: Dp = 3.5.dp,
    inset: Dp = 4.5.dp,
    cornerRadius: Dp = 18.dp
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

    // Subtle recessed stitch shadow underneath thread
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.14f),
        style = Stroke(
            width = strokePx + 0.8f,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), dashGap.toPx()),
                0f
            )
        )
    )

    // Primary waxed metallic/linen stitch thread
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
