package com.example.dnd_nfc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // <--- ESTE IMPORT FALTABA
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definimos el esquema de color oscuro
private val DarkColorScheme = darkColorScheme(
    primary = DndRed,
    onPrimary = Color.White,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    background = DragonBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun DnD_NFCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // FORZAMOS SIEMPRE EL MODO OSCURO
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()

            // Forzamos iconos claros en la barra de estado (porque el fondo es oscuro)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}