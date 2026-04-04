package com.diabeto.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cache local des réponses IA fréquentes.
 * Évite de rappeler Gemini pour des questions déjà posées.
 * TTL de 24h par défaut pour les réponses génériques,
 * pas de cache pour les données patient-spécifiques.
 * Intégrité vérifiée par HMAC-SHA256.
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
    val hitCount: Int = 0,          // Nombre d'utilisations
    val hmac: String = ""           // HMAC-SHA256 for integrity verification
) {
    companion object {
        private const val HMAC_ALGO = "HmacSHA256"

        fun computeHmac(queryHash: String, response: String, key: ByteArray): String {
            val mac = Mac.getInstance(HMAC_ALGO)
            mac.init(SecretKeySpec(key, HMAC_ALGO))
            val data = "$queryHash:$response".toByteArray(Charsets.UTF_8)
            return mac.doFinal(data).joinToString("") { "%02x".format(it) }
        }
    }

    fun verifyIntegrity(key: ByteArray): Boolean {
        if (hmac.isEmpty()) return false
        return hmac == computeHmac(queryHash, response, key)
    }
}
