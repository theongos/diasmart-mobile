package com.diabeto.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.entity.*
import com.diabeto.data.model.ConsentStatus
import com.diabeto.data.model.UserRole
import com.diabeto.data.repository.AuthRepository
import com.diabeto.data.repository.DataSharingRepository
import com.diabeto.data.repository.PatientRepository
import com.diabeto.data.repository.RendezVousRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Filtre pour les rendez-vous
 */
enum class RendezVousFilter {
    TOUS, A_VENIR, PASSES
}

/**
 * État de la liste des rendez-vous
 */
data class PatientOption(
    val id: Long,
    val uid: String = "",
    val nom: String = "",
    val isFirestore: Boolean = false
)

/**
 * RDV simplifié pour l'affichage patient (depuis Firestore)
 */
data class RendezVousPatientItem(
    val id: String = "",
    val titre: String = "",
    val dateHeure: LocalDateTime = LocalDateTime.now(),
    val dureeMinutes: Int = 30,
    val type: String = "CONSULTATION",
    val lieu: String = "",
    val notes: String = "",
    val estConfirme: Boolean = false,
    val medecinNom: String = ""
) {
    fun estPasse(): Boolean = dateHeure.isBefore(LocalDateTime.now())
    fun estAujourdhui(): Boolean = dateHeure.toLocalDate() == LocalDate.now()
}

data class RendezVousUiState(
    val rendezVous: List<RendezVousAvecPatient> = emptyList(),
    val patientRendezVous: List<RendezVousPatientItem> = emptyList(),
    val filter: RendezVousFilter = RendezVousFilter.A_VENIR,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val addSuccess: Boolean = false,
    val patientId: Long? = null,
    val isMedecin: Boolean = false,
    val patientOptions: List<PatientOption> = emptyList()
)

/**
 * État du formulaire d'ajout de rendez-vous
 */
data class AddRendezVousState(
    val selectedPatientId: Long = 0,
    val titre: String = "",
    val date: LocalDate = LocalDate.now().plusDays(1),
    val heure: LocalTime = LocalTime.of(9, 0),
    val duree: Int = 30,
    val type: TypeRendezVous = TypeRendezVous.CONSULTATION,
    val lieu: String = "",
    val notes: String = "",
    val error: String? = null
)

