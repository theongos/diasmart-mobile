package com.diabeto.data.repository

import android.util.Log
import com.diabeto.data.model.ChatbotMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatHistoryRepo"

/**
 * Data class pour une session de conversation ROLLY
 */
data class ChatSession(
    val id: String = "",
    val title: String = "Nouvelle discussion",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val lastPreview: String = ""
) {
    fun toMap() = mapOf(
        "title" to title,
        "createdAt" to createdAt,
        "lastMessageAt" to lastMessageAt,
        "messageCount" to messageCount,
        "lastPreview" to lastPreview
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ChatSession {
            return ChatSession(
                id = id,
                title = map["title"] as? String ?: "Discussion",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                lastMessageAt = (map["lastMessageAt"] as? Number)?.toLong() ?: 0L,
                messageCount = (map["messageCount"] as? Number)?.toInt() ?: 0,
                lastPreview = map["lastPreview"] as? String ?: ""
            )
        }
    }
}

/**
 * Persistance de l'historique des conversations ROLLY dans Firestore.
 *
 * Structure multi-sessions :
 *   /rolly_history/{uid}/conversations/{convId}
 *     - title, createdAt, lastMessageAt, messageCount, lastPreview
 *   /rolly_history/{uid}/conversations/{convId}/messages/{msgId}
 *     - contenu, estUtilisateur, timestamp
 */
