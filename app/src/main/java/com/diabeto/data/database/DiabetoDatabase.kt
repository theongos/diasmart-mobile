package com.diabeto.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.diabeto.data.dao.*
import com.diabeto.data.entity.*

/**
 * Base de données Room principale de l'application
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
    version = 6,
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
        
        @Volatile
        private var INSTANCE: DiabetoDatabase? = null
        
        fun getInstance(context: Context): DiabetoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): DiabetoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DiabetoDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
