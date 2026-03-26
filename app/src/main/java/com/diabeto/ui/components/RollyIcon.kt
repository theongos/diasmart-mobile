package com.diabeto.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Couleurs de l'icône ROLLY
private val RollyBgDark = Color(0xFF001A3C)
private val RollyBgMid = Color(0xFF003366)
private val RollyCyan = Color(0xFF00D2FF)
private val RollyTeal = Color(0xFF0B8FAC)
private val RollyGlow = Color(0xFF4DD0E1)
private val RollyWhite = Color(0xFFE0F7FA)

/**
 * Icône ROLLY personnalisée — design médical futuriste
 * Reproduit : croix médicale + ligne ECG + flèche ascendante + halo lumineux
 *
 * @param size Taille de l'icône
 * @param showBackground Si true, dessine le fond arrondi sombre
 * @param tint Couleur dominante (par défaut cyan ROLLY)
 */
@Composable
fun RollyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showBackground: Boolean = true,
    tint: Color = RollyCyan
) {
    if (showBackground) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RollyBgMid, RollyBgDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size * 0.75f)) {
                drawRollySymbol(tint)
            }
        }
    } else {
        Canvas(modifier = modifier.size(size)) {
            drawRollySymbol(tint)
        }
    }
}

/**
 * Petite version inline (pour les barres de nav, boutons, etc.)
 * Sans fond, juste le symbole.
 */
@Composable
fun RollyIconInline(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = RollyCyan
) {
    Canvas(modifier = modifier.size(size)) {
        drawRollySymbol(tint)
    }
}

/**
 * Dessine le symbole ROLLY complet sur le Canvas.
 */
