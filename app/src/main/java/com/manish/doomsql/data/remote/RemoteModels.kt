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
    /** Public content repo: https://github.com/xlr88/doomsql-content */
    const val DEFAULT_REPO = "xlr88/doomsql-content"
    const val BRANCH = "main"

    /** Primary: jsDelivr CDN. The hourly cache-buster avoids jsDelivr's long cache on @main. */
    fun getManifestUrl(repo: String = DEFAULT_REPO): String {
        val hourlyBuster = System.currentTimeMillis() / 3_600_000L
        return "https://cdn.jsdelivr.net/gh/$repo@$BRANCH/questions/manifest.json?v=$hourlyBuster"
    }

    /** Fallback: GitHub raw files (different host, used only if jsDelivr fails). */
    fun getFallbackManifestUrl(repo: String = DEFAULT_REPO): String {
        return "https://raw.githubusercontent.com/$repo/$BRANCH/questions/manifest.json"
    }

    fun getQuestionUrl(fileName: String, repo: String = DEFAULT_REPO): String {
        return "https://cdn.jsdelivr.net/gh/$repo@$BRANCH/questions/$fileName"
    }

    fun getFallbackQuestionUrl(fileName: String, repo: String = DEFAULT_REPO): String {
        return "https://raw.githubusercontent.com/$repo/$BRANCH/questions/$fileName"
    }
}
