package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core AMOLED & Obsidian Surfaces
val AmoledBlack = Color(0xFF000000)
val DarkModeBackground = Color(0xFF0A0A12)
val ObsidianSurface = Color(0xFF090910)
val ObsidianSurfaceElevated = Color(0xFF12121F)
val ObsidianSurfaceHighest = Color(0xFF1A1A2C)
val ObsidianOutline = Color(0xFF26263D)
val ObsidianOutlineSubtle = Color(0xFF181828)

// Futuristic Neon & Celestial Accents
val ElectricBlue = Color(0xFF2979FF)
val OrionViolet = Color(0xFF7C4DFF)
val NeonMagenta = Color(0xFFE040FB)
val CyberCyan = Color(0xFF00E5FF)
val AuroraEmerald = Color(0xFF00E676)
val SolarAmber = Color(0xFFFFB300)
val CrimsonPulse = Color(0xFFFF4081)

// Typography Neutrals
val TextPrimary = Color(0xFFF5F7FF)
val TextSecondary = Color(0xFFA2A8C2)
val TextMuted = Color(0xFF6B718A)

enum class AccentColorPreset(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
) {
    ORION_ELECTRIC(
        displayName = "Orion Blue",
        primary = ElectricBlue,
        secondary = OrionViolet,
        tertiary = CyberCyan
    ),
    NEBULA_VIOLET(
        displayName = "Nebula Violet",
        primary = OrionViolet,
        secondary = NeonMagenta,
        tertiary = ElectricBlue
    ),
    CYBER_CYAN(
        displayName = "Cyber Cyan",
        primary = CyberCyan,
        secondary = ElectricBlue,
        tertiary = OrionViolet
    ),
    NOVA_MAGENTA(
        displayName = "Nova Magenta",
        primary = NeonMagenta,
        secondary = OrionViolet,
        tertiary = CyberCyan
    )
}
