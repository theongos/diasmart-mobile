package com.diabeto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.entity.PatientEntity
import com.diabeto.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel léger pour synchroniser les données morphométriques
 * entre le profil Firestore (ProfileScreen) et le patient Room DB.
 *
 * Quand l'utilisateur modifie taille/poids/tourTaille/masseGrasse dans le profil,
 * ça se met automatiquement à jour dans le patient Room, et inversement.
 */
@HiltViewModel
class ProfileSyncViewModel @Inject constructor(
    private val patientRepository: PatientRepository
) : ViewModel() {

    /**
     * Récupère le premier patient Room DB (l'utilisateur courant)
     */
    suspend fun getFirstPatient(): PatientEntity? {
        return try {
            patientRepository.getAllPatientsList().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Met à jour un champ morphométrique du patient Room DB
     * quand l'utilisateur le modifie dans ProfileScreen (Firestore).
     */
    fun syncMorphoToPatient(field: String, value: Double?) {
        viewModelScope.launch {
            try {
                val patient = patientRepository.getAllPatientsList().firstOrNull() ?: return@launch

                val updatedPatient = when (field) {
                    "poids" -> patient.copy(poids = value)
                    "taille" -> patient.copy(taille = value)
                    "tourTaille" -> patient.copy(tourDeTaille = value)
                    "masseGrasse" -> patient.copy(masseGrasse = value)
                    else -> return@launch
                }

                patientRepository.updatePatient(updatedPatient)
                android.util.Log.i("ProfileSyncVM", "Morpho sync -> Room patient ($field = $value)")
            } catch (e: Exception) {
                android.util.Log.w("ProfileSyncVM", "Sync morpho Room échouée (non bloquant)", e)
            }
        }
    }
}
