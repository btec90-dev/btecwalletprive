package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ArtisanThemeConfig(
    val id: String,
    val name: String,
    val hardwareName: String,
    val isClassic: Boolean,
    val isDark: Boolean = false,
    val primaryColor: Color,
    val onPrimaryColor: Color = Color.White,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceColor: Color,
    val surfaceElevatedColor: Color,
    val onSurfaceColor: Color,
    val backgroundColor: Color,
    val onBackgroundColor: Color,
    val secondaryTextColor: Color,
    val cardBorderColor: Color,
    val stitchingColor: Color,
    val metallicAccent: Color,
    val isSaddleStitched: Boolean = true
)

object ArtisanThemes {
    val CLASSIC_SADDLE_TAN = ArtisanThemeConfig(
        id = "classic_saddle_tan",
        name = "Classic Saddle Tan",
        hardwareName = "Brushed Brass",
        isClassic = true,
        primaryColor = LeatherSaddleTan,
        primaryContainer = Color(0xFFF2E2CE),
        onPrimaryContainer = Color(0xFF331D0E),
        surfaceColor = Color(0xFFFFFAF4),
        surfaceElevatedColor = Color(0xFFF8ECDD),
        onSurfaceColor = Color(0xFF2B190E),
        backgroundColor = Color(0xFFEFE1D0),
        onBackgroundColor = Color(0xFF2B190E),
        secondaryTextColor = Color(0xFF6B5344),
        cardBorderColor = Color(0xFFCFA97E),
        stitchingColor = BrassBrushed,
        metallicAccent = BrassBrushed
    )

    val ESPRESSO_BROWN = ArtisanThemeConfig(
        id = "espresso_brown",
        name = "Espresso Brown",
        hardwareName = "Aged Bronze",
        isClassic = true,
        primaryColor = LeatherEspresso,
        primaryContainer = Color(0xFFECE2DC),
        onPrimaryContainer = Color(0xFF1F110C),
        surfaceColor = Color(0xFFFFFBF8),
        surfaceElevatedColor = Color(0xFFF5ECE6),
        onSurfaceColor = Color(0xFF241510),
        backgroundColor = Color(0xFFEDE2DA),
        onBackgroundColor = Color(0xFF241510),
        secondaryTextColor = Color(0xFF68534B),
        cardBorderColor = Color(0xFFBC9E8C),
        stitchingColor = BronzeAged,
        metallicAccent = BronzeAged
    )

    val OXBLOOD_BURGUNDY = ArtisanThemeConfig(
        id = "oxblood_burgundy",
        name = "Oxblood Burgundy",
        hardwareName = "Champagne Gold",
        isClassic = false,
        primaryColor = LeatherOxblood,
        primaryContainer = Color(0xFFF5E2E5),
        onPrimaryContainer = Color(0xFF380C14),
        surfaceColor = Color(0xFFFFFAFA),
        surfaceElevatedColor = Color(0xFFF9ECEE),
        onSurfaceColor = Color(0xFF2D1116),
        backgroundColor = Color(0xFFF3E6E8),
        onBackgroundColor = Color(0xFF2D1116),
        secondaryTextColor = Color(0xFF6D4B51),
        cardBorderColor = Color(0xFFD5A9B0),
        stitchingColor = GoldChampagne,
        metallicAccent = GoldChampagne
    )

    val MIDNIGHT_BLACK = ArtisanThemeConfig(
        id = "midnight_black",
        name = "Midnight Black",
        hardwareName = "Brushed Steel",
        isClassic = true,
        isDark = true,
        primaryColor = Color(0xFF2F353E),
        onPrimaryColor = Color(0xFFF3F6FA),
        primaryContainer = Color(0xFF272B32),
        onPrimaryContainer = Color(0xFFE6ECF2),
        surfaceColor = Color(0xFF1E2127),
        surfaceElevatedColor = Color(0xFF262A31),
        onSurfaceColor = Color(0xFFF0F3F7),
        backgroundColor = Color(0xFF131519),
        onBackgroundColor = Color(0xFFF0F3F7),
        secondaryTextColor = Color(0xFF9AA5B4),
        cardBorderColor = Color(0xFF424954),
        stitchingColor = SteelBrushed,
        metallicAccent = SteelBrushed
    )

