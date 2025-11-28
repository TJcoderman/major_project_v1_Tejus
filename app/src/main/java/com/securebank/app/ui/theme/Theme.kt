package com.securebank.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * ============================================
 * SECUREBANK THEME - DISTINCTIVE BANKING AESTHETIC
 * ============================================
 * A sophisticated dark theme with emerald accents
 * inspired by secure, professional banking interfaces.
 */

// Color Palette - Dark Obsidian with Emerald Accents
val Obsidian = Color(0xFF0D1117)
val ObsidianLight = Color(0xFF161B22)
val ObsidianSurface = Color(0xFF21262D)
val ObsidianBorder = Color(0xFF30363D)

val Emerald = Color(0xFF2EA043)
val EmeraldBright = Color(0xFF3FB950)
val EmeraldDark = Color(0xFF238636)
val EmeraldMuted = Color(0xFF1B4721)

val Gold = Color(0xFFD4A72C)
val GoldMuted = Color(0xFFBB8009)

val Coral = Color(0xFFF85149)
val CoralMuted = Color(0xFFDA3633)

val CloudWhite = Color(0xFFF0F6FC)
val CloudGray = Color(0xFFC9D1D9)
val MutedGray = Color(0xFF8B949E)
val DimGray = Color(0xFF484F58)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldBright,
    
    secondary = Gold,
    onSecondary = Obsidian,
    secondaryContainer = GoldMuted,
    onSecondaryContainer = Gold,
    
    tertiary = CloudGray,
    onTertiary = Obsidian,
    
    background = Obsidian,
    onBackground = CloudWhite,
    
    surface = ObsidianLight,
    onSurface = CloudWhite,
    surfaceVariant = ObsidianSurface,
    onSurfaceVariant = CloudGray,
    
    error = Coral,
    onError = Color.White,
    errorContainer = CoralMuted,
    onErrorContainer = Color.White,
    
    outline = ObsidianBorder,
    outlineVariant = DimGray,
    
    inverseSurface = CloudWhite,
    inverseOnSurface = Obsidian,
    inversePrimary = EmeraldDark
)

// Light Theme Colors (for accessibility)
private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F5C4),
    onPrimaryContainer = EmeraldDark,
    
    secondary = GoldMuted,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = GoldMuted,
    
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    
    error = CoralMuted,
    onError = Color.White,
    
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

// Shapes
val SecureBankShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun SecureBankTheme(
    darkTheme: Boolean = true, // Default to dark theme for banking aesthetic
    dynamicColor: Boolean = false, // Keep our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = SecureBankShapes,
        content = content
    )
}

