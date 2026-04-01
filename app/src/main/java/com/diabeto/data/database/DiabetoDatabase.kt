package com.diabeto.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diabeto.data.dao.*
import com.diabeto.data.entity.*
import net.sqlcipher.database.SupportFactory

/**
 * Base de données Room principale de l'application.
 * Chiffrée avec SQLCipher pour protéger les données médicales sensibles.
 */
@Database(
    entities = [
        PatientEntity::class,
        LectureGlucoseEntity::class,
        MedicamentEntity::class,
        RendezVousEntity::class,
        HbA1cEntity::class,
        JournalEntity::class,
        AiCacheEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DiabetoDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun glucoseDao(): GlucoseDao
    abstract fun medicamentDao(): MedicamentDao
    abstract fun rendezVousDao(): RendezVousDao
    abstract fun hbA1cDao(): HbA1cDao
    abstract fun journalDao(): JournalDao
    abstract fun aiCacheDao(): AiCacheDao

    companion object {
        const val DATABASE_NAME = "diabeto_database.db"
        private const val TAG = "DiabetoDatabase"

        // Passphrase for SQLCipher — derived from app + device identity
        private fun getPassphrase(context: Context): ByteArray {
            val packageName = context.packageName
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "diasmart_default"
            val key = "DSm@rt_${packageName}_$androidId"
            return key.toByteArray(Charsets.UTF_8)
        }

        // ══════════════════════════════════════════════════════════════
        // MIGRATIONS EXPLICITES (plus de fallbackToDestructiveMigration)
        // ══════════════════════════════════════════════════════════════

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v7: Add lastModified column to all entities for conflict resolution
                val tables = listOf(
                    "patients", "lectures_glucose", "medicaments",
                    "rendez_vous", "hba1c_lectures", "journal_entries"
                )
                for (table in tables) {
                    try {
                        db.execSQL("ALTER TABLE $table ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                    } catch (e: Exception) {
                        // Column may already exist in some schema versions
                        Log.w(TAG, "Migration 6→7: column lastModified may already exist in $table: ${e.message}")
                    }
                }
                Log.d(TAG, "Migration 6→7 complete: added lastModified columns")
            }
        }

        @Volatile
        private var INSTANCE: DiabetoDatabase? = null

        fun getInstance(context: Context): DiabetoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): DiabetoDatabase {
            val passphrase = getPassphrase(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                DiabetoDatabase::class.java,
                DATABASE_NAME
            )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_6_7)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        }
    }
}
