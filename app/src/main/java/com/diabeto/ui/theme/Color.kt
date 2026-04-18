package com.diabeto.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════
//  DIASMART — Design System DayLife-inspired
//  Palette: Deep indigo/purple primary, coral accents, warm neutrals
// ══════════════════════════════════════════════════════════════════

// Couleurs principales
val Primary = Color(0xFF6771E4)          // DayLife indigo
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE8E5FF)  // Indigo très clair
val OnPrimaryContainer = Color(0xFF1A0066)

val Secondary = Color(0xFFFF6B8A)         // Coral/rose doux
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFFFE0E8)
val OnSecondaryContainer = Color(0xFF5C0024)

val Tertiary = Color(0xFF00C9A7)          // Menthe/turquoise
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFCCF5EC)
val OnTertiaryContainer = Color(0xFF004D3D)

// Couleurs sémantiques pour la glycémie
val GlucoseLow = Color(0xFFFF6B8A)        // Rose — Hypoglycémie < 70
val GlucoseNormal = Color(0xFF00C9A7)     // Turquoise — Normal 70-180
val GlucoseHigh = Color(0xFFFFB547)       // Ambre doré — Hyperglycémie 180-250
val GlucoseSevere = Color(0xFFFF4757)     // Rouge vif — Sévère > 250 ou < 54

// Couleurs d'état
val Success = Color(0xFF00C9A7)
val Warning = Color(0xFFFFB547)
val Error = Color(0xFFFF4757)
val Info = Color(0xFF6771E4)

// Material-style semantic colors (used across screens)
val StatusGreen = Color(0xFF4CAF50)       // Vert Material — validations, actif, accepté
val StatusOrange = Color(0xFFFF9800)      // Orange Material — en attente, prédictions
val StatusRed = Color(0xFFFF5252)         // Rouge Material — erreur, rejeté, quota
val StatusRedDark = Color(0xFFF44336)     // Rouge foncé — refus, hypoglycémie

// Échelle HbA1c
val HbA1cNormal = Color(0xFF4CAF50)       // < 5.7% — Normal
val HbA1cPreDiabete = Color(0xFFFFC107)   // 5.7-6.4% — Prédiabète
val HbA1cCible = Color(0xFF81C784)        // 6.5-7.0% — Cible
val HbA1cAuDessus = Color(0xFFFFB74D)     // 7.0-8.0% — Au-dessus
val HbA1cRisque = Color(0xFFE57373)       // > 8.0% — Risque
val HbA1cDanger = Color(0xFFD32F2F)       // > 9.0% — Danger

// Couleurs de fond — tons chauds et doux (light mode: softer)
val Background = Color(0xFFF9F8FD)         // Lavande très doux
val OnBackground = Color(0xFF1A1A2E)       // Bleu nuit profond
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1A1A2E)
val SurfaceVariant = Color(0xFFF2F1F7)     // Gris lavande doux
val OnSurfaceVariant = Color(0xFF6E6B7B)   // Gris moyen violet

// Couleurs d'outline
val Outline = Color(0xFFDBD9E8)
val OutlineVariant = Color(0xFFEFEDF7)

// Couleurs pour les graphiques
val ChartLine = Primary
val ChartPointNormal = GlucoseNormal
val ChartPointLow = GlucoseLow
val ChartPointHigh = GlucoseHigh
val ChartTargetZone = GlucoseNormal.copy(alpha = 0.15f)
val ChartGradientTop = Primary.copy(alpha = 0.3f)
val ChartGradientBottom = Primary.copy(alpha = 0.02f)

// Gradients DiaSmart
val GradientStart = Color(0xFF8B93F0)      // Indigo clair
val GradientMid = Color(0xFF6771E4)        // Indigo primaire
val GradientEnd = Color(0xFF4F58C2)         // Indigo profond

// Gradient header doux (purple → bleu)
val HeaderGradientStart = Color(0xFF6771E4)
val HeaderGradientEnd = Color(0xFF4F58C2)

// Gradient accent (coral → rose)
val AccentGradientStart = Color(0xFFFF6B8A)
val AccentGradientEnd = Color(0xFFFF8E72)

// Gradient turquoise
val TealGradientStart = Color(0xFF00C9A7)
val TealGradientEnd = Color(0xFF00B4D8)

// Cartes de fonctionnalités — couleurs pastels très douces (light mode)
val CardGlucose = Color(0xFFECEAFF)        // Lavande très doux
val CardMedication = Color(0xFFFFE8EF)     // Rose pâle doux
val CardAppointment = Color(0xFFD4F7EF)    // Menthe doux
val CardInsulin = Color(0xFFFFF6DE)        // Doré pâle doux
val CardActivity = Color(0xFFDEF2FF)       // Bleu ciel doux
val CardNutrition = Color(0xFFFFF0D6)      // Pêche doux

// Rolly card (dashboard)
val RollyCardColor = Color(0xFF6771E4)

// Cartes de fonctionnalités — couleurs DENSES pour le mode sombre
val CardGlucoseDark = Color(0xFF4A45A8)         // Lavande dense
val CardMedicationDark = Color(0xFFA63050)      // Rose dense
val CardAppointmentDark = Color(0xFF178A6E)     // Menthe dense
val CardInsulinDark = Color(0xFFAD952C)         // Doré dense
val CardActivityDark = Color(0xFF1C6AA6)        // Bleu dense
val CardNutritionDark = Color(0xFFAD6A1C)       // Pêche dense

// Navigation bar
val NavBarBackground = Color(0xFFFFFFFF)
val NavBarSelected = Color(0xFF6771E4)
val NavBarUnselected = Color(0xFFB8B5C8)

// Texte
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6E6B7B)
val TextTertiary = Color(0xFF9D9AAF)

// ══════════════════════════════════════════════════════════════════
//  Dark Mode tokens — reusable across all screens
// ══════════════════════════════════════════════════════════════════
val DarkBackground = Color(0xFF0D0D1A)
val DarkSurface = Color(0xFF141428)
val DarkTextPrimary = Color(0xFFE8E5FF)
// Renforcé pour meilleure lisibilité en thème sombre (avant : #B8B5C8)
val DarkTextSecondary = Color(0xFFD1CEE0)
// Renforcé pour contraste onglets inactifs / labels secondaires (avant : #8A87A8)
val DarkTextTertiary = Color(0xFFB0ADC8)
val DarkNavBar = Color(0xFF141428)
val DarkOutline = Color(0xFF2A2940)
val DarkPrimaryContainer = Color(0xFF3D2FCC)

// Accent colors (reusable across screens)
val AccentCyan = Color(0xFF00E5FF)
val AccentOrange = Color(0xFFFF8E72)
val AccentPurple = Color(0xFF8B5CF6)
val AccentBlue = Color(0xFF3B82F6)
val AccentTeal = Color(0xFF14B8A6)

// Scrim / overlay
val Scrim = Color(0xFF000000)
