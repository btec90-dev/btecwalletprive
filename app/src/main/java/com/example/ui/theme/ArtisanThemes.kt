package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ArtisanThemeConfig(
    val id: String,
    val name: String,
    val isClassic: Boolean,
    val primaryColor: Color,
    val onPrimaryColor: Color = Color.White,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceColor: Color,
    val backgroundColor: Color,
    val cardBorderColor: Color,
    val stitchingColor: Color,
    val isSaddleStitched: Boolean = false
)

object ArtisanThemes {
    val BROWN_LEATHER = ArtisanThemeConfig(
        id = "brown_leather",
        name = "Brown Leather",
        isClassic = true,
        primaryColor = Color(0xFF4E342E), // Rich deep brown
        primaryContainer = Color(0xFFEFEBE9),
        onPrimaryContainer = Color(0xFF2C1810),
        surfaceColor = Color(0xFFFFFBF7), // Warm leather tint surface
        backgroundColor = Color(0xFFF4ECE1), // Warm earth background
        cardBorderColor = Color(0xFFD7CCC8),
        stitchingColor = Color(0xFF8D6E63)
    )

    val VINTAGE_SADDLE = ArtisanThemeConfig(
        id = "vintage_saddle",
        name = "Vintage Saddle",
        isClassic = true,
        primaryColor = Color(0xFF6D4C41),
        primaryContainer = Color(0xFFF3EFEA),
        onPrimaryContainer = Color(0xFF3B2219),
        surfaceColor = Color(0xFFFFFDF9),
        backgroundColor = Color(0xFFF5EFEB),
        cardBorderColor = Color(0xFFD2B48C),
        stitchingColor = Color(0xFFB5651D)
    )

    val ESPRESSO = ArtisanThemeConfig(
        id = "espresso",
        name = "Espresso",
        isClassic = true,
        primaryColor = Color(0xFF3E2723),
        primaryContainer = Color(0xFFE8E0DD),
        onPrimaryContainer = Color(0xFF1B0F0D),
        surfaceColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFFEFEAE6),
        cardBorderColor = Color(0xFFBCAAA4),
        stitchingColor = Color(0xFF6D4C41)
    )

    val CARAMEL = ArtisanThemeConfig(
        id = "caramel",
        name = "Caramel",
        isClassic = true,
        primaryColor = Color(0xFF8D6E63),
        primaryContainer = Color(0xFFF5EBE6),
        onPrimaryContainer = Color(0xFF4E342E),
        surfaceColor = Color(0xFFFFFDFB),
        backgroundColor = Color(0xFFFAF7F2),
        cardBorderColor = Color(0xFFE0D3CD),
        stitchingColor = Color(0xFFA1887F)
    )

    val WHITE_LEATHER = ArtisanThemeConfig(
        id = "white_leather",
        name = "White Leather",
        isClassic = true,
        primaryColor = Color(0xFF3E3A37),
        primaryContainer = Color(0xFFECECE7),
        onPrimaryContainer = Color(0xFF2C2825),
        surfaceColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFFF5F5F0),
        cardBorderColor = Color(0xFF5D4037),
        stitchingColor = Color(0xFF5D4037),
        isSaddleStitched = true
    )

    val FOREST_GREEN = ArtisanThemeConfig(
        id = "forest_green",
        name = "Forest Green",
        isClassic = false,
        primaryColor = Color(0xFF1B4D3E),
        primaryContainer = Color(0xFFE1ECE7),
        onPrimaryContainer = Color(0xFF0F3026),
        surfaceColor = Color(0xFFFFFFFD),
        backgroundColor = Color(0xFFF3F7F5),
        cardBorderColor = Color(0xFFB2D0C6),
        stitchingColor = Color(0xFF2E6B58)
    )

    val DEEP_BLUE = ArtisanThemeConfig(
        id = "deep_blue",
        name = "Deep Blue",
        isClassic = false,
        primaryColor = Color(0xFF0D2B45),
        primaryContainer = Color(0xFFE0EBF3),
        onPrimaryContainer = Color(0xFF061827),
        surfaceColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFFF2F6F9),
        cardBorderColor = Color(0xFFAEC7D9),
        stitchingColor = Color(0xFF20486D)
    )

    val BURGUNDY = ArtisanThemeConfig(
        id = "burgundy",
        name = "Burgundy",
        isClassic = false,
        primaryColor = Color(0xFF581845),
        primaryContainer = Color(0xFFF6E2EE),
        onPrimaryContainer = Color(0xFF390D2C),
        surfaceColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFFF8F3F6),
        cardBorderColor = Color(0xFFD6AEC8),
        stitchingColor = Color(0xFF78255F)
    )

    val SAHARA_GOLD = ArtisanThemeConfig(
        id = "sahara_gold",
        name = "Sahara Gold",
        isClassic = false,
        primaryColor = Color(0xFF9A7B18),
        primaryContainer = Color(0xFFFCF4D9),
        onPrimaryContainer = Color(0xFF4A3A05),
        surfaceColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFFFAF8F0),
        cardBorderColor = Color(0xFFE8D490),
        stitchingColor = Color(0xFFD4AF37)
    )

    val ALL_THEMES = listOf(
        BROWN_LEATHER,
        VINTAGE_SADDLE,
        ESPRESSO,
        CARAMEL,
        WHITE_LEATHER,
        FOREST_GREEN,
        DEEP_BLUE,
        BURGUNDY,
        SAHARA_GOLD
    )

    fun getThemeById(id: String): ArtisanThemeConfig {
        return ALL_THEMES.firstOrNull { it.id == id } ?: BROWN_LEATHER
    }

    fun toColorScheme(config: ArtisanThemeConfig): ColorScheme {
        return lightColorScheme(
            primary = config.primaryColor,
            onPrimary = config.onPrimaryColor,
            primaryContainer = config.primaryContainer,
            onPrimaryContainer = config.onPrimaryContainer,
            secondary = config.stitchingColor,
            onSecondary = Color.White,
            secondaryContainer = config.primaryContainer,
            onSecondaryContainer = config.onPrimaryContainer,
            tertiary = config.stitchingColor,
            background = config.backgroundColor,
            onBackground = Color(0xFF2C1810),
            surface = config.surfaceColor,
            onSurface = Color(0xFF2C1810),
            surfaceVariant = config.primaryContainer,
            onSurfaceVariant = config.onPrimaryContainer,
            outline = config.cardBorderColor,
            outlineVariant = config.cardBorderColor.copy(alpha = 0.6f)
        )
    }
}

val LocalArtisanTheme = staticCompositionLocalOf { ArtisanThemes.BROWN_LEATHER }