    val BRITISH_RACING_GREEN = ArtisanThemeConfig(
        id = "british_racing_green",
        name = "British Racing Green",
        hardwareName = "Polished Brass",
        isClassic = false,
        primaryColor = LeatherRacingGreen,
        primaryContainer = Color(0xFFDFECE6),
        onPrimaryContainer = Color(0xFF0D281E),
        surfaceColor = Color(0xFFFAFDFB),
        surfaceElevatedColor = Color(0xFFEFF6F2),
        onSurfaceColor = Color(0xFF11261D),
        backgroundColor = Color(0xFFE8F0EC),
        onBackgroundColor = Color(0xFF11261D),
        secondaryTextColor = Color(0xFF4C675B),
        cardBorderColor = Color(0xFFA9C7B9),
        stitchingColor = BrassPolished,
        metallicAccent = BrassPolished
    )

    val COGNAC_AMBER = ArtisanThemeConfig(
        id = "cognac_amber",
        name = "Cognac Amber",
        hardwareName = "Warm Copper",
        isClassic = false,
        primaryColor = LeatherCognac,
        primaryContainer = Color(0xFFF7E5D8),
        onPrimaryContainer = Color(0xFF461D07),
        surfaceColor = Color(0xFFFFFAF5),
        surfaceElevatedColor = Color(0xFFFAEEE4),
        onSurfaceColor = Color(0xFF331808),
        backgroundColor = Color(0xFFF4E6D9),
        onBackgroundColor = Color(0xFF331808),
        secondaryTextColor = Color(0xFF74533E),
        cardBorderColor = Color(0xFFD9AE8F),
        stitchingColor = CopperWarm,
        metallicAccent = CopperWarm
    )

    val NAVY_BLUE = ArtisanThemeConfig(
        id = "navy_blue",
        name = "Navy Blue",
        hardwareName = "Satin Nickel",
        isClassic = false,
        primaryColor = LeatherNavy,
        primaryContainer = Color(0xFFE0E8F3),
        onPrimaryContainer = Color(0xFF0A1B33),
        surfaceColor = Color(0xFFFAFBFD),
        surfaceElevatedColor = Color(0xFFEDF2F8),
        onSurfaceColor = Color(0xFF0F1F36),
        backgroundColor = Color(0xFFE9EFF6),
        onBackgroundColor = Color(0xFF0F1F36),
        secondaryTextColor = Color(0xFF4E607A),
        cardBorderColor = Color(0xFFAABBD2),
        stitchingColor = NickelSatin,
        metallicAccent = NickelSatin
    )

    val VINTAGE_SLATE_GRAY = ArtisanThemeConfig(
        id = "vintage_slate_gray",
        name = "Vintage Slate Gray",
        hardwareName = "Pewter Gunmetal",
        isClassic = true,
        primaryColor = LeatherSlate,
        primaryContainer = Color(0xFFE5E8EC),
        onPrimaryContainer = Color(0xFF1E2328),
        surfaceColor = Color(0xFFFCFCFD),
        surfaceElevatedColor = Color(0xFFF0F2F4),
        onSurfaceColor = Color(0xFF22272C),
        backgroundColor = Color(0xFFE9ECEF),
        onBackgroundColor = Color(0xFF22272C),
        secondaryTextColor = Color(0xFF5D6670),
        cardBorderColor = Color(0xFFB5BDC5),
        stitchingColor = GunmetalPewter,
        metallicAccent = GunmetalPewter
    )

    val WARM_CARAMEL = ArtisanThemeConfig(
        id = "warm_caramel",
        name = "Warm Caramel",
        hardwareName = "Antique Gold",
        isClassic = true,
        primaryColor = LeatherCaramel,
        primaryContainer = Color(0xFFF6EAD9),
        onPrimaryContainer = Color(0xFF422810),
        surfaceColor = Color(0xFFFFFCF7),
        surfaceElevatedColor = Color(0xFFFAF0E2),
        onSurfaceColor = Color(0xFF321E0C),
        backgroundColor = Color(0xFFF4EBDD),
        onBackgroundColor = Color(0xFF321E0C),
        secondaryTextColor = Color(0xFF705841),
        cardBorderColor = Color(0xFFD8BE98),
        stitchingColor = GoldAntique,
        metallicAccent = GoldAntique
    )

