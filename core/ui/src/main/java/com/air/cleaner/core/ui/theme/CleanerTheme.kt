package com.air.cleaner.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanerColorScheme = lightColorScheme(
    primary = Color(0xFF176B87),
    onPrimary = Color.White,
    secondary = Color(0xFF4C6F64),
    onSecondary = Color.White,
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF14211F),
    surface = Color.White,
    onSurface = Color(0xFF14211F),
    error = Color(0xFFB42318),
    onError = Color.White,
)

@Composable
fun CleanerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CleanerColorScheme,
        content = content,
    )
}
