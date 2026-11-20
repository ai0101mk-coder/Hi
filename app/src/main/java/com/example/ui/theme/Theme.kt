package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.ThemePreference

@Immutable
data class GlassColorTokens(
    val isDark: Boolean,
    val backgroundBase: Color,
    val blobColors: List<Color>,
    val glassFill: Color,
    val glassFillElevated: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val glassShadow: Color,
    val dockFill: Color,
    val activePillFill: Color,
    val dividerColor: Color
)

private val LightGlassTokens = GlassColorTokens(
    isDark = false,
    backgroundBase = LightBackgroundBase,
    blobColors = listOf(LightBlobPurple, LightBlobBlue, LightBlobPink, LightBlobPeach),
    glassFill = LightGlassFill,
    glassFillElevated = LightGlassFillElevated,
    glassBorderStart = LightGlassBorderStart,
    glassBorderEnd = LightGlassBorderEnd,
    glassShadow = Color(0x1F5B4DF0),
    dockFill = Color(0xDDF8F9FE),
    activePillFill = Color(0xFFFFFFFF),
    dividerColor = Color(0x1A111827)
)

private val DarkGlassTokens = GlassColorTokens(
    isDark = true,
    backgroundBase = DarkBackgroundBase,
    blobColors = listOf(DarkBlobPurple, DarkBlobBlue, DarkBlobPink, DarkBlobCyan),
    glassFill = DarkGlassFill,
    glassFillElevated = DarkGlassFillElevated,
    glassBorderStart = DarkGlassBorderStart,
    glassBorderEnd = DarkGlassBorderEnd,
    glassShadow = Color(0x4D000000),
    dockFill = Color(0xCC161829),
    activePillFill = Color(0xFF2A2E4D),
    dividerColor = Color(0x1FFFFFFF)
)

val LocalGlassTokens = staticCompositionLocalOf { LightGlassTokens }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackgroundBase,
    surface = DarkGlassFill,
    surfaceVariant = DarkGlassFillElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackgroundBase,
    surface = LightGlassFill,
    surfaceVariant = LightGlassFillElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun VisualExplorerTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val glassTokens = if (useDark) DarkGlassTokens else LightGlassTokens

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
