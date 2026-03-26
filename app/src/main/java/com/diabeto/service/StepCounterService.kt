package com.diabeto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.diabeto.MainActivity
import com.diabeto.R

/**
 * Foreground Service pour compter les pas en arrière-plan.
 * Utilise le capteur TYPE_STEP_COUNTER du système (hardware).
 * Persiste les données via SharedPreferences.
 */
class StepCounterService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "step_counter_channel"
        const val NOTIFICATION_ID = 2001
        const val TAG = "StepCounterService"
        const val PREFS_NAME = "step_counter_prefs"
        const val KEY_INITIAL_STEPS = "initial_steps"
        const val KEY_SESSION_STEPS = "session_steps"
        const val KEY_IS_TRACKING = "is_tracking"
        const val KEY_DAILY_STEPS = "daily_steps"
        const val KEY_LAST_DATE = "last_date"

        const val ACTION_START = "com.diabeto.service.START_TRACKING"
        const val ACTION_STOP = "com.diabeto.service.STOP_TRACKING"

        fun isTracking(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_IS_TRACKING, false)
        }

        fun getSessionSteps(context: Context): Int {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_SESSION_STEPS, 0)
        }

        fun getDailySteps(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
            return if (lastDate == today) prefs.getInt(KEY_DAILY_STEPS, 0) else 0
        }
    }

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps: Int = -1
    private var sessionSteps: Int = 0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification(0))
                startTracking()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (stepSensor == null) {
            Log.e(TAG, "Capteur de pas non disponible")
            stopSelf()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        initialSteps = prefs.getInt(KEY_INITIAL_STEPS, -1)
        sessionSteps = prefs.getInt(KEY_SESSION_STEPS, 0)

        sensorManager.registerListener(
            this,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        prefs.edit().putBoolean(KEY_IS_TRACKING, true).apply()
        Log.d(TAG, "Suivi des pas demarré (initial=$initialSteps, session=$sessionSteps)")
    }

    private fun stopTracking() {
        sensorManager.unregisterListener(this)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_TRACKING, false)
            .apply()
        Log.d(TAG, "Suivi des pas arrêté (total=$sessionSteps)")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsFromSensor = event.values[0].toInt()

        if (initialSteps < 0) {
            initialSteps = totalStepsFromSensor
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_INITIAL_STEPS, initialSteps)
                .apply()
        }

        sessionSteps = totalStepsFromSensor - initialSteps
        val distance = String.format("%.2f", sessionSteps * 0.00075)
        val calories = String.format("%.0f", sessionSteps * 0.04)

        // Save to prefs
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val dailySteps = if (lastDate == today) {
            prefs.getInt(KEY_DAILY_STEPS, 0).coerceAtLeast(sessionSteps)
        } else {
            sessionSteps
        }

        prefs.edit()
            .putInt(KEY_SESSION_STEPS, sessionSteps)
            .putInt(KEY_DAILY_STEPS, dailySteps)
            .putString(KEY_LAST_DATE, today)
            .apply()

        // Update notification
        val notification = createNotification(sessionSteps, distance, calories)
        val notifManager = getSystemService(NotificationManager::class.java)
        notifManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_TRACKING, false)
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Podomètre DiaSmart",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Suivi des pas en arrière-plan"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        steps: Int,
        distance: String = "0.00",
        calories: String = "0"
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StepCounterService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Podomètre actif")
            .setContentText("$steps pas | ${distance} km | ${calories} cal")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_notification, "Arrêter", pendingStop)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
