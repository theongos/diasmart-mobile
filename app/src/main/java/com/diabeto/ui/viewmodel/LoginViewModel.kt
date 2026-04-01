package com.diabeto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.database.DiabetoDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val database: DiabetoDatabase
) : ViewModel() {

    data class LoginUiState(
        val isLoggedIn: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val showDeleteConfirmation: Boolean = false
    )

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginClicked(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Identifiant et mot de passe sont requis") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        // Authentication is handled by Firebase Auth via LoginScreen/AuthViewModel
        // This ViewModel is kept for legacy local-only screens
        _uiState.update { it.copy(isLoading = false, error = "Veuillez utiliser la connexion Firebase") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun requestDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun confirmDeleteData() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmation = false) }
            database.clearAllTables()
        }
    }
}
