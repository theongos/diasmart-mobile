package com.diabeto.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache local des réponses IA fréquentes.
 * Évite de rappeler Gemini pour des questions déjà posées.
 * TTL de 24h par défaut pour les réponses génériques,
 * pas de cache pour les données patient-spécifiques.
 */
@Entity(tableName = "ai_cache")
data class AiCacheEntity(
    @PrimaryKey
    val queryHash: String,          // SHA-256 du prompt normalisé
    val query: String,              // Question originale (pour debug)
    val response: String,           // Réponse IA complète
    val category: String,           // "general", "nutrition", "definition"
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // 24h
    val hitCount: Int = 0           // Nombre d'utilisations
)