private fun DrawScope.drawRollySymbol(tint: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val glowColor = tint.copy(alpha = 0.3f)
    val brightColor = tint
    val whiteColor = RollyWhite

    // ── 1. Cercle orbital (halo extérieur) ──
    drawCircle(
        color = glowColor,
        radius = w * 0.44f,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.02f)
    )

    // Petits points orbitaux (nœuds de données)
    val orbitalRadius = w * 0.44f
    listOf(45f, 135f, 225f, 315f).forEachIndexed { i, angle ->
        val rad = Math.toRadians(angle.toDouble())
        val dotX = cx + orbitalRadius * kotlin.math.cos(rad).toFloat()
        val dotY = cy + orbitalRadius * kotlin.math.sin(rad).toFloat()
        val dotSize = if (i % 2 == 0) w * 0.025f else w * 0.018f
        drawCircle(
            color = tint.copy(alpha = 0.6f),
            radius = dotSize,
            center = Offset(dotX, dotY)
        )
    }

    // ── 2. Croix médicale (centre) ──
    val crossW = w * 0.12f
    val crossH = h * 0.32f

    // Branche verticale
    drawRoundRect(
        color = whiteColor,
        topLeft = Offset(cx - crossW / 2f, cy - crossH / 2f),
        size = Size(crossW, crossH),
        cornerRadius = CornerRadius(crossW * 0.25f)
    )
    // Branche horizontale
    drawRoundRect(
        color = whiteColor,
        topLeft = Offset(cx - crossH / 2f, cy - crossW / 2f),
        size = Size(crossH, crossW),
        cornerRadius = CornerRadius(crossW * 0.25f)
    )

    // ── 3. Ligne ECG (battement cardiaque) ──
    val ecgPath = Path().apply {
        val startX = cx - w * 0.32f
        val endX = cx + w * 0.32f
        val baseY = cy

        moveTo(startX, baseY)
        lineTo(cx - w * 0.18f, baseY)
        // Premier pic (petit)
        lineTo(cx - w * 0.14f, baseY - h * 0.06f)
        lineTo(cx - w * 0.10f, baseY)
        // Pic QRS (grand — battement cardiaque)
        lineTo(cx - w * 0.06f, baseY + h * 0.08f)
        lineTo(cx, baseY - h * 0.18f)          // pic R haut
        lineTo(cx + w * 0.06f, baseY + h * 0.10f) // descente S
        lineTo(cx + w * 0.10f, baseY)
        // Pic T (récupération)
        lineTo(cx + w * 0.16f, baseY - h * 0.05f)
        lineTo(cx + w * 0.20f, baseY)
        lineTo(endX, baseY)
    }

    // Glow de la ligne ECG
    drawPath(
        path = ecgPath,
        color = glowColor,
        style = Stroke(
            width = w * 0.04f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    // Ligne ECG principale
    drawPath(
        path = ecgPath,
        color = brightColor,
        style = Stroke(
            width = w * 0.025f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // ── 4. Flèche ascendante (tendance/prédiction) ──
    val arrowPath = Path().apply {
        val arrowX = cx + w * 0.22f
        val arrowBottom = cy + h * 0.05f
        val arrowTop = cy - h * 0.28f

        moveTo(arrowX, arrowBottom)
        lineTo(arrowX + w * 0.08f, arrowTop + h * 0.06f)
        // Tête de flèche
        moveTo(arrowX + w * 0.04f, arrowTop)
        lineTo(arrowX + w * 0.08f, arrowTop + h * 0.06f)
        lineTo(arrowX + w * 0.12f, arrowTop + h * 0.02f)
    }

    drawPath(
        path = arrowPath,
        color = Color(0xFF4CAF50),
        style = Stroke(
            width = w * 0.025f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // ── 5. Smartphone/écran stylisé (bas) ──
    val phoneW = w * 0.18f
    val phoneH = h * 0.28f
    val phoneX = cx - phoneW / 2f
    val phoneY = cy + h * 0.05f

    drawRoundRect(
        color = tint.copy(alpha = 0.4f),
        topLeft = Offset(phoneX, phoneY),
        size = Size(phoneW, phoneH),
        cornerRadius = CornerRadius(w * 0.03f),
        style = Stroke(width = w * 0.018f)
    )

    // Mini barres de données dans le phone
    val barW = phoneW * 0.15f
    val barSpacing = phoneW * 0.22f
    listOf(0.4f, 0.65f, 0.5f).forEachIndexed { i, heightRatio ->
        val barH = phoneH * 0.35f * heightRatio
        val barX = phoneX + phoneW * 0.15f + i * barSpacing
        val barY = phoneY + phoneH * 0.7f - barH

        drawRoundRect(
            color = when (i) {
                0 -> tint.copy(alpha = 0.6f)
                1 -> Color(0xFF4CAF50).copy(alpha = 0.6f)
                else -> tint.copy(alpha = 0.5f)
            },
            topLeft = Offset(barX, barY),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(w * 0.01f)
        )
    }

    // ── 6. Petites lignes circuit (tech/IA) ──
    val circuitColor = tint.copy(alpha = 0.2f)
    val circuitStroke = Stroke(width = w * 0.01f, cap = StrokeCap.Round)

    // Lignes horizontales subtiles
    drawLine(circuitColor, Offset(w * 0.05f, h * 0.3f), Offset(w * 0.15f, h * 0.3f), strokeWidth = w * 0.01f)
    drawLine(circuitColor, Offset(w * 0.85f, h * 0.7f), Offset(w * 0.95f, h * 0.7f), strokeWidth = w * 0.01f)
    drawLine(circuitColor, Offset(w * 0.08f, h * 0.75f), Offset(w * 0.15f, h * 0.75f), strokeWidth = w * 0.01f)
    drawLine(circuitColor, Offset(w * 0.82f, h * 0.25f), Offset(w * 0.92f, h * 0.25f), strokeWidth = w * 0.01f)

    // Points terminaux des circuits
    listOf(
        Offset(w * 0.05f, h * 0.3f),
        Offset(w * 0.95f, h * 0.7f),
        Offset(w * 0.08f, h * 0.75f),
        Offset(w * 0.92f, h * 0.25f)
    ).forEach { pt ->
        drawCircle(circuitColor, w * 0.012f, pt)
    }
}
