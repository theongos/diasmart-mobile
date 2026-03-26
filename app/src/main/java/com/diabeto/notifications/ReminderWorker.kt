package com.diabeto.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.diabeto.data.database.DiabetoDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = DiabetoDatabase.getInstance(applicationContext)
            val medicaments = db.medicamentDao().getAllActiveMedicaments()

            for (med in medicaments) {
                if (!med.rappelActive) continue
                val patient = db.patientDao().getPatientById(med.patientId) ?: continue

                NotificationHelper.showMedicamentReminder(
                    context = applicationContext,
                    medicamentId = med.id,
                    medicamentName = "${med.nom} ${med.dosage}",
                    patientName = patient.nomComplet
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@HiltWorker
class AppointmentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = DiabetoDatabase.getInstance(applicationContext)
            val now = LocalDateTime.now()
            val oneHourLater = now.plusHours(1)

            // Find appointments in the next hour
            val upcomingRdvs = db.rendezVousDao().getUpcomingRendezVousAfter(now, oneHourLater)

            for (rdv in upcomingRdvs) {
                if (rdv.rappelEnvoye) continue
                val patient = db.patientDao().getPatientById(rdv.patientId) ?: continue

                NotificationHelper.showRendezVousReminder(
                    context = applicationContext,
                    rdvId = rdv.id,
                    titre = rdv.titre,
                    patientName = patient.nomComplet,
                    dateHeure = rdv.dateHeure.format(DateTimeFormatter.ofPattern("HH:mm"))
                )
                db.rendezVousDao().markReminderSent(rdv.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@HiltWorker
class GlucoseMeasurementReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = DiabetoDatabase.getInstance(applicationContext)
            val patients = db.patientDao().getAllPatientsList()

            for (patient in patients) {
                // Check if patient has measured today
                val todayReadings = db.glucoseDao().getReadingsCountForDate(
                    patient.id,
                    LocalDate.now()
                )

                if (todayReadings == 0) {
                    NotificationHelper.showMeasurementReminder(
                        context = applicationContext,
                        patientId = patient.id,
                        patientName = patient.nomComplet
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Scheduler for periodic reminders using WorkManager
 */
object ReminderScheduler {

    private const val MEDICATION_WORK = "medication_reminder_work"
    private const val APPOINTMENT_WORK = "appointment_reminder_work"
    private const val MEASUREMENT_WORK = "measurement_reminder_work"

    fun scheduleMedicationReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            8, TimeUnit.HOURS
        )
            .setConstraints(Constraints.Builder().build())
            .addTag(MEDICATION_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MEDICATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleAppointmentReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<AppointmentReminderWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(Constraints.Builder().build())
            .addTag(APPOINTMENT_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            APPOINTMENT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleMeasurementReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<GlucoseMeasurementReminderWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(Constraints.Builder().build())
            .addTag(MEASUREMENT_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MEASUREMENT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(MEDICATION_WORK)
        wm.cancelUniqueWork(APPOINTMENT_WORK)
        wm.cancelUniqueWork(MEASUREMENT_WORK)
    }
}
