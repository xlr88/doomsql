package com.manish.doomsql.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.manish.doomsql.BuildConfig
import com.manish.doomsql.data.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthRepository(private val context: Context) : AuthRepository {

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase is not initialized: ${e.message}")
            null
        }
    }

    override val isConfigured: Boolean
        get() = firebaseAuth != null

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        try {
            firebaseAuth?.let { auth ->
                _currentUser.value = auth.currentUser?.toAuthUser()
                auth.addAuthStateListener { updatedAuth ->
                    _currentUser.value = updatedAuth.currentUser?.toAuthUser()
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not attach auth listener: ${e.message}")
        }
    }

    override suspend fun signInWithGoogle(context: Context): AuthResult<AuthUser> {
        val auth = firebaseAuth ?: return AuthResult.Error(
            "Firebase is not configured yet. Please add google-services.json to the app folder."
        )

        return try {
            val credential = retrieveGoogleAuthCredential(context)
                ?: return AuthResult.Error("Could not retrieve Google credentials.")

            val authResult = auth.signInWithCredential(credential).awaitTask()
            val user = authResult.user?.toAuthUser()
            if (user != null) {
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Google Sign-In completed, but user details were unavailable.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Error("Google Sign-In was cancelled by user.", e)
        } catch (e: NoCredentialException) {
            // Also thrown when this build's signing SHA-1 isn't registered in Firebase.
            Log.w("AuthRepository", "NoCredentialException: ${e.message}", e)
            AuthResult.Error("Google Sign-In isn't available right now. Check that a Google account is added on this device, then try again.", e)
        } catch (e: GetCredentialException) {
            Log.w("AuthRepository", "Credential Manager exception: ${e.message}", e)
            val msg = when {
                e.message?.contains("cancel", ignoreCase = true) == true -> "Google Sign-In was cancelled."
                e.message?.contains("no account", ignoreCase = true) == true -> "No Google account found on this device."
                e.message?.contains("network", ignoreCase = true) == true -> "Network error during Google Sign-In. Please check your connection."
                else -> e.message ?: "Could not authenticate with Google."
            }
            AuthResult.Error(msg, e)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In failed", e)
            AuthResult.Error(mapAuthException(e), e)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign out error", e)
        }
        clearGoogleCredentialState()
    }

    /** Tells Android to forget the chosen Google account, so the next sign-in shows the account picker. */
    private suspend fun clearGoogleCredentialState() {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not clear credential state", e)
        }
    }

    override suspend fun deleteAccount(
        context: Context,
        alsoResetLocalProgress: Boolean,
        onResetLocalProgress: suspend () -> Unit
    ): AuthResult<Unit> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase is not initialized.")
        val user = auth.currentUser ?: return AuthResult.Error("No user is currently signed in.")

        return try {
            try {
                user.delete().awaitTask()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                Log.i("AuthRepository", "Recent login required to delete account. Re-authenticating with Google...")
                val reauthCredential = retrieveGoogleAuthCredential(context)
                    ?: return AuthResult.Error("For security, please sign in with Google again to confirm account deletion.")

                user.reauthenticate(reauthCredential).awaitTask()
                user.delete().awaitTask()
            }

            _currentUser.value = null
            clearGoogleCredentialState()
            if (alsoResetLocalProgress) {
                onResetLocalProgress()
            }
            AuthResult.Success(Unit)
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Error("Re-authentication was cancelled. Account was not deleted.", e)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Account deletion failed", e)
            AuthResult.Error(mapAuthException(e), e)
        }
    }

    /**
     * Obtains a Google AuthCredential from Android Credential Manager using GoogleIdTokenCredential.
     */
    private suspend fun retrieveGoogleAuthCredential(context: Context): AuthCredential? {
        val webClientId = getWebClientId(context) ?: return null

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        }
        return null
    }

    private fun getWebClientId(context: Context): String? {
        // 1. Try BuildConfig.WEB_CLIENT_ID if injected via Secrets plugin / .env
        try {
            val field = BuildConfig::class.java.getField("WEB_CLIENT_ID")
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) return value
        } catch (_: Exception) {}

        // 2. Try default_web_client_id generated from google-services.json by Google Services plugin
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val str = context.getString(resId)
                if (str.isNotBlank()) return str
            }
        } catch (_: Exception) {}

        return null
    }

    private fun mapAuthException(e: Throwable): String {
        return when (e) {
            is FirebaseNetworkException, is UnknownHostException ->
                "Network error. Please check your internet connection."
            is FirebaseAuthRecentLoginRequiredException ->
                "For security, please sign in again before deleting your account."
            else -> e.localizedMessage ?: "An unexpected authentication error occurred."
        }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        isEmailVerified = isEmailVerified,
        isAnonymous = isAnonymous
    )
}

/**
 * Await helper for Google Play Services / Firebase Tasks.
 */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
