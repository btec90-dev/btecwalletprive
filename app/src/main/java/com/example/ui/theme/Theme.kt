package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

val BalatecShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun BalatecWalletTheme(
    themeConfig: ArtisanThemeConfig = ArtisanThemes.ESPRESSO_BROWN,
    content: @Composable () -> Unit
) {
    val colorScheme = ArtisanThemes.toColorScheme(themeConfig)

    CompositionLocalProvider(LocalArtisanTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = BalatecShapes,
            content = content
        )
    }
}
