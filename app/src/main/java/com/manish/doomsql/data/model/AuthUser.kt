package com.manish.doomsql.data.model

/**
 * Clean domain representation of an authenticated user.
 * Decouples the application UI and domain layers from Firebase SDK dependencies.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val isAnonymous: Boolean = false
)
