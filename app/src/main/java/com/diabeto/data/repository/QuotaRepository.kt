package com.diabeto.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "QuotaRepository"
private const val DAILY_LIMIT = 10
private const val COLLECTION = "user_quotas"

data class QuotaStatus(
    val used: Int = 0,
    val limit: Int = DAILY_LIMIT,
    val remaining: Int = DAILY_LIMIT,
    val isExceeded: Boolean = false
)

@Singleton
class QuotaRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun todayKey(): String = LocalDate.now().format(dateFormat)

    suspend fun getQuotaStatus(): QuotaStatus {
        val uid = getUserId() ?: return QuotaStatus()
        val today = todayKey()

        return try {
            val doc = firestore.collection(COLLECTION)
                .document(uid)
                .get()
                .await()

            if (doc.exists()) {
                val date = doc.getString("date") ?: ""
                val count = doc.getLong("count")?.toInt() ?: 0

                if (date == today) {
                    val remaining = (DAILY_LIMIT - count).coerceAtLeast(0)
                    QuotaStatus(
                        used = count,
                        limit = DAILY_LIMIT,
                        remaining = remaining,
                        isExceeded = count >= DAILY_LIMIT
                    )
                } else {
                    // Nouveau jour, reset
                    QuotaStatus()
                }
            } else {
                QuotaStatus()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur getQuotaStatus", e)
            QuotaStatus()
        }
    }

    suspend fun consumeQuota(): Boolean {
        val uid = getUserId() ?: return false
        val today = todayKey()

        return try {
            val docRef = firestore.collection(COLLECTION).document(uid)
            val doc = docRef.get().await()

            val currentCount = if (doc.exists()) {
                val date = doc.getString("date") ?: ""
                if (date == today) {
                    doc.getLong("count")?.toInt() ?: 0
                } else {
                    0 // Nouveau jour
                }
            } else {
                0
            }

            if (currentCount >= DAILY_LIMIT) {
                Log.w(TAG, "Quota dépassé pour $uid : $currentCount/$DAILY_LIMIT")
                return false
            }

            val newCount = currentCount + 1
            docRef.set(
                mapOf(
                    "date" to today,
                    "count" to newCount,
                    "lastUsed" to System.currentTimeMillis(),
                    "uid" to uid
                )
            ).await()

            Log.d(TAG, "Quota consommé : $newCount/$DAILY_LIMIT")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur consumeQuota", e)
            true // En cas d'erreur réseau, on laisse passer
        }
    }
}
