package com.manish.doomsql.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manish.doomsql.data.repository.AuthRepository
import com.manish.doomsql.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val isForgotPasswordLoading: Boolean = false,
    val showVerificationNoticeDialog: Boolean = false
)

class SignInViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onDismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogle(context)
            _uiState.update { it.copy(isGoogleLoading = false) }
            when (result) {
                is AuthResult.Success -> {
                    onSuccess()
                }
                is AuthResult.Error -> {
                    // Don't show error if user simply dismissed/cancelled the picker
                    if (!result.message.contains("cancelled", ignoreCase = true)) {
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                }
            }
        }
    }

    fun submitEmailAuth(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val email = currentState.email.trim()
        val password = currentState.password

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address.") }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }

        if (currentState.isSignUpMode) {
            if (password.length < 6) {
                _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
                return
            }
            if (password != currentState.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.signUpWithEmail(email, password)
                _uiState.update { it.copy(isLoading = false) }
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                showVerificationNoticeDialog = true,
                                successMessage = "Account created! A verification email has been sent."
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                }
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.signInWithEmail(email, password)
                _uiState.update { it.copy(isLoading = false) }
                when (result) {
                    is AuthResult.Success -> {
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                }
            }
        }
    }

    fun openForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                forgotPasswordEmail = it.email,
                errorMessage = null
            )
        }
    }

    fun dismissForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotPasswordDialog = false) }
    }

    fun onForgotPasswordEmailChanged(email: String) {
        _uiState.update { it.copy(forgotPasswordEmail = email) }
    }

    fun submitForgotPassword() {
        val email = _uiState.value.forgotPasswordEmail.trim()
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isForgotPasswordLoading = true) }
            val result = authRepository.sendPasswordResetEmail(email)
            _uiState.update {
                it.copy(
                    isForgotPasswordLoading = false,
                    showForgotPasswordDialog = false
                )
            }
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(successMessage = "Password reset instructions sent to $email")
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun dismissVerificationNoticeDialog(onProceed: () -> Unit) {
        _uiState.update { it.copy(showVerificationNoticeDialog = false) }
        onProceed()
    }
}
