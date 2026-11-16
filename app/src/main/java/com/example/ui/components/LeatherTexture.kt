package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArtisanThemeConfig
import kotlin.math.abs

/**
 * Applies a subtle, organic skeuomorphic leather texture (multi-tone hide sheen,
 * deterministic pebbled grain pores, and faint hide creases) purely with Compose
 * drawing modifiers and zero bitmap overhead.
 */
fun Modifier.artisanLeatherTexture(
    baseColor: Color,
    isDark: Boolean = false,
    grainIntensity: Float = 1.0f,
    showVignette: Boolean = true
): Modifier = this.drawWithCache {
    val topHighlight = lerp(baseColor, Color.White, if (isDark) 0.055f else 0.16f)
    val midTone = baseColor
    val bottomShade = lerp(baseColor, Color.Black, if (isDark) 0.18f else 0.055f)

    val verticalHideBrush = Brush.verticalGradient(
        colors = listOf(topHighlight, midTone, bottomShade)
    )
    val diagonalSheenBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.025f else 0.07f),
            Color.Transparent,
            Color.Black.copy(alpha = if (isDark) 0.08f else 0.03f)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    val darkPoreAlpha = (if (isDark) 0.16f else 0.042f) * grainIntensity
    val lightPoreAlpha = (if (isDark) 0.038f else 0.14f) * grainIntensity
    val creaseAlpha = (if (isDark) 0.045f else 0.024f) * grainIntensity

    val darkPoreColor = Color.Black.copy(alpha = darkPoreAlpha.coerceIn(0f, 0.35f))
    val lightPoreColor = Color.White.copy(alpha = lightPoreAlpha.coerceIn(0f, 0.35f))
    val creaseColor = Color.Black.copy(alpha = creaseAlpha.coerceIn(0f, 0.15f))
    val edgeShadow = Color.Black.copy(alpha = if (isDark) 0.14f else 0.045f)
    val topRimGlow = Color.White.copy(alpha = if (isDark) 0.06f else 0.22f)

    val cellPx = 11.dp.toPx().coerceAtLeast(8f)
    val creaseStepPx = 26.dp.toPx().coerceAtLeast(16f)
    val baseRadius = 1.05.dp.toPx().coerceAtLeast(1f)

    onDrawBehind {
        // 1. Base hide gradient & soft directional sheen
        drawRect(brush = verticalHideBrush)
        drawRect(brush = diagonalSheenBrush)

        // 2. Deterministic organic pebble grain pores
        val cols = (size.width / cellPx).toInt() + 1
        val rows = (size.height / (cellPx * 0.88f)).toInt() + 1
        val rowStep = cellPx * 0.88f

        for (r in 0..rows) {
            val staggerX = if (r % 2 == 0) 0f else cellPx * 0.45f
            val cyBase = r * rowStep + 4f
            if (cyBase > size.height + cellPx) break
            for (c in 0..cols) {
                val hash = leatherCellHash(c, r)
                val jx = (((hash and 0xFF) / 255f) - 0.5f) * cellPx * 0.58f
                val jy = ((((hash ushr 8) and 0xFF) / 255f) - 0.5f) * rowStep * 0.58f
                val scale = 0.72f + (((hash ushr 16) and 0xFF) / 255f) * 0.65f

                val cx = c * cellPx + staggerX + jx
                val cy = cyBase + jy
                if (cx in -4f..(size.width + 4f) && cy in -4f..(size.height + 4f)) {
                    val poreR = baseRadius * scale
                    drawCircle(
                        color = darkPoreColor,
                        radius = poreR,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = lightPoreColor,
                        radius = poreR * 0.8f,
                        center = Offset(cx + poreR * 0.9f, cy + poreR * 0.9f)
                    )
                }
            }
        }

        // 3. Faint natural cross-grain hide creases
        var creaseX = -size.height * 0.3f
        var idx = 0
        while (creaseX < size.width + size.height * 0.3f) {
            val slant = if (idx % 2 == 0) 0.22f else -0.18f
            val x1 = creaseX
            val x2 = creaseX + size.height * slant
            drawLine(
                color = creaseColor,
                start = Offset(x1, 0f),
                end = Offset(x2, size.height),
                strokeWidth = 0.8f
            )
            creaseX += creaseStepPx
            idx++
        }

        // 4. Subtle top edge sheen & bottom burnished vignette
        if (showVignette) {
            drawLine(
                color = topRimGlow,
                start = Offset(0f, 1f),
                end = Offset(size.width, 1f),
                strokeWidth = 1.2f
            )
            drawLine(
                color = edgeShadow,
                start = Offset(0f, size.height - 1f),
                end = Offset(size.width, size.height - 1f),
                strokeWidth = 1.5f
            )
        }
    }
}

/**
 * Applies the artisan leather texture plus a subtle metallic top rim and
 * horizontal saddle-stitching seam for the bottom NavigationBar.
 */
fun Modifier.artisanNavigationBarLeather(
    theme: ArtisanThemeConfig,
    stitchInsetTop: Dp = 5.dp
): Modifier = this
    .artisanLeatherTexture(
        baseColor = theme.surfaceColor,
        isDark = theme.isDark,
        grainIntensity = 0.9f,
        showVignette = false
    )
    .drawWithCache {
        val metallicLineColor = theme.metallicAccent.copy(alpha = if (theme.isDark) 0.45f else 0.55f)
        val stitchColor = theme.stitchingColor.copy(alpha = 0.52f)
        val stitchShadowColor = Color.Black.copy(alpha = if (theme.isDark) 0.28f else 0.12f)
        val dashPx = 5.5.dp.toPx()
        val gapPx = 3.5.dp.toPx()
        val stitchY = stitchInsetTop.toPx()
        val stitchStroke = 1.2.dp.toPx()
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)

        onDrawBehind {
            // Top metallic hardware trim line
            drawLine(
                color = metallicLineColor,
                start = Offset(0f, 0.8f),
                end = Offset(size.width, 0.8f),
                strokeWidth = 1.4.dp.toPx()
            )
            // Recessed thread shadow
            drawLine(
                color = stitchShadowColor,
                start = Offset(8f, stitchY + 0.8f),
                end = Offset(size.width - 8f, stitchY + 0.8f),
                strokeWidth = stitchStroke + 0.6f,
                pathEffect = dashEffect
            )
            // Waxed horizontal saddle-stitch seam
            drawLine(
                color = stitchColor,
                start = Offset(8f, stitchY),
                end = Offset(size.width - 8f, stitchY),
                strokeWidth = stitchStroke,
                pathEffect = dashEffect
            )
        }
    }

private fun leatherCellHash(x: Int, y: Int): Int {
    var h = (x * 73856093) xor (y * 19349663)
    h = (h xor (h ushr 13)) * 1274126177
    return abs(h)
}
