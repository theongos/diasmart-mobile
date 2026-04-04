package com.diabeto.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Queue des opérations réseau échouées, à retenter quand le réseau revient.
 * Utilisé pour garantir qu'aucune donnée n'est perdue en mode hors-ligne.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String,      // "BACKUP_DOC", "SEND_MESSAGE", etc.
    val collection: String,         // Firestore collection name
    val documentId: String,         // Firestore document ID
    val payload: String,            // JSON-serialized data
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = 0,
    val status: String = STATUS_PENDING // PENDING, IN_PROGRESS, FAILED
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_FAILED = "FAILED"
    }
}
