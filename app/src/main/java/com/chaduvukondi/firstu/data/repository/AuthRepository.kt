package com.chaduvukondi.firstu.data.repository

import android.content.Context
import com.chaduvukondi.firstu.data.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
}

/**
 * Domain repository abstraction for authentication in DoomSQL.
 * Ensures the rest of the app never touches Firebase directly.
 */
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
     * Authenticate via Google Sign-In using Android Credential Manager (androidx.credentials + GoogleIdOption).
     */
    suspend fun signInWithGoogle(context: Context): AuthResult<AuthUser>

    /**
     * Sign out current user.
     */
    suspend fun signOut()

    /**
     * Delete user account from Firebase (re-authenticating with Google if Firebase requires recent login).
     * Optionally resets local SQLite progress if [alsoResetLocalProgress] is true.
     */
    suspend fun deleteAccount(
        context: Context,
        alsoResetLocalProgress: Boolean,
        onResetLocalProgress: suspend () -> Unit
    ): AuthResult<Unit>
}
