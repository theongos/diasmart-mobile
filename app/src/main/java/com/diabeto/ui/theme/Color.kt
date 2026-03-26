package com.diabeto.ui.theme

import androidx.compose.ui.graphics.Color

// Couleurs principales
val Primary = Color(0xFF0B8FAC)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFB3E5FC)
val OnPrimaryContainer = Color(0xFF01579B)

val Secondary = Color(0xFF5C6BC0)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE8EAF6)
val OnSecondaryContainer = Color(0xFF283593)

val Tertiary = Color(0xFF26A69A)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFB2DFDB)
val OnTertiaryContainer = Color(0xFF00695C)

// Couleurs sémantiques pour la glycémie
val GlucoseLow = Color(0xFFE57373)        // Hypoglycémie < 70
val GlucoseNormal = Color(0xFF81C784)     // Normal 70-180
val GlucoseHigh = Color(0xFFFFB74D)       // Hyperglycémie 180-250
val GlucoseSevere = Color(0xFFE53935)     // Sévère > 250 ou < 54

// Couleurs d'état
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Error = Color(0xFFF44336)
val Info = Color(0xFF2196F3)

// Couleurs de fond
val Background = Color(0xFFF5F7FA)
val OnBackground = Color(0xFF2C3E50)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF2C3E50)
val SurfaceVariant = Color(0xFFECEFF1)
val OnSurfaceVariant = Color(0xFF607D8B)

// Couleurs d'outline
val Outline = Color(0xFFB0BEC5)
val OutlineVariant = Color(0xFFECEFF1)

// Couleurs pour les graphiques
val ChartLine = Primary
val ChartPointNormal = GlucoseNormal
val ChartPointLow = GlucoseLow
val ChartPointHigh = GlucoseHigh
val ChartTargetZone = Color(0xFF81C784).copy(alpha = 0.2f)

// Gradient DiaSmart (cyan → teal → dark teal)
val GradientStart = Color(0xFF00D2FF)
val GradientMid = Color(0xFF0B8FAC)
val GradientEnd = Color(0xFF005F73)

// Navigation bar
val NavBarBackground = Color(0xFFFFFFFF)
val NavBarSelected = Color(0xFF0B8FAC)
val NavBarUnselected = Color(0xFF9E9E9E)
