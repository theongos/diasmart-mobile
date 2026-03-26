package com.diabeto.data.repository

import android.util.Log
import com.diabeto.data.dao.*
import com.diabeto.data.entity.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud backup/restore of local Room database to Firestore.
 *
 * Firestore structure:
 *   backups/{uid}/patients/{id}
 *   backups/{uid}/glucose/{id}
 *   backups/{uid}/hba1c/{id}
 *   backups/{uid}/medicaments/{id}
 *   backups/{uid}/rendezvous/{id}
 *   backups/{uid}/journal/{id}
 *   backups/{uid}/metadata/info  → { lastBackupAt, counts... }
 *
 * Auto-backup: called after each write operation.
 * Restore: called on login if local DB is empty.
 */
@Singleton
class CloudBackupRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val glucoseDao: GlucoseDao,
    private val hbA1cDao: HbA1cDao,
    private val medicamentDao: MedicamentDao,
    private val rendezVousDao: RendezVousDao,
    private val journalDao: JournalDao
) {
    companion object {
        private const val TAG = "CloudBackup"
        private const val BACKUPS = "backups"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String? get() = auth.currentUser?.uid

    // ══════════════════════════════════════════════════════════════
    // FULL BACKUP — uploads all local data to Firestore
    // ══════════════════════════════════════════════════════════════

    suspend fun performFullBackup(): Result<Int> = withContext(Dispatchers.IO) {
        val userId = uid ?: return@withContext Result.failure(Exception("Non connecte"))
        try {
            var totalDocs = 0
            val userRef = db.collection(BACKUPS).document(userId)

            // 1. Patients
            val patients = patientDao.getAllPatientsList()
            for (p in patients) {
                userRef.collection("patients").document(p.id.toString())
                    .set(patientToMap(p), SetOptions.merge()).await()
                totalDocs++
            }

            // 2. Glucose readings (batch per patient)
            for (p in patients) {
                val readings = glucoseDao.getLecturesByPatientList(p.id, 100000)
                for (r in readings) {
                    userRef.collection("glucose").document(r.id.toString())
                        .set(glucoseToMap(r), SetOptions.merge()).await()
                    totalDocs++
                }
            }

            // 3. HbA1c
            for (p in patients) {
                val hba1cs = hbA1cDao.getHbA1cByPatientList(p.id)
                for (h in hba1cs) {
                    userRef.collection("hba1c").document(h.id.toString())
                        .set(hba1cToMap(h), SetOptions.merge()).await()
                    totalDocs++
                }
            }

            // 4. Medicaments
            for (p in patients) {
                val meds = medicamentDao.getMedicamentsByPatientList(p.id)
                for (m in meds) {
                    userRef.collection("medicaments").document(m.id.toString())
                        .set(medicamentToMap(m), SetOptions.merge()).await()
                    totalDocs++
                }
            }

            // 5. Rendez-vous
            for (p in patients) {
                val rdvs = rendezVousDao.getRendezVousByPatientList(p.id)
                for (r in rdvs) {
                    userRef.collection("rendezvous").document(r.id.toString())
                        .set(rendezVousToMap(r), SetOptions.merge()).await()
                    totalDocs++
                }
            }

            // 6. Journal
            for (p in patients) {
                val entries = journalDao.getEntriesByPatientList(p.id)
                for (j in entries) {
                    userRef.collection("journal").document(j.id.toString())
                        .set(journalToMap(j), SetOptions.merge()).await()
                    totalDocs++
                }
            }

            // 7. Metadata
            userRef.collection("metadata").document("info").set(
                mapOf(
                    "lastBackupAt" to LocalDateTime.now().toString(),
                    "patientCount" to patients.size,
                    "totalDocuments" to totalDocs
                ),
                SetOptions.merge()
            ).await()

            Log.d(TAG, "Full backup: $totalDocs documents uploaded")
            Result.success(totalDocs)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // FULL RESTORE — downloads cloud data to local Room DB
    // ══════════════════════════════════════════════════════════════

    suspend fun performFullRestore(): Result<Int> = withContext(Dispatchers.IO) {
        val userId = uid ?: return@withContext Result.failure(Exception("Non connecte"))
        try {
            var totalDocs = 0
            val userRef = db.collection(BACKUPS).document(userId)

            // Check if backup exists
            val metadata = userRef.collection("metadata").document("info").get().await()
            if (!metadata.exists()) {
                Log.d(TAG, "No cloud backup found for $userId")
                return@withContext Result.success(0)
            }

            // ID mapping: cloud ID → new local ID (since Room auto-generates IDs)
            val patientIdMap = mutableMapOf<Long, Long>()

            // 1. Restore patients
            val patientDocs = userRef.collection("patients").get().await()
            for (doc in patientDocs.documents) {
                val patient = mapToPatient(doc.data ?: continue)
                val oldId = patient.id
                val newId = patientDao.insertPatient(patient.copy(id = 0)) // auto-generate
                patientIdMap[oldId] = newId
                totalDocs++
            }

            // 2. Restore glucose
            val glucoseDocs = userRef.collection("glucose").get().await()
            for (doc in glucoseDocs.documents) {
                val reading = mapToGlucose(doc.data ?: continue)
                val newPatientId = patientIdMap[reading.patientId] ?: continue
                glucoseDao.insertLecture(reading.copy(id = 0, patientId = newPatientId))
                totalDocs++
            }

            // 3. Restore HbA1c
            val hba1cDocs = userRef.collection("hba1c").get().await()
            for (doc in hba1cDocs.documents) {
                val hba1c = mapToHbA1c(doc.data ?: continue)
                val newPatientId = patientIdMap[hba1c.patientId] ?: continue
                hbA1cDao.insertHbA1c(hba1c.copy(id = 0, patientId = newPatientId))
                totalDocs++
            }

            // 4. Restore medicaments
            val medDocs = userRef.collection("medicaments").get().await()
            for (doc in medDocs.documents) {
                val med = mapToMedicament(doc.data ?: continue)
                val newPatientId = patientIdMap[med.patientId] ?: continue
                medicamentDao.insertMedicament(med.copy(id = 0, patientId = newPatientId))
                totalDocs++
            }

            // 5. Restore rendez-vous
            val rdvDocs = userRef.collection("rendezvous").get().await()
            for (doc in rdvDocs.documents) {
                val rdv = mapToRendezVous(doc.data ?: continue)
                val newPatientId = patientIdMap[rdv.patientId] ?: continue
                rendezVousDao.insertRendezVous(rdv.copy(id = 0, patientId = newPatientId))
                totalDocs++
            }

            // 6. Restore journal
            val journalDocs = userRef.collection("journal").get().await()
            for (doc in journalDocs.documents) {
                val entry = mapToJournal(doc.data ?: continue)
                val newPatientId = patientIdMap[entry.patientId] ?: continue
                journalDao.insertEntry(entry.copy(id = 0, patientId = newPatientId))
                totalDocs++
            }

            Log.d(TAG, "Full restore: $totalDocs documents restored")
            Result.success(totalDocs)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // INCREMENTAL BACKUP — single entity sync after write
    // ══════════════════════════════════════════════════════════════

    suspend fun backupPatient(patient: PatientEntity) = backupDoc("patients", patient.id.toString(), patientToMap(patient))
    suspend fun backupGlucose(reading: LectureGlucoseEntity) = backupDoc("glucose", reading.id.toString(), glucoseToMap(reading))
    suspend fun backupHbA1c(hba1c: HbA1cEntity) = backupDoc("hba1c", hba1c.id.toString(), hba1cToMap(hba1c))
    suspend fun backupMedicament(med: MedicamentEntity) = backupDoc("medicaments", med.id.toString(), medicamentToMap(med))
    suspend fun backupRendezVous(rdv: RendezVousEntity) = backupDoc("rendezvous", rdv.id.toString(), rendezVousToMap(rdv))
    suspend fun backupJournal(entry: JournalEntity) = backupDoc("journal", entry.id.toString(), journalToMap(entry))

    suspend fun deleteBackupDoc(collection: String, docId: String) {
        val userId = uid ?: return
        try {
            db.collection(BACKUPS).document(userId)
                .collection(collection).document(docId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Delete backup doc failed", e)
        }
    }

    private suspend fun backupDoc(collection: String, docId: String, data: Map<String, Any?>) {
        val userId = uid ?: return
        try {
            db.collection(BACKUPS).document(userId)
                .collection(collection).document(docId)
                .set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Backup $collection/$docId failed", e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // CHECK IF RESTORE NEEDED
    // ══════════════════════════════════════════════════════════════

    suspend fun hasCloudBackup(): Boolean {
        val userId = uid ?: return false
        return try {
            val meta = db.collection(BACKUPS).document(userId)
                .collection("metadata").document("info").get().await()
            meta.exists() && (meta.getLong("patientCount") ?: 0) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isLocalDbEmpty(): Boolean = withContext(Dispatchers.IO) {
        patientDao.getPatientCount() == 0
    }

    suspend fun getBackupInfo(): Map<String, Any>? {
        val userId = uid ?: return null
        return try {
            val meta = db.collection(BACKUPS).document(userId)
                .collection("metadata").document("info").get().await()
            meta.data
        } catch (e: Exception) {
            null
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ENTITY ↔ MAP CONVERTERS
    // ══════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════

    private fun patientToMap(p: PatientEntity) = mapOf(
        "id" to p.id,
        "nom" to p.nom,
        "prenom" to p.prenom,
        "dateNaissance" to p.dateNaissance.toString(),
        "sexe" to p.sexe.name,
        "telephone" to p.telephone,
        "email" to p.email,
        "adresse" to p.adresse,
        "typeDiabete" to p.typeDiabete.name,
        "dateDiagnostic" to p.dateDiagnostic?.toString(),
        "poids" to p.poids,
        "taille" to p.taille,
        "tourDeTaille" to p.tourDeTaille,
        "masseGrasse" to p.masseGrasse,
        "notes" to p.notes,
        "createdAt" to p.createdAt.toString(),
        "updatedAt" to p.updatedAt.toString()
    )

    private fun mapToPatient(m: Map<String, Any?>): PatientEntity = PatientEntity(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        nom = m["nom"] as? String ?: "",
        prenom = m["prenom"] as? String ?: "",
        dateNaissance = (m["dateNaissance"] as? String)?.let { LocalDate.parse(it) } ?: LocalDate.now(),
        sexe = (m["sexe"] as? String)?.let { runCatching { Sexe.valueOf(it) }.getOrNull() } ?: Sexe.HOMME,
        telephone = m["telephone"] as? String ?: "",
        email = m["email"] as? String ?: "",
        adresse = m["adresse"] as? String ?: "",
        typeDiabete = (m["typeDiabete"] as? String)?.let { runCatching { TypeDiabete.valueOf(it) }.getOrNull() } ?: TypeDiabete.TYPE_2,
        dateDiagnostic = (m["dateDiagnostic"] as? String)?.let { LocalDate.parse(it) },
        poids = (m["poids"] as? Number)?.toDouble(),
        taille = (m["taille"] as? Number)?.toDouble(),
        tourDeTaille = (m["tourDeTaille"] as? Number)?.toDouble(),
        masseGrasse = (m["masseGrasse"] as? Number)?.toDouble(),
        notes = m["notes"] as? String ?: "",
        createdAt = (m["createdAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
        updatedAt = (m["updatedAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    )

    private fun glucoseToMap(r: LectureGlucoseEntity) = mapOf(
        "id" to r.id,
        "patientId" to r.patientId,
        "valeur" to r.valeur,
        "dateHeure" to r.dateHeure.toString(),
        "contexte" to r.contexte.name,
        "notes" to r.notes,
        "createdAt" to r.createdAt.toString()
    )

    private fun mapToGlucose(m: Map<String, Any?>): LectureGlucoseEntity = LectureGlucoseEntity(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        patientId = (m["patientId"] as? Number)?.toLong() ?: 0,
        valeur = (m["valeur"] as? Number)?.toDouble() ?: 0.0,
        dateHeure = (m["dateHeure"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
        contexte = (m["contexte"] as? String)?.let { runCatching { ContexteGlucose.valueOf(it) }.getOrNull() }
            ?: ContexteGlucose.AUTRE,
        notes = m["notes"] as? String ?: "",
        createdAt = (m["createdAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    )

    private fun hba1cToMap(h: HbA1cEntity) = mapOf(
        "id" to h.id,
        "patientId" to h.patientId,
        "valeur" to h.valeur,
        "dateMesure" to h.dateMesure.toString(),
        "laboratoire" to h.laboratoire,
        "notes" to h.notes,
        "estEstimation" to h.estEstimation,
        "createdAt" to h.createdAt.toString()
    )

    private fun mapToHbA1c(m: Map<String, Any?>): HbA1cEntity = HbA1cEntity(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        patientId = (m["patientId"] as? Number)?.toLong() ?: 0,
        valeur = (m["valeur"] as? Number)?.toDouble() ?: 0.0,
        dateMesure = (m["dateMesure"] as? String)?.let { LocalDate.parse(it) } ?: LocalDate.now(),
        laboratoire = m["laboratoire"] as? String ?: "",
        notes = m["notes"] as? String ?: "",
        estEstimation = m["estEstimation"] as? Boolean ?: false,
        createdAt = (m["createdAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    )

    private fun medicamentToMap(med: MedicamentEntity) = mapOf(
        "id" to med.id,
        "patientId" to med.patientId,
        "nom" to med.nom,
        "dosage" to med.dosage,
        "frequence" to med.frequence.name,
        "heurePrise" to med.heurePrise.toString(),
        "dateDebut" to med.dateDebut.toString(),
        "dateFin" to med.dateFin?.toString(),
        "estActif" to med.estActif,
        "rappelActive" to med.rappelActive,
        "notes" to med.notes,
        "createdAt" to med.createdAt.toString()
    )

    private fun mapToMedicament(m: Map<String, Any?>): MedicamentEntity = MedicamentEntity(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        patientId = (m["patientId"] as? Number)?.toLong() ?: 0,
        nom = m["nom"] as? String ?: "",
        dosage = m["dosage"] as? String ?: "",
        frequence = (m["frequence"] as? String)?.let { runCatching { FrequencePrise.valueOf(it) }.getOrNull() }
            ?: FrequencePrise.QUOTIDIEN,
        heurePrise = (m["heurePrise"] as? String)?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0),
        dateDebut = (m["dateDebut"] as? String)?.let { LocalDate.parse(it) } ?: LocalDate.now(),
        dateFin = (m["dateFin"] as? String)?.let { LocalDate.parse(it) },
        estActif = m["estActif"] as? Boolean ?: true,
        rappelActive = m["rappelActive"] as? Boolean ?: true,
        notes = m["notes"] as? String ?: "",
        createdAt = (m["createdAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    )

    private fun rendezVousToMap(r: RendezVousEntity) = mapOf(
        "id" to r.id,
        "patientId" to r.patientId,
        "titre" to r.titre,
        "dateHeure" to r.dateHeure.toString(),
        "dureeMinutes" to r.dureeMinutes,
        "type" to r.type.name,
        "lieu" to r.lieu,
        "notes" to r.notes,
        "estConfirme" to r.estConfirme,
        "rappelEnvoye" to r.rappelEnvoye,
        "createdAt" to r.createdAt.toString()
    )

    private fun mapToRendezVous(m: Map<String, Any?>): RendezVousEntity = RendezVousEntity(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        patientId = (m["patientId"] as? Number)?.toLong() ?: 0,
        titre = m["titre"] as? String ?: "",
        dateHeure = (m["dateHeure"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
        dureeMinutes = (m["dureeMinutes"] as? Number)?.toInt() ?: 30,
        type = (m["type"] as? String)?.let { runCatching { TypeRendezVous.valueOf(it) }.getOrNull() }
            ?: TypeRendezVous.CONSULTATION,
        lieu = m["lieu"] as? String ?: "",
        notes = m["notes"] as? String ?: "",
        estConfirme = m["estConfirme"] as? Boolean ?: false,
        rappelEnvoye = m["rappelEnvoye"] as? Boolean ?: false,
        createdAt = (m["createdAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    )

    private fun journalToMap(j: JournalEntity) = mapOf(
        "id" to j.id,
        "patientId" to j.patientId,
        "date" to j.date.toString(),
        "humeur" to j.humeur.name,
        "niveauStress" to j.niveauStress.name,
        "qualiteSommeil" to j.qualiteSommeil.name,
        "heuresSommeil" to j.heuresSommeil,
        "activitePhysique" to j.activitePhysique,
        "minutesActivite" to j.minutesActivite,
        "pas" to j.pas,
        "notes" to j.notes,
        "glycemieCorrelation" to j.glycemieCorrelation,
        "createdAt" to j.createdAt.toString()
    )

    private fun mapToJournal(m: Map<String, Any?>): JournalEntity = JournalEntity(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        patientId = (m["patientId"] as? Number)?.toLong() ?: 0,
        date = (m["date"] as? String)?.let { LocalDate.parse(it) } ?: LocalDate.now(),
        humeur = (m["humeur"] as? String)?.let { runCatching { Humeur.valueOf(it) }.getOrNull() } ?: Humeur.NEUTRE,
        niveauStress = (m["niveauStress"] as? String)?.let { runCatching { NiveauStress.valueOf(it) }.getOrNull() } ?: NiveauStress.AUCUN,
        qualiteSommeil = (m["qualiteSommeil"] as? String)?.let { runCatching { QualiteSommeil.valueOf(it) }.getOrNull() } ?: QualiteSommeil.BONNE,
        heuresSommeil = (m["heuresSommeil"] as? Number)?.toDouble() ?: 7.0,
        activitePhysique = m["activitePhysique"] as? Boolean ?: false,
        minutesActivite = (m["minutesActivite"] as? Number)?.toInt() ?: 0,
        pas = (m["pas"] as? Number)?.toInt() ?: 0,
        notes = m["notes"] as? String ?: "",
        glycemieCorrelation = (m["glycemieCorrelation"] as? Number)?.toDouble(),
        createdAt = (m["createdAt"] as? String)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    )
}