    val IVORY_WHITE_LEATHER = ArtisanThemeConfig(
        id = "ivory_white_leather",
        name = "Ivory White Leather",
        hardwareName = "Champagne Gold",
        isClassic = false,
        primaryColor = LeatherIvoryInk,
        primaryContainer = Color(0xFFF2ECE1),
        onPrimaryContainer = Color(0xFF2D2218),
        surfaceColor = Color(0xFFFFFFFF),
        surfaceElevatedColor = Color(0xFFFAF6EE),
        onSurfaceColor = Color(0xFF2B2118),
        backgroundColor = Color(0xFFF5F1E8),
        onBackgroundColor = Color(0xFF2B2118),
        secondaryTextColor = Color(0xFF6C5D4F),
        cardBorderColor = Color(0xFFD6C6AE),
        stitchingColor = GoldChampagne,
        metallicAccent = GoldChampagne
    )

    val ALL_THEMES = listOf(
        CLASSIC_SADDLE_TAN,
        ESPRESSO_BROWN,
        OXBLOOD_BURGUNDY,
        MIDNIGHT_BLACK,
        BRITISH_RACING_GREEN,
        COGNAC_AMBER,
        NAVY_BLUE,
        VINTAGE_SLATE_GRAY,
        WARM_CARAMEL,
        IVORY_WHITE_LEATHER
    )

    fun getThemeById(id: String): ArtisanThemeConfig {
        return when (id) {
            "vintage_saddle" -> CLASSIC_SADDLE_TAN
            "brown_leather", "espresso" -> ESPRESSO_BROWN
            "burgundy", "rosewood" -> OXBLOOD_BURGUNDY
            "obsidian" -> MIDNIGHT_BLACK
            "forest_green" -> BRITISH_RACING_GREEN
            "deep_blue" -> NAVY_BLUE
            "caramel", "sahara_gold" -> WARM_CARAMEL
            "white_leather" -> IVORY_WHITE_LEATHER
            else -> ALL_THEMES.firstOrNull { it.id == id } ?: ESPRESSO_BROWN
        }
    }

    fun toColorScheme(config: ArtisanThemeConfig): ColorScheme {
        return if (config.isDark) {
            darkColorScheme(
                primary = config.metallicAccent,
                onPrimary = Color(0xFF131519),
                primaryContainer = config.primaryContainer,
                onPrimaryContainer = config.onPrimaryContainer,
                secondary = config.stitchingColor,
                onSecondary = Color(0xFF131519),
                secondaryContainer = config.primaryContainer,
                onSecondaryContainer = config.onPrimaryContainer,
                tertiary = config.metallicAccent,
                background = config.backgroundColor,
                onBackground = config.onBackgroundColor,
                surface = config.surfaceColor,
                onSurface = config.onSurfaceColor,
                surfaceVariant = config.surfaceElevatedColor,
                onSurfaceVariant = config.secondaryTextColor,
                outline = config.cardBorderColor,
                outlineVariant = config.cardBorderColor.copy(alpha = 0.6f)
            )
        } else {
            lightColorScheme(
                primary = config.primaryColor,
                onPrimary = config.onPrimaryColor,
                primaryContainer = config.primaryContainer,
                onPrimaryContainer = config.onPrimaryContainer,
                secondary = config.stitchingColor,
                onSecondary = Color.White,
                secondaryContainer = config.primaryContainer,
                onSecondaryContainer = config.onPrimaryContainer,
                tertiary = config.metallicAccent,
                background = config.backgroundColor,
                onBackground = config.onBackgroundColor,
                surface = config.surfaceColor,
                onSurface = config.onSurfaceColor,
                surfaceVariant = config.surfaceElevatedColor,
                onSurfaceVariant = config.secondaryTextColor,
                outline = config.cardBorderColor,
                outlineVariant = config.cardBorderColor.copy(alpha = 0.6f)
            )
        }
    }
}

val LocalArtisanTheme = staticCompositionLocalOf { ArtisanThemes.ESPRESSO_BROWN }
