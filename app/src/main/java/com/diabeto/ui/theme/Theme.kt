package com.diabeto.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * CompositionLocal global pour savoir si le thème actif est sombre.
 * Respecte le choix utilisateur (Clair / Sombre / Système) contrairement
 * à isSystemInDarkTheme() qui ne lit que le réglage Android.
 *
 * Usage dans n'importe quel Composable :
 *   val isDark = LocalIsDarkTheme.current
 */
val LocalIsDarkTheme = compositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnPrimary,
    errorContainer = Error.copy(alpha = 0.12f),
    onErrorContainer = Error,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = OnBackground.copy(alpha = 0.4f)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9D91FF),
    onPrimary = Color(0xFF1A0066),
    primaryContainer = Color(0xFF3D2FCC),
    onPrimaryContainer = Color(0xFFE8E5FF),
    secondary = Color(0xFFFFB3C6),
    onSecondary = Color(0xFF5C0024),
    secondaryContainer = Color(0xFF8A0038),
    onSecondaryContainer = Color(0xFFFFE0E8),
    tertiary = Color(0xFF66E3CE),
    onTertiary = Color(0xFF004D3D),
    tertiaryContainer = Color(0xFF006B5A),
    onTertiaryContainer = Color(0xFFCCF5EC),
    error = Color(0xFFFF8A95),
    onError = Color(0xFF5C0011),
    errorContainer = Color(0xFF8A001A),
    onErrorContainer = Color(0xFFFFE0E4),
    background = Color(0xFF0D0D1A),
    onBackground = Color(0xFFE8E5FF),
    surface = Color(0xFF141428),
    onSurface = Color(0xFFE8E5FF),
    surfaceVariant = Color(0xFF2A2940),
    onSurfaceVariant = Color(0xFFB8B5C8),
    outline = Color(0xFF4A4860),
    outlineVariant = Color(0xFF2A2940),
    scrim = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun DiabetoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Désactivé pour garder notre palette custom
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barre de statut transparente pour un look immersif
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
