package com.snowify.app.ui.theme
import androidx.compose.ui.graphics.Color
// Dark (default)
val Dark_BgBase = Color(0xFF0A0A0A)
val Dark_BgSurface = Color(0xFF121212)
val Dark_BgElevated = Color(0xFF1A1A1A)
val Dark_BgHighlight = Color(0xFF262626)
val Dark_BgCard = Color(0xFF161616)
val Dark_TextPrimary = Color(0xFFFFFFFF)
val Dark_TextSecondary = Color(0xFFB3B3B3)
val Dark_TextSubdued = Color(0xFF6A6A6A)
val Dark_Accent = Color(0xFFAA55E6)
val Dark_AccentHover = Color(0xFFC07EF0)
val Dark_AccentDim = Color(0x1FAA55E6)
val Dark_AccentGlow = Color(0x40AA55E6)
val Dark_Red = Color(0xFFF06070)
// Light
val Light_BgBase = Color(0xFFF0F0F0)
val Light_BgSurface = Color(0xFFFAFAFA)
val Light_BgElevated = Color(0xFFFFFFFF)
val Light_BgHighlight = Color(0xFFE4E4E4)
val Light_BgCard = Color(0xFFFFFFFF)
val Light_TextPrimary = Color(0xFF1A1A1A)
val Light_TextSecondary = Color(0xFF555555)
val Light_TextSubdued = Color(0xFF999999)
val Light_Accent = Color(0xFF8B3DC7)
val Light_AccentHover = Color(0xFFA050DD)
// Ocean
val Ocean_BgBase = Color(0xFF0B0E14)
val Ocean_Accent = Color(0xFF5B8DEE)
val Ocean_TextPrimary = Color(0xFFEAF0FF)
val Ocean_TextSecondary = Color(0xFFA0AEC0)
// Forest
val Forest_BgBase = Color(0xFF0A120E)
val Forest_Accent = Color(0xFF48BB78)
val Forest_TextPrimary = Color(0xFFE6F5EC)
val Forest_TextSecondary = Color(0xFF8FBFA5)
// Sunset
val Sunset_BgBase = Color(0xFF12090E)
val Sunset_Accent = Color(0xFFED6450)
val Sunset_TextPrimary = Color(0xFFFDE8E4)
val Sunset_TextSecondary = Color(0xFFC09A94)
// Rose
val Rose_BgBase = Color(0xFF100A10)
val Rose_Accent = Color(0xFFDB7093)
val Rose_TextPrimary = Color(0xFFF5E6EE)
val Rose_TextSecondary = Color(0xFFB898AA)
// Midnight
val Midnight_BgBase = Color(0xFF08080F)
val Midnight_Accent = Color(0xFF7B7BDA)
val Midnight_TextPrimary = Color(0xFFE4E4F5)
val Midnight_TextSecondary = Color(0xFF9898B8)
data class SnowifyColors(
    val bgBase: Color,
    val bgSurface: Color,
    val bgElevated: Color,
    val bgHighlight: Color,
    val bgCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSubdued: Color,
    val accent: Color,
    val accentHover: Color,
    val accentDim: Color,
    val accentGlow: Color,
    val red: Color,
    val isDark: Boolean,
)
val DarkColors = SnowifyColors(
    bgBase = Dark_BgBase,
    bgSurface = Dark_BgSurface,
    bgElevated = Dark_BgElevated,
    bgHighlight = Dark_BgHighlight,
    bgCard = Dark_BgCard,
    textPrimary = Dark_TextPrimary,
    textSecondary = Dark_TextSecondary,
    textSubdued = Dark_TextSubdued,
    accent = Dark_Accent,
    accentHover = Dark_AccentHover,
    accentDim = Dark_AccentDim,
    accentGlow = Dark_AccentGlow,
    red = Dark_Red,
    isDark = true,
)
val LightColors = SnowifyColors(
    bgBase = Light_BgBase,
    bgSurface = Light_BgSurface,
    bgElevated = Light_BgElevated,
    bgHighlight = Light_BgHighlight,
    bgCard = Light_BgCard,
    textPrimary = Light_TextPrimary,
    textSecondary = Light_TextSecondary,
    textSubdued = Color(0xFF999999),
    accent = Light_Accent,
    accentHover = Light_AccentHover,
    accentDim = Color(0x208B3DC7),
    accentGlow = Color(0x408B3DC7),
    red = Color(0xFFE05060),
    isDark = false,
)
fun oceanColors() = DarkColors.copy(
    bgBase = Ocean_BgBase,
    bgSurface = Color(0xFF101520),
    bgElevated = Color(0xFF151C28),
    bgHighlight = Color(0xFF1E2A3C),
    bgCard = Color(0xFF131A24),
    textPrimary = Ocean_TextPrimary,
    textSecondary = Ocean_TextSecondary,
    accent = Ocean_Accent,
    accentHover = Color(0xFF7BA8F4),
    accentDim = Color(0x1F5B8DEE),
    accentGlow = Color(0x405B8DEE),
)
fun forestColors() = DarkColors.copy(
    bgBase = Forest_BgBase,
    bgSurface = Color(0xFF0F1A13),
    bgElevated = Color(0xFF142419),
    bgHighlight = Color(0xFF1A3020),
    bgCard = Color(0xFF111E15),
    textPrimary = Forest_TextPrimary,
    textSecondary = Forest_TextSecondary,
    accent = Forest_Accent,
    accentHover = Color(0xFF68D391),
    accentDim = Color(0x1F48BB78),
    accentGlow = Color(0x4048BB78),
)
fun sunsetColors() = DarkColors.copy(
    bgBase = Sunset_BgBase,
    bgSurface = Color(0xFF1A0F14),
    bgElevated = Color(0xFF231419),
    bgHighlight = Color(0xFF2E1B20),
    bgCard = Color(0xFF1E1217),
    textPrimary = Sunset_TextPrimary,
    textSecondary = Sunset_TextSecondary,
    accent = Sunset_Accent,
    accentHover = Color(0xFFF08070),
    accentDim = Color(0x1FED6450),
    accentGlow = Color(0x40ED6450),
)
fun roseColors() = DarkColors.copy(
    bgBase = Rose_BgBase,
    bgSurface = Color(0xFF180E18),
    bgElevated = Color(0xFF201420),
    bgHighlight = Color(0xFF2A1A2A),
    bgCard = Color(0xFF1C121C),
    textPrimary = Rose_TextPrimary,
    textSecondary = Rose_TextSecondary,
    accent = Rose_Accent,
    accentHover = Color(0xFFE890AA),
    accentDim = Color(0x1FDB7093),
    accentGlow = Color(0x40DB7093),
)
fun midnightColors() = DarkColors.copy(
    bgBase = Midnight_BgBase,
    bgSurface = Color(0xFF0C0C14),
    bgElevated = Color(0xFF12121C),
    bgHighlight = Color(0xFF1A1A28),
    bgCard = Color(0xFF0E0E18),
    textPrimary = Midnight_TextPrimary,
    textSecondary = Midnight_TextSecondary,
    accent = Midnight_Accent,
    accentHover = Color(0xFF9898E8),
    accentDim = Color(0x1F7B7BDA),
    accentGlow = Color(0x407B7BDA),
)
