package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun BalatecWalletTheme(
    themeConfig: ArtisanThemeConfig = ArtisanThemes.BROWN_LEATHER,
    content: @Composable () -> Unit
) {
    val colorScheme = ArtisanThemes.toColorScheme(themeConfig)

    CompositionLocalProvider(LocalArtisanTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
