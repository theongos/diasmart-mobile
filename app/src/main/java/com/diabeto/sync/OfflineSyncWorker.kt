package com.diabeto.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.diabeto.data.dao.PendingOperationDao
import com.diabeto.data.entity.PendingOperationEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "OfflineSyncWorker"

/**
 * Worker that retries failed network operations from the pending queue.
 * Scheduled when connectivity is restored or periodically as fallback.
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "User not authenticated — skipping offline sync")
            return Result.success()
        }

        val pending = pendingOperationDao.getPending(50)
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending operations")
            return Result.success()
        }

        Log.d(TAG, "Processing ${pending.size} pending operations...")
        val db = FirebaseFirestore.getInstance()
        var successCount = 0
        var failCount = 0

        for (op in pending) {
            try {
                pendingOperationDao.updateStatus(op.id, PendingOperationEntity.STATUS_IN_PROGRESS)

                when (op.operationType) {
                    "BACKUP_DOC" -> {
                        val data = jsonToMap(op.payload)
                        db.collection(op.collection).document(op.documentId).set(data).await()
                    }
                    "DELETE_DOC" -> {
                        db.collection(op.collection).document(op.documentId).delete().await()
                    }
                }

                pendingOperationDao.delete(op.id)
                successCount++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to process op ${op.id}: ${e.message}")
                if (op.retryCount + 1 >= op.maxRetries) {
                    pendingOperationDao.updateStatus(op.id, PendingOperationEntity.STATUS_FAILED)
                } else {
                    pendingOperationDao.updateStatus(op.id, PendingOperationEntity.STATUS_PENDING)
                }
                failCount++
            }
        }

        Log.d(TAG, "Offline sync done: $successCount success, $failCount failed")
        pendingOperationDao.purgeExhausted()

        return if (failCount > 0 && runAttemptCount < 3) Result.retry() else Result.success()
    }

    private fun jsonToMap(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val obj = JSONObject(json)
        for (key in obj.keys()) {
            result[key] = obj.get(key)
        }
        return result
    }

    companion object {
        private const val WORK_NAME = "offline_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Offline sync worker scheduled")
        }
    }
}
