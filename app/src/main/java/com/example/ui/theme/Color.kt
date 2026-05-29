package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Professional Polish - Color Palette Definitions
val ProfPrimary = Color(0xFF0061A4)          // Corporate Deep Royal Blue
val ProfPrimaryContainer = Color(0xFFD1E4FF) // Sophisticated Ice Blue accent container
val ProfOnPrimaryContainer = Color(0xFF001D36)// Deep Navy text
val ProfSecondary = Color(0xFF535F70)        // Muted Slate Blue
val ProfTertiary = Color(0xFF41484D)         // Cool Gray

val ProfBackgroundLight = Color(0xFFFBFCFF)   // Clean modern slate light background
val ProfOnBackgroundLight = Color(0xFF191C1E) // High contrast dark text
val ProfSurfaceLight = Color(0xFFFBFCFF)
val ProfSurfaceVariantLight = Color(0xFFF3F4F9)// Soft footer/surface background
val ProfOnSurfaceVariantLight = Color(0xFF41484D)
val ProfOutlineLight = Color(0xFFC1C7CE)

// Dark Theme Variants
val ProfBackgroundDark = Color(0xFF101214)    // Heavy elegant dark pitch ground
val ProfOnBackgroundDark = Color(0xFFE2E2E6)  // Bright crisp readable text
val ProfSurfaceDark = Color(0xFF1A1C1E)       // Premium dark surface card
val ProfSurfaceVariantDark = Color(0xFF232528)// Muted deep grey
val ProfOnSurfaceDark = Color(0xFFE2E2E6)

// Compatibility Layer - Map the existing names directly to make update seamless
val TealPrimary = ProfPrimary
val TealSecondary = ProfSecondary
val TealAccent = ProfPrimaryContainer

val DarkBackground = ProfBackgroundDark
val DarkSurface = ProfSurfaceDark
val DarkSurfaceVariant = ProfSurfaceVariantDark
val DarkOnSurface = ProfOnSurfaceDark

// Legacy values compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
