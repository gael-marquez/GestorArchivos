package com.example.gestorarchivos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Colores Tema Guinda (IPN)
private val GuindaLight = lightColorScheme(
    primary = Color(0xFF6D1F3C), // Guinda oscuro
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9B2D4F),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF8E2442),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF3E0014),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color.White,
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF504447),
    outline = Color(0xFF827376),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val GuindaDark = darkColorScheme(
    primary = Color(0xFFFFB1C2),
    onPrimary = Color(0xFF5E0020),
    primaryContainer = Color(0xFF7D1D3A),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE4BCC3),
    onSecondary = Color(0xFF5E0020),
    secondaryContainer = Color(0xFF6F323D),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFE6C08D),
    onTertiary = Color(0xFF42290C),
    background = Color(0xFF201A1A),
    onBackground = Color(0xFFECE0DF),
    surface = Color(0xFF201A1A),
    onSurface = Color(0xFFECE0DF),
    surfaceVariant = Color(0xFF504447),
    onSurfaceVariant = Color(0xFFD5C2C5),
    outline = Color(0xFF9E8C8F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// Colores Tema Azul (ESCOM)
private val AzulLight = lightColorScheme(
    primary = Color(0xFF0D47A1), // Azul ESCOM
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF1565C0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E4FF),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF006874),
    onTertiary = Color.White,
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val AzulDark = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFCEE5FF),
    secondary = Color(0xFF9DCAFF),
    onSecondary = Color(0xFF003257),
    secondaryContainer = Color(0xFF00497C),
    onSecondaryContainer = Color(0xFFD0E4FF),
    tertiary = Color(0xFF4FD8EB),
    onTertiary = Color(0xFF00363D),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

enum class AppTheme {
    GUINDA, // IPN
    AZUL    // ESCOM
}

@Composable
fun GestorArchivosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.GUINDA,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.GUINDA -> if (darkTheme) GuindaDark else GuindaLight
        AppTheme.AZUL -> if (darkTheme) AzulDark else AzulLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}