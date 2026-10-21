package com.example.solesense.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlack,
    onPrimary = PrimaryWhite,
    background = PrimaryWhite,
    surface = PrimaryWhite,
    onBackground = PrimaryBlack,
    onSurface = PrimaryBlack
)

@Composable
fun SoleSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Có thể mở rộng dark mode sau
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Hiện tại fix cứng giao diện sáng theo UI

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    // Truyền Color và Typography vào MaterialTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}