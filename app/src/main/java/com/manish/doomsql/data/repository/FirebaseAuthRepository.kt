package com.manish.doomsql.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.manish.doomsql.BuildConfig
import com.manish.doomsql.data.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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

        val webClientId = getWebClientId(context)
            ?: return AuthResult.Error(
                "Google Sign-In Web Client ID not found. Ensure google-services.json contains an OAuth Web Client ID or define WEB_CLIENT_ID in your .env file."
            )

        return try {
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
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).awaitTask()
                val user = authResult.user?.toAuthUser()
                if (user != null) {
                    _currentUser.value = user
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Google Sign-In completed, but user details were unavailable.")
                }
            } else {
                AuthResult.Error("Unsupported credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Error("Google Sign-In was cancelled.", e)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In failed", e)
            AuthResult.Error(mapFirebaseException(e), e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult<AuthUser> {
        val auth = firebaseAuth ?: return AuthResult.Error(
            "Firebase is not configured yet. Please add google-services.json to the app folder."
        )

        if (email.isBlank()) return AuthResult.Error("Please enter your email address.")
        if (password.isBlank()) return AuthResult.Error("Please enter your password.")

        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
            val user = result.user?.toAuthUser()
            if (user != null) {
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Sign in failed. Could not retrieve user profile.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email sign-in failed", e)
            AuthResult.Error(mapFirebaseException(e), e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult<AuthUser> {
        val auth = firebaseAuth ?: return AuthResult.Error(
            "Firebase is not configured yet. Please add google-services.json to the app folder."
        )

        if (email.isBlank()) return AuthResult.Error("Please enter your email address.")
        if (password.length < 6) return AuthResult.Error("Password must be at least 6 characters long.")

        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
            val firebaseUser = result.user
            try {
                firebaseUser?.sendEmailVerification()?.awaitTask()
            } catch (verEx: Exception) {
                Log.w("AuthRepository", "Failed to send initial verification email", verEx)
            }
            val user = firebaseUser?.toAuthUser()
            if (user != null) {
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Account creation failed.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email sign-up failed", e)
            AuthResult.Error(mapFirebaseException(e), e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        val auth = firebaseAuth ?: return AuthResult.Error(
            "Firebase is not configured yet. Please add google-services.json to the app folder."
        )

        if (email.isBlank()) return AuthResult.Error("Please enter your email address.")

        return try {
            auth.sendPasswordResetEmail(email.trim()).awaitTask()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Password reset failed", e)
            AuthResult.Error(mapFirebaseException(e), e)
        }
    }

    override suspend fun sendEmailVerification(): AuthResult<Unit> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase is not initialized.")
        val user = auth.currentUser ?: return AuthResult.Error("No user is currently signed in.")

        return try {
            user.sendEmailVerification().awaitTask()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to send verification email", e)
            AuthResult.Error(mapFirebaseException(e), e)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign out error", e)
        }
    }

    override suspend fun deleteAccount(
        alsoResetLocalProgress: Boolean,
        onResetLocalProgress: suspend () -> Unit
    ): AuthResult<Unit> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase is not initialized.")
        val user = auth.currentUser ?: return AuthResult.Error("No user is currently signed in.")

        return try {
            user.delete().awaitTask()
            _currentUser.value = null
            if (alsoResetLocalProgress) {
                onResetLocalProgress()
            }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Account deletion failed", e)
            AuthResult.Error(mapFirebaseException(e), e)
        }
    }

    private fun getWebClientId(context: Context): String? {
        // 1. Try BuildConfig.WEB_CLIENT_ID if injected via Secrets plugin
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

    private fun mapFirebaseException(e: Throwable): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect password or invalid email format."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            is FirebaseAuthRecentLoginRequiredException -> "For security, please sign in again before deleting your account."
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
