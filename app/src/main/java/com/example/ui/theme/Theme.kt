package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.AnimationIntensity
import com.example.data.model.ArtworkStyle
import com.example.data.model.LayoutDensity

@Immutable
data class OrionUiConfig(
    val amoledBlack: Boolean = true,
    val accentPreset: AccentColorPreset = AccentColorPreset.ORION_ELECTRIC,
    val dynamicArtworkColors: Boolean = true,
    val animationIntensity: AnimationIntensity = AnimationIntensity.FULL,
    val artworkStyle: ArtworkStyle = ArtworkStyle.ROUNDED,
    val layoutDensity: LayoutDensity = LayoutDensity.SPACIOUS,
    val activeArtworkPrimary: Color? = null,
    val activeArtworkSecondary: Color? = null
)

val LocalOrionUiConfig = staticCompositionLocalOf { OrionUiConfig() }

@Composable
fun OrionMusicTheme(
    uiConfig: OrionUiConfig = OrionUiConfig(),
    content: @Composable () -> Unit
) {
    val targetPrimary = if (uiConfig.dynamicArtworkColors && uiConfig.activeArtworkPrimary != null) {
        uiConfig.activeArtworkPrimary
    } else {
        uiConfig.accentPreset.primary
    }
    val targetSecondary = if (uiConfig.dynamicArtworkColors && uiConfig.activeArtworkSecondary != null) {
        uiConfig.activeArtworkSecondary
    } else {
        uiConfig.accentPreset.secondary
    }

    val animDuration = when (uiConfig.animationIntensity) {
        AnimationIntensity.FULL -> 650
        AnimationIntensity.REDUCED -> 250
        AnimationIntensity.MINIMAL -> 0
    }

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = animDuration),
        label = "primary_color"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = animDuration),
        label = "secondary_color"
    )

    val bg = if (uiConfig.amoledBlack) AmoledBlack else DarkModeBackground
    val surface = if (uiConfig.amoledBlack) ObsidianSurface else Color(0xFF10101A)
    val surfaceVariant = if (uiConfig.amoledBlack) ObsidianSurfaceElevated else Color(0xFF181828)

    val colorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = Color.White,
        primaryContainer = animatedPrimary.copy(alpha = 0.20f),
        onPrimaryContainer = TextPrimary,
        secondary = animatedSecondary,
        onSecondary = Color.White,
        secondaryContainer = animatedSecondary.copy(alpha = 0.20f),
        onSecondaryContainer = TextPrimary,
        tertiary = uiConfig.accentPreset.tertiary,
        onTertiary = Color.Black,
        background = bg,
        onBackground = TextPrimary,
        surface = surface,
        onSurface = TextPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = TextSecondary,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceVariant,
        surfaceContainerHigh = ObsidianSurfaceHighest,
        surfaceContainerHighest = Color(0xFF222238),
        outline = ObsidianOutline,
        outlineVariant = ObsidianOutlineSubtle,
        error = CrimsonPulse,
        onError = Color.White
    )

    CompositionLocalProvider(LocalOrionUiConfig provides uiConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OrionTypography,
            shapes = OrionShapes,
            content = content
        )
    }
}
