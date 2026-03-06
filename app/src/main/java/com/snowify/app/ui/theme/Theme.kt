package com.snowify.app.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.snowify.app.util.AppTheme
val LocalSnowifyColors = staticCompositionLocalOf { DarkColors }
val LocalSnowifyAnimations = staticCompositionLocalOf { true }
@Composable
fun SnowifyTheme(
    appTheme: AppTheme = AppTheme.DARK,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val snowifyColors = when (appTheme) {
        AppTheme.DARK -> DarkColors
        AppTheme.LIGHT -> LightColors
        AppTheme.OCEAN -> oceanColors()
        AppTheme.FOREST -> forestColors()
        AppTheme.SUNSET -> sunsetColors()
        AppTheme.ROSE -> roseColors()
        AppTheme.MIDNIGHT -> midnightColors()
    }
    val materialColorScheme = if (snowifyColors.isDark) {
        darkColorScheme(
            primary = snowifyColors.accent,
            onPrimary = Color.White,
            secondary = snowifyColors.accentHover,
            background = snowifyColors.bgBase,
            surface = snowifyColors.bgSurface,
            onBackground = snowifyColors.textPrimary,
            onSurface = snowifyColors.textPrimary,
            surfaceVariant = snowifyColors.bgElevated,
            onSurfaceVariant = snowifyColors.textSecondary,
            error = snowifyColors.red,
        )
    } else {
        lightColorScheme(
            primary = snowifyColors.accent,
            onPrimary = Color.White,
            secondary = snowifyColors.accentHover,
            background = snowifyColors.bgBase,
            surface = snowifyColors.bgSurface,
            onBackground = snowifyColors.textPrimary,
            onSurface = snowifyColors.textPrimary,
            surfaceVariant = snowifyColors.bgElevated,
            onSurfaceVariant = snowifyColors.textSecondary,
            error = snowifyColors.red,
        )
    }
    CompositionLocalProvider(
        LocalSnowifyColors provides snowifyColors,
        LocalSnowifyAnimations provides animationsEnabled,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = SnowifyTypography,
            shapes = SnowifyShapes,
            content = content,
        )
    }
}
object SnowifyTheme {
    val colors: SnowifyColors
        @Composable @ReadOnlyComposable get() = LocalSnowifyColors.current
    val animationsEnabled: Boolean
        @Composable @ReadOnlyComposable get() = LocalSnowifyAnimations.current
}
