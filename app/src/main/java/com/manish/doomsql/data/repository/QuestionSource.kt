package com.manish.doomsql.data.repository

import android.content.Context
import android.util.Log
import com.manish.doomsql.data.model.Question
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Three-layer question source resolution:
 * Layer 1: filesDir/questions/ (internal storage, downloaded/updated)
 * Layer 2: assets/questions/ (bundled in APK baseline)
 */
class QuestionSource(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val questionsDir: File by lazy {
        File(context.filesDir, "questions").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Resolves a single Question by ID checking Layer 1 first, then Layer 2.
     */
    fun loadQuestion(id: String, fallbackFileName: String? = null): Question? {
        // Layer 1: Check internal storage
        val localFile = File(questionsDir, "$id.json")
        if (localFile.exists() && localFile.length() > 0) {
            try {
                val content = localFile.readText()
                return json.decodeFromString<Question>(content)
            } catch (e: Exception) {
                Log.w("QuestionSource", "Corrupt local question file for $id, falling back to assets", e)
            }
        }

        // Layer 2: Check bundled APK assets
        val assetFileName = fallbackFileName ?: "$id.json"
        return try {
            val content = context.assets.open("questions/$assetFileName").bufferedReader().use { it.readText() }
            json.decodeFromString<Question>(content)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads all questions, with Layer 1 replacing Layer 2 for existing IDs, and new Layer 1 questions appended.
     */
    fun loadAllQuestions(): List<Question> {
        val questionMap = mutableMapOf<String, Question>()

        // 1. Load baseline from assets
        try {
            val indexContent = context.assets.open("questions/index.json").bufferedReader().use { it.readText() }
            val assetFiles: List<String> = json.decodeFromString(indexContent)
            for (fileName in assetFiles) {
                try {
                    val content = context.assets.open("questions/$fileName").bufferedReader().use { it.readText() }
                    val q = json.decodeFromString<Question>(content)
                    questionMap[q.id] = q
                } catch (e: Exception) {
                    Log.w("QuestionSource", "Could not load asset question $fileName", e)
                }
            }
        } catch (e: Exception) {
            Log.e("QuestionSource", "Error loading asset index.json", e)
        }

        // 2. Override with or append questions from internal storage (Layer 1)
        if (questionsDir.exists()) {
            val localFiles = questionsDir.listFiles { _, name ->
                name.endsWith(".json") && !name.startsWith("tmp_")
            } ?: emptyArray()

            for (file in localFiles) {
                try {
                    val content = file.readText()
                    val q = json.decodeFromString<Question>(content)
                    questionMap[q.id] = q
                } catch (e: Exception) {
                    Log.w("QuestionSource", "Could not load internal question ${file.name}", e)
                }
            }
        }

        return questionMap.values.toList()
    }

    /**
     * Returns the local contentVersion for a question id (0 if not present).
     */
    fun getLocalContentVersion(id: String): Int {
        val localFile = File(questionsDir, "$id.json")
        if (localFile.exists()) {
            try {
                val q = json.decodeFromString<Question>(localFile.readText())
                return q.contentVersion
            } catch (_: Exception) {}
        }

        // Check assets
        try {
            val content = context.assets.open("questions/$id.json").bufferedReader().use { it.readText() }
            val q = json.decodeFromString<Question>(content)
            return q.contentVersion
        } catch (_: Exception) {}

        return 0
    }

    /**
     * Checks if a question exists in Layer 1 (internal storage).
     */
    fun hasInternalFile(id: String): Boolean {
        return File(questionsDir, "$id.json").exists()
    }

    /**
     * Atomically saves downloaded question content after verification.
     * Writes to tmp_<id>.json and renames to <id>.json.
     */
    fun saveQuestionAtomically(id: String, rawBytes: ByteArray): Boolean {
        val tempFile = File(questionsDir, "tmp_$id.json")
        val targetFile = File(questionsDir, "$id.json")

        return try {
            tempFile.writeBytes(rawBytes)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                // Fallback copy if renameTo fails
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e("QuestionSource", "Failed to atomically save question $id", e)
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }
}
