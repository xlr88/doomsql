package com.manish.doomsql.data.repository

import android.content.Context
import com.manish.doomsql.data.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
}

interface AuthRepository {
    /**
     * Flow emitting the currently authenticated user, or null if signed out.
     */
    val currentUser: StateFlow<AuthUser?>

    /**
     * True if Firebase has been initialized (e.g. google-services.json is present).
     */
    val isConfigured: Boolean

    /**
     * Authenticate via Google Sign-In using Android Credential Manager (androidx.credentials).
     */
    suspend fun signInWithGoogle(context: Context): AuthResult<AuthUser>

    /**
     * Authenticate via Email + Password.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult<AuthUser>

    /**
     * Create a new account with Email + Password and automatically send verification email.
     */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult<AuthUser>

    /**
     * Send password reset email to the specified address.
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit>

    /**
     * Resend verification email to current user.
     */
    suspend fun sendEmailVerification(): AuthResult<Unit>

    /**
     * Sign out current user.
     */
    suspend fun signOut()

    /**
     * Delete user account from Firebase.
     * Optionally resets local SQLite progress if [alsoResetLocalProgress] is true.
     */
    suspend fun deleteAccount(
        alsoResetLocalProgress: Boolean,
        onResetLocalProgress: suspend () -> Unit
    ): AuthResult<Unit>
}
