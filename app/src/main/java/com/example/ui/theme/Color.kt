package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// 1. LIGHT PALETTE (Crisp High-Contrast White/Slate)
// ==========================================
val PosLightPrimary = Color(0xFF1D4ED8)           // Rich Royal Blue (Tailwind 700)
val PosLightOnPrimary = Color(0xFFFFFFFF)
val PosLightPrimaryContainer = Color(0xFFDBEAFE)  // Light crisp blue tint
val PosLightOnPrimaryContainer = Color(0xFF1E3A8A)

val PosLightSecondary = Color(0xFF0F766E)         // Deep Emerald Teal
val PosLightOnSecondary = Color(0xFFFFFFFF)
val PosLightSecondaryContainer = Color(0xFFCCFBF1)
val PosLightOnSecondaryContainer = Color(0xFF115E59)

val PosLightTertiary = Color(0xFFB45309)          // Sharp Amber
val PosLightOnTertiary = Color(0xFFFFFFFF)
val PosLightTertiaryContainer = Color(0xFFFEF3C7)
val PosLightOnTertiaryContainer = Color(0xFF78350F)

val PosLightBackground = Color(0xFFF1F5F9)        // Distinct Slate-100 canvas
val PosLightOnBackground = Color(0xFF090D16)      // Pitch high-contrast text
val PosLightSurface = Color(0xFFFFFFFF)           // Pure White card
val PosLightOnSurface = Color(0xFF090D16)         // Ultra sharp dark text
val PosLightSurfaceVariant = Color(0xFFE2E8F0)    // Slate-200 for inner chips/inputs
val PosLightOnSurfaceVariant = Color(0xFF334155)  // Slate-700 (high-contrast secondary)
val PosLightOutline = Color(0xFF94A3B8)           // Sharp distinct borders
val PosLightOutlineVariant = Color(0xFFCBD5E1)

// ==========================================
// 2. DARK PALETTE (Obsidian Charcoal, Vibrant Glow)
// ==========================================
val PosDarkPrimary = Color(0xFF38BDF8)            // Luminous Sky Blue (Sky 400)
val PosDarkOnPrimary = Color(0xFF03223F)          // Deep Navy text on sky blue
val PosDarkPrimaryContainer = Color(0xFF1E3A8A)   // Indigo container
val PosDarkOnPrimaryContainer = Color(0xFFE0F2FE)

val PosDarkSecondary = Color(0xFF2DD4BF)          // Vibrant Aqua Teal
val PosDarkOnSecondary = Color(0xFF042F2C)
val PosDarkSecondaryContainer = Color(0xFF115E59)
val PosDarkOnSecondaryContainer = Color(0xFFCCFBF1)

val PosDarkTertiary = Color(0xFFFBBF24)           // Electric Amber
val PosDarkOnTertiary = Color(0xFF451A03)
val PosDarkTertiaryContainer = Color(0xFF78350F)
val PosDarkOnTertiaryContainer = Color(0xFFFEF3C7)

val PosDarkBackground = Color(0xFF0B0F19)         // Deep Obsidian Black
val PosDarkOnBackground = Color(0xFFF8FAFC)       // Crisp white text
val PosDarkSurface = Color(0xFF141C2E)            // Elevated deep slate card
val PosDarkOnSurface = Color(0xFFF8FAFC)          // Maximum contrast white
val PosDarkSurfaceVariant = Color(0xFF1E293B)     // Slate-800
val PosDarkOnSurfaceVariant = Color(0xFFCBD5E1)   // Slate-300 (very legible)
val PosDarkOutline = Color(0xFF475569)            // Slate-600 crisp card borders
val PosDarkOutlineVariant = Color(0xFF334155)

// ==========================================
// 3. GREY PALETTE (Industrial Slate / Neutral Grey POS Terminal)
// ==========================================
val PosGreyPrimary = Color(0xFF60A5FA)            // Crisp Bright Blue
val PosGreyOnPrimary = Color(0xFF0F172A)
val PosGreyPrimaryContainer = Color(0xFF374151)   // Sleek Steel Slate
val PosGreyOnPrimaryContainer = Color(0xFFF1F5F9)

val PosGreySecondary = Color(0xFF34D399)          // Sharp Mint Emerald
val PosGreyOnSecondary = Color(0xFF064E3B)
val PosGreySecondaryContainer = Color(0xFF064E3B)
val PosGreyOnSecondaryContainer = Color(0xFFA7F3D0)

val PosGreyTertiary = Color(0xFFF59E0B)           // Vibrant Amber
val PosGreyOnTertiary = Color(0xFF451A03)
val PosGreyTertiaryContainer = Color(0xFF4B3C2B)
val PosGreyOnTertiaryContainer = Color(0xFFFDE68A)

val PosGreyBackground = Color(0xFF1E2126)        // Matte Industrial Grey canvas
val PosGreyOnBackground = Color(0xFFF8FAFC)      // Pure White typography
val PosGreySurface = Color(0xFF2A2E35)           // Elevated Solid Grey card
val PosGreyOnSurface = Color(0xFFFFFFFF)         // Ultra sharp white text
val PosGreySurfaceVariant = Color(0xFF353B44)    // Slate steel inner chip/input
val PosGreyOnSurfaceVariant = Color(0xFFCBD5E1)  // Clean silvery-grey secondary text
val PosGreyOutline = Color(0xFF5A6270)           // Sharp metallic border
val PosGreyOutlineVariant = Color(0xFF3E434D)

// ==========================================
// Status & Alert Colors (Vibrant across all themes)
// ==========================================
val PosSuccess = Color(0xFF10B981)
val PosSuccessContainer = Color(0xFFD1FAE5)
val PosWarning = Color(0xFFF59E0B)
val PosWarningContainer = Color(0xFFFEF3C7)
val PosError = Color(0xFFEF4444)
val PosErrorContainer = Color(0xFFFEE2E2)

// Legacy aliases to prevent compile errors
val PosPrimary = PosLightPrimary
val PosOnPrimary = PosLightOnPrimary
val PosPrimaryContainer = PosLightPrimaryContainer
val PosOnPrimaryContainer = PosLightOnPrimaryContainer
val PosSecondary = PosLightSecondary
val PosOnSecondary = PosLightOnSecondary
val PosSecondaryContainer = PosLightSecondaryContainer
val PosOnSecondaryContainer = PosLightOnSecondaryContainer
val PosTertiary = PosLightTertiary
val PosOnTertiary = PosLightOnTertiary
val PosTertiaryContainer = PosLightTertiaryContainer
val PosOnTertiaryContainer = PosLightOnTertiaryContainer
val PosBackground = PosLightBackground
val PosOnBackground = PosLightOnBackground
val PosSurface = PosLightSurface
val PosOnSurface = PosLightOnSurface
val PosSurfaceVariant = PosLightSurfaceVariant
val PosOnSurfaceVariant = PosLightOnSurfaceVariant
val PosOutline = PosLightOutline
