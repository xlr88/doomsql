package com.manish.doomsql.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RemoteManifest(
    val manifestVersion: Int,
    val generatedAt: String? = null,
    val questions: List<RemoteQuestionEntry> = emptyList()
)

@Serializable
data class RemoteQuestionEntry(
    val id: String,
    val file: String,
    val contentVersion: Int = 1,
    val sha256: String,
    val minAppVersionCode: Int = 1,
    val difficulty: String,
    val title: String,
    val addedAt: String? = null
)

object RemoteQuestionConfig {
    /**
     * Default GitHub repository serving content.
     * Can be customized or overridden if using a dedicated content repo.
     */
    const val DEFAULT_REPO = "xlr88/doomsql-content"
    const val BRANCH = "main"

    fun getManifestUrl(repo: String = DEFAULT_REPO): String {
        // Hourly cache-busting as specified in README to bypass jsDelivr @main CDN stale window
        val hourlyBuster = System.currentTimeMillis() / 3_600_000L
        return "https://cdn.jsdelivr.net/gh/$repo@$BRANCH/questions/manifest.json?v=$hourlyBuster"
    }

    fun getQuestionUrl(fileName: String, repo: String = DEFAULT_REPO): String {
        return "https://cdn.jsdelivr.net/gh/$repo@$BRANCH/questions/$fileName"
    }
}