@Singleton
class ChatHistoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val COL_HISTORY = "rolly_history"
        private const val SUB_CONVERSATIONS = "conversations"
        private const val SUB_MESSAGES = "messages"
        private const val MAX_CONTEXT_MESSAGES = 30
    }

    private val uid: String?
        get() = authRepository.currentUserId

    var currentSessionId: String? = null
        private set

    /**
     * Assure que le document parent rolly_history/{uid} existe
     */
    private suspend fun ensureParentDocExists() {
        val userId = uid ?: return
        try {
            val parentRef = firestore.collection(COL_HISTORY).document(userId)
            val doc = parentRef.get().await()
            if (!doc.exists()) {
                parentRef.set(mapOf(
                    "createdAt" to System.currentTimeMillis(),
                    "userId" to userId
                )).await()
                Log.d(TAG, "Document parent rolly_history/$userId créé")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur création document parent", e)
        }
    }

    /**
     * Charge toutes les sessions de conversation
     */
    suspend fun getAllSessions(): List<ChatSession> {
        val userId = uid ?: return emptyList()
        return try {
            ensureParentDocExists()
            val snap = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                @Suppress("UNCHECKED_CAST")
                doc.data?.let { ChatSession.fromMap(doc.id, it as Map<String, Any?>) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement sessions", e)
            emptyList()
        }
    }

    /**
     * Crée une nouvelle session de conversation
     */
    suspend fun createSession(title: String = "Nouvelle discussion"): String? {
        val userId = uid ?: return null
        return try {
            ensureParentDocExists()
            val session = ChatSession(title = title)
            val ref = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .add(session.toMap())
                .await()
            currentSessionId = ref.id
            ref.id
        } catch (e: Exception) {
            Log.e(TAG, "Erreur création session", e)
            null
        }
    }

    /**
     * Switch vers une session existante
     */
    fun setCurrentSession(sessionId: String) {
        currentSessionId = sessionId
    }

    /**
     * Sauvegarde un message dans la session courante
     */
    suspend fun saveMessage(message: ChatbotMessage) {
        val userId = uid ?: return
        try {
            // Auto-créer session si nécessaire
            if (currentSessionId == null) {
                createSession()
            }
            val sessionId = currentSessionId ?: return

            firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)
                .collection(SUB_MESSAGES)
                .document(message.id)
                .set(message.toMap())
                .await()

            // Mettre à jour les métadonnées de la session
            val sessionRef = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)

            val snap = sessionRef.get().await()
            val count = ((snap.getLong("messageCount") ?: 0L) + 1).toInt()
            val updateData = mutableMapOf<String, Any>(
                "messageCount" to count,
                "lastMessageAt" to System.currentTimeMillis()
            )

            // Auto-titre depuis le premier message utilisateur
            if (message.estUtilisateur && count <= 2) {
                val title = if (message.contenu.length > 50) message.contenu.take(50) + "..." else message.contenu
                updateData["title"] = title
            }
            if (!message.estUtilisateur) {
                val preview = if (message.contenu.length > 80) message.contenu.take(80) + "..." else message.contenu
                updateData["lastPreview"] = preview
            }

            sessionRef.update(updateData).await()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde message", e)
        }
    }

    /**
     * Sauvegarde une paire question/réponse
     */
    suspend fun saveExchange(userMessage: ChatbotMessage, aiResponse: ChatbotMessage) {
        saveMessage(userMessage)
        saveMessage(aiResponse)
    }

    /**
     * Charge les messages d'une session spécifique
     */
    suspend fun getSessionMessages(sessionId: String): List<ChatbotMessage> {
        val userId = uid ?: return emptyList()
        return try {
            val snap = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)
                .collection(SUB_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                @Suppress("UNCHECKED_CAST")
                doc.data?.let { ChatbotMessage.fromMap(doc.id, it as Map<String, Any?>) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement messages session", e)
            emptyList()
        }
    }

    /**
     * Charge les messages de la session courante
     */
    suspend fun getCurrentSessionMessages(): List<ChatbotMessage> {
        val sessionId = currentSessionId ?: return emptyList()
        return getSessionMessages(sessionId)
    }

    /**
     * Charge les N derniers messages de la session courante pour le contexte
     */
    suspend fun getRecentMessages(limit: Int = MAX_CONTEXT_MESSAGES): List<ChatbotMessage> {
        val userId = uid ?: return emptyList()
        val sessionId = currentSessionId ?: return emptyList()
        return try {
            val snap = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)
                .collection(SUB_MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                @Suppress("UNCHECKED_CAST")
                doc.data?.let { ChatbotMessage.fromMap(doc.id, it as Map<String, Any?>) }
            }.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement historique", e)
            emptyList()
        }
    }

    /**
     * Construit le contexte historique pour le prompt ROLLY
     */
    suspend fun buildHistoryContext(): String {
        val messages = getRecentMessages()
        if (messages.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("=== HISTORIQUE DES CONVERSATIONS PRÉCÉDENTES ===")
        sb.appendLine("(${messages.size} derniers échanges)")

        messages.forEach { msg ->
            val role = if (msg.estUtilisateur) "Patient" else "ROLLY"
            val time = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                .format(java.util.Date(msg.timestamp))
            val contenu = if (msg.contenu.length > 300) msg.contenu.take(300) + "..." else msg.contenu
            sb.appendLine("[$time] $role : $contenu")
        }

        sb.appendLine("=== FIN HISTORIQUE ===")
        sb.appendLine("Utilise cet historique pour personnaliser tes réponses, suivre l'évolution du patient et éviter de répéter les mêmes conseils.")
        return sb.toString()
    }

    /**
     * Supprime une session et tous ses messages
     */
    suspend fun deleteSession(sessionId: String) {
        val userId = uid ?: return
        try {
            val msgsSnap = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)
                .collection(SUB_MESSAGES)
                .get()
                .await()

            val batch = firestore.batch()
            msgsSnap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)
                .delete()
                .await()

            if (currentSessionId == sessionId) {
                currentSessionId = null
            }
            Log.d(TAG, "Session $sessionId supprimée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur suppression session", e)
        }
    }

    /**
     * Efface tout l'historique (toutes les sessions)
     */
    suspend fun clearHistory() {
        val userId = uid ?: return
        try {
            val sessions = getAllSessions()
            for (session in sessions) {
                deleteSession(session.id)
            }
            // Also clean up old flat messages if any
            val oldSnap = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection("messages")
                .get()
                .await()
            if (oldSnap.size() > 0) {
                val batch = firestore.batch()
                oldSnap.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            currentSessionId = null
            Log.d(TAG, "Tout l'historique effacé")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur suppression historique complet", e)
        }
    }

    /**
     * Migre les anciens messages flat vers une session
     */
    suspend fun migrateOldMessages(): Boolean {
        val userId = uid ?: return false
        return try {
            val oldSnap = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            if (oldSnap.size() == 0) return false

            val sessionRef = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .add(mapOf(
                    "title" to "Discussion précédente",
                    "createdAt" to System.currentTimeMillis(),
                    "lastMessageAt" to System.currentTimeMillis(),
                    "messageCount" to oldSnap.size(),
                    "lastPreview" to "Historique migré"
                ))
                .await()

            for (doc in oldSnap.documents) {
                val data = doc.data ?: continue
                sessionRef.collection(SUB_MESSAGES).add(data).await()
            }

            currentSessionId = sessionRef.id
            Log.d(TAG, "Migration: ${oldSnap.size()} messages vers session ${sessionRef.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur migration", e)
            false
        }
    }

    /**
     * Nombre total de messages dans la session courante
     */
    suspend fun getMessageCount(): Long {
        val userId = uid ?: return 0
        val sessionId = currentSessionId ?: return 0
        return try {
            val doc = firestore.collection(COL_HISTORY)
                .document(userId)
                .collection(SUB_CONVERSATIONS)
                .document(sessionId)
                .get()
                .await()
            doc.getLong("messageCount") ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
