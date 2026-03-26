package com.diabeto.voip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.diabeto.MainActivity


/**
 * Foreground service to keep VoIP call alive when app is in background.
 * Shows persistent notification with call controls.
 */
class CallService : Service() {

    companion object {
        private const val TAG = "CallService"
        private const val CHANNEL_ID = "diasmart_call"
        private const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.diabeto.voip.START_CALL"
        const val ACTION_END = "com.diabeto.voip.END_CALL"
        const val ACTION_HANGUP = "com.diabeto.voip.HANGUP"

        fun start(context: Context, callerName: String, isVideo: Boolean) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_START
                putExtra("callerName", callerName)
                putExtra("isVideo", isVideo)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val name = intent.getStringExtra("callerName") ?: "Appel"
                val isVideo = intent.getBooleanExtra("isVideo", false)
                startForeground(NOTIFICATION_ID, buildNotification(name, isVideo))
            }
            ACTION_HANGUP -> {
                CallManagerProvider.callManager?.endCall()
                stopSelf()
            }
            ACTION_END -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Appels DiaSmart",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications d'appels en cours"
            setSound(null, null)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(callerName: String, isVideo: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangupIntent = PendingIntent.getService(
            this, 1,
            Intent(this, CallService::class.java).apply { action = ACTION_HANGUP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callType = if (isVideo) "video" else "audio"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Appel $callType en cours")
            .setContentText(callerName)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Raccrocher", hangupIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallService destroyed")
    }
}