@HiltViewModel
class RendezVousViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rendezVousRepository: RendezVousRepository,
    private val patientRepository: PatientRepository,
    private val authRepository: AuthRepository,
    private val dataSharingRepository: DataSharingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RendezVousVM"
    }

    private val initialPatientId: Long? = savedStateHandle.get<Long>("patientId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(RendezVousUiState(patientId = initialPatientId))
    val uiState: StateFlow<RendezVousUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow(AddRendezVousState())
    val addState: StateFlow<AddRendezVousState> = _addState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    init {
        initialPatientId?.let {
            _addState.update { state -> state.copy(selectedPatientId = it) }
        }
        loadData()
        loadPatientOptions()
    }

    private fun loadPatientOptions() {
        viewModelScope.launch {
            try {
                val profile = authRepository.getCurrentUserProfile()
                val isMedecin = profile?.role == UserRole.MEDECIN

                _uiState.update { it.copy(isMedecin = isMedecin) }

                if (isMedecin) {
                    // Charger les patients acceptes depuis Firestore
                    val myRequests = dataSharingRepository.getMyRequests()
                    val acceptedPatients = myRequests
                        .filter { it.status == ConsentStatus.ACCEPTED && it.isActive }

                    val options = acceptedPatients.mapIndexed { index, consent ->
                        PatientOption(
                            id = index.toLong() + 10000L,
                            uid = consent.patientUid,
                            nom = consent.patientNom,
                            isFirestore = true
                        )
                    }

                    // Aussi charger les patients locaux Room
                    val localPatients = patientRepository.getAllPatientsList()
                    val localOptions = localPatients.map {
                        PatientOption(id = it.id, nom = it.nomComplet, isFirestore = false)
                    }

                    _uiState.update {
                        it.copy(patientOptions = options + localOptions)
                    }
                }
                // Patient: pas de patient options - ils ne créent pas de RDV
            } catch (_: Exception) { }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val profile = authRepository.getCurrentUserProfile()
                val isMedecin = profile?.role == UserRole.MEDECIN

                if (isMedecin) {
                    // Médecin: charge les RDV locaux (qu'il a créés)
                    val rdvs = if (initialPatientId != null) {
                        rendezVousRepository.getRendezVousByPatientList(initialPatientId)
                            .mapNotNull { rdv ->
                                patientRepository.getPatientById(rdv.patientId)?.let { patient ->
                                    RendezVousAvecPatient(rdv, patient)
                                }
                            }
                    } else {
                        rendezVousRepository.getUpcomingRendezVous(50)
                    }
                    _uiState.update { it.copy(rendezVous = rdvs, isLoading = false, isMedecin = true) }
                } else {
                    // Patient: charge les RDV depuis Firestore (programmés par son médecin)
                    loadPatientRendezVousFromFirestore()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Charge les RDV du patient depuis Firestore.
     * Structure: rdv_shared/{patientUid}/rendezvous/{rdvId}
     * Chaque doc contient les détails du RDV créé par le médecin.
     */
    private suspend fun loadPatientRendezVousFromFirestore() {
        val currentUid = authRepository.currentUserId ?: return
        try {
            val docs = firestore.collection("rdv_shared")
                .document(currentUid)
                .collection("rendezvous")
                .get().await()

            val rdvList = docs.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    RendezVousPatientItem(
                        id = doc.id,
                        titre = data["titre"] as? String ?: "",
                        dateHeure = (data["dateHeure"] as? String)?.let {
                            LocalDateTime.parse(it)
                        } ?: LocalDateTime.now(),
                        dureeMinutes = (data["dureeMinutes"] as? Number)?.toInt() ?: 30,
                        type = data["type"] as? String ?: "CONSULTATION",
                        lieu = data["lieu"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        estConfirme = data["estConfirme"] as? Boolean ?: false,
                        medecinNom = data["medecinNom"] as? String ?: "Votre médecin"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing RDV doc", e)
                    null
                }
            }.sortedBy { it.dateHeure }

            _uiState.update {
                it.copy(
                    patientRendezVous = rdvList,
                    isLoading = false,
                    isMedecin = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading patient RDV from Firestore", e)
            _uiState.update { it.copy(isLoading = false, error = "Impossible de charger les rendez-vous") }
        }
    }

    fun setFilter(filter: RendezVousFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun toggleAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
        if (!show) {
            _addState.value = AddRendezVousState(selectedPatientId = initialPatientId ?: 0)
        }
    }

    fun updateAddField(field: String, value: Any?) {
        _addState.update { state ->
            when (field) {
                "patientId" -> state.copy(selectedPatientId = value as Long)
                "titre" -> state.copy(titre = value as String)
                "date" -> state.copy(date = value as LocalDate)
                "heure" -> state.copy(heure = value as LocalTime)
                "duree" -> state.copy(duree = value as Int)
                "type" -> state.copy(type = value as TypeRendezVous)
                "lieu" -> state.copy(lieu = value as String)
                "notes" -> state.copy(notes = value as String)
                else -> state
            }
        }
    }

    fun addRendezVous() {
        viewModelScope.launch {
            try {
                val state = _addState.value

                if (state.selectedPatientId <= 0L || state.titre.isBlank()) {
                    _addState.update { it.copy(error = "Patient et titre sont obligatoires") }
                    return@launch
                }

                val dateTime = LocalDateTime.of(state.date, state.heure)

                val rdv = RendezVousEntity(
                    patientId = state.selectedPatientId,
                    titre = state.titre.trim(),
                    dateHeure = dateTime,
                    dureeMinutes = state.duree,
                    type = state.type,
                    lieu = state.lieu.trim(),
                    notes = state.notes.trim()
                )

                val rdvId = rendezVousRepository.insertRendezVous(rdv)

                // Sync to Firestore rdv_shared for the patient
                val selectedOption = _uiState.value.patientOptions.find { it.id == state.selectedPatientId }
                val patientUid = selectedOption?.uid
                if (!patientUid.isNullOrBlank()) {
                    val medecinProfile = authRepository.getCurrentUserProfile()
                    val rdvData = mapOf(
                        "titre" to rdv.titre,
                        "dateHeure" to dateTime.toString(),
                        "dureeMinutes" to rdv.dureeMinutes,
                        "type" to rdv.type.name,
                        "lieu" to rdv.lieu,
                        "notes" to rdv.notes,
                        "estConfirme" to false,
                        "medecinNom" to (medecinProfile?.nomComplet ?: "Votre médecin"),
                        "medecinUid" to (authRepository.currentUserId ?: ""),
                        "createdAt" to LocalDateTime.now().toString()
                    )
                    try {
                        firestore.collection("rdv_shared")
                            .document(patientUid)
                            .collection("rendezvous")
                            .document(rdvId.toString())
                            .set(rdvData).await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync RDV to Firestore", e)
                    }
                }

                _uiState.update { it.copy(showAddDialog = false, addSuccess = true) }
                _addState.value = AddRendezVousState(selectedPatientId = initialPatientId ?: 0)

                loadData()

            } catch (e: Exception) {
                _addState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleConfirmation(rendezVous: RendezVousEntity) {
        viewModelScope.launch {
            try {
                rendezVousRepository.toggleConfirmation(rendezVous.id, rendezVous.estConfirme)
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteRendezVous(rendezVous: RendezVousEntity) {
        viewModelScope.launch {
            try {
                rendezVousRepository.deleteRendezVous(rendezVous)
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun getFilteredRendezVous(): List<RendezVousAvecPatient> {
        return when (_uiState.value.filter) {
            RendezVousFilter.TOUS -> _uiState.value.rendezVous
            RendezVousFilter.A_VENIR -> _uiState.value.rendezVous.filter { !it.rendezVous.estPasse() }
            RendezVousFilter.PASSES -> _uiState.value.rendezVous.filter { it.rendezVous.estPasse() }
        }
    }

    fun getFilteredPatientRendezVous(): List<RendezVousPatientItem> {
        return when (_uiState.value.filter) {
            RendezVousFilter.TOUS -> _uiState.value.patientRendezVous
            RendezVousFilter.A_VENIR -> _uiState.value.patientRendezVous.filter { !it.estPasse() }
            RendezVousFilter.PASSES -> _uiState.value.patientRendezVous.filter { it.estPasse() }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _addState.update { it.copy(error = null) }
    }
    
    fun clearAddSuccess() {
        _uiState.update { it.copy(addSuccess = false) }
    }
    
    fun refresh() {
        loadData()
    }
}
