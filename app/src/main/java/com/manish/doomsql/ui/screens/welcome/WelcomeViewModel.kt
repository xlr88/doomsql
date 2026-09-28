package com.manish.doomsql.ui.screens.welcome

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manish.doomsql.data.repository.AuthRepository
import com.manish.doomsql.data.repository.AuthResult
import com.manish.doomsql.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null
)

class WelcomeViewModel(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun continueWithGoogle(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogle(context)
            _uiState.update { it.copy(isGoogleLoading = false) }
            when (result) {
                is AuthResult.Success -> {
                    userPreferences.setCompletedWelcome(true)
                    onComplete()
                }
                is AuthResult.Error -> {
                    // Do not show an error snackbar if the user simply dismissed/cancelled the sheet
                    if (!result.message.contains("cancelled", ignoreCase = true)) {
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                }
            }
        }
    }

    fun practiceOffline(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setCompletedWelcome(true)
            onComplete()
        }
    }

    fun skip(onComplete: () -> Unit) {
        practiceOffline(onComplete)
    }
}
