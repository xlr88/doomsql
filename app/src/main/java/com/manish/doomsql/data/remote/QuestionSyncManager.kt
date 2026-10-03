package com.manish.doomsql.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.manish.doomsql.BuildConfig
import com.manish.doomsql.data.engine.SqlExecutionEngine
import com.manish.doomsql.data.engine.SqlResultComparator
import com.manish.doomsql.data.engine.SqlValidationResult
import com.manish.doomsql.data.engine.SqlValidator
import com.manish.doomsql.data.local.dao.QuestionIndexDao
import com.manish.doomsql.data.local.entity.QuestionIndexEntity
import com.manish.doomsql.data.model.QueryExecutionResult
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.repository.QuestionSource
import com.manish.doomsql.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface SyncResult {
    data class Success(val newCount: Int, val updatedCount: Int) : SyncResult
    data class UpToDate(val manifestVersion: Int) : SyncResult
    data object NoInternet : SyncResult
    data class Error(val message: String) : SyncResult
    data object Skipped : SyncResult
}

class QuestionSyncManager(
    private val context: Context,
    private val questionSource: QuestionSource,
    private val questionIndexDao: QuestionIndexDao,
    private val userPreferences: UserPreferencesRepository,
    private val sqlEngine: SqlExecutionEngine
) {
    companion object {
        private const val TAG = "DoomSQL_Sync"
        private const val TIMEOUT_MS = 15_000
        private const val SYNC_INTERVAL_MS = 24L * 60L * 60L * 1000L // 24h between automatic checks (manual check ignores this)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun sync(isManual: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. 24-hour gate for automatic syncs (launch / network reconnect)
        if (!isManual) {
            val lastSyncAt = userPreferences.getLastSyncAt()
            if (now - lastSyncAt < SYNC_INTERVAL_MS && lastSyncAt > 0) {
                Log.d(TAG, "Automatic sync skipped; 24h interval has not elapsed.")
                return@withContext SyncResult.Skipped
            }
        }

        // 2. Connectivity check
        if (!isOnline()) {
            Log.d(TAG, "Sync aborted; no active internet connection.")
            return@withContext SyncResult.NoInternet
        }

        // 3. Download manifest.json
        val manifestUrl = RemoteQuestionConfig.getManifestUrl()
        Log.i(TAG, "Fetching remote manifest from: $manifestUrl")
        val manifestBytes = try {
            downloadBytes(manifestUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download manifest: ${e.message}")
            return@withContext SyncResult.Error("Network error fetching question manifest.")
        }

        val manifest: RemoteManifest = try {
            json.decodeFromString(manifestBytes.decodeToString())
        } catch (e: Exception) {
            Log.e(TAG, "Manifest JSON parsing failed", e)
            return@withContext SyncResult.Error("Invalid question manifest format.")
        }

        val storedManifestVersion = userPreferences.getManifestVersion()
        if (manifest.manifestVersion <= storedManifestVersion && !isManual) {
            Log.i(TAG, "Manifest is up to date (version: ${manifest.manifestVersion}).")
            userPreferences.setLastSyncAt(now)
            return@withContext SyncResult.UpToDate(manifest.manifestVersion)
        }

        // 4. Process questions in manifest
        var newQuestionsCount = 0
        var updatedQuestionsCount = 0

        for (entry in manifest.questions) {
            // A. Check minAppVersionCode escape hatch
            if (entry.minAppVersionCode > BuildConfig.VERSION_CODE) {
                Log.d(TAG, "Skipping question ${entry.id}: requires min app version ${entry.minAppVersionCode}")
                continue
            }

            // B. Check if question needs downloading
            val localVersion = questionSource.getLocalContentVersion(entry.id)
            val isNew = localVersion == 0
            if (!isNew && localVersion >= entry.contentVersion) {
                // Already current
                continue
            }

            // C. Download question JSON
            val questionUrl = RemoteQuestionConfig.getQuestionUrl(entry.file)
            val questionBytes = try {
                downloadBytes(questionUrl)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download question ${entry.id} from $questionUrl: ${e.message}")
                continue
            }

            // D. Validation Step 1: SHA-256 Hash check
            val computedHash = sha256(questionBytes)
            if (!computedHash.equals(entry.sha256, ignoreCase = true)) {
                Log.e(TAG, "REJECTED [${entry.id}]: SHA-256 mismatch! Expected: ${entry.sha256}, Actual: $computedHash")
                continue
            }

            // E. Validation Step 2: Parse and schema verify
            val question: Question = try {
                json.decodeFromString(questionBytes.decodeToString())
            } catch (e: Exception) {
                Log.e(TAG, "REJECTED [${entry.id}]: Malformed question JSON", e)
                continue
            }

            if (question.id != entry.id) {
                Log.e(TAG, "REJECTED [${entry.id}]: Question ID mismatch inside JSON (${question.id})")
                continue
            }

            // Verify row arity matches column count for all declared tables
            var arityValid = true
            for (table in question.tables) {
                val colCount = table.columns.size
                for (row in table.rows) {
                    if (row.size != colCount) {
                        Log.e(TAG, "REJECTED [${entry.id}]: Table '${table.name}' row arity mismatch (${row.size} vs $colCount)")
                        arityValid = false
                        break
                    }
                }
                if (!arityValid) break
            }
            if (!arityValid) continue

            // Verify row arity for expectedOutput
            val expectedColCount = question.expectedOutput.columns.size
            for (row in question.expectedOutput.rows) {
                if (row.size != expectedColCount) {
                    Log.e(TAG, "REJECTED [${entry.id}]: expectedOutput row arity mismatch (${row.size} vs $expectedColCount)")
                    arityValid = false
                    break
                }
            }
            if (!arityValid) continue

            // F. Validation Step 3: SQL Validator
            val validationResult = SqlValidator.validate(question.solutionQuery)
            if (validationResult !is SqlValidationResult.Valid) {
                Log.e(TAG, "REJECTED [${entry.id}]: solutionQuery failed SQL validation")
                continue
            }

            // G. Validation Step 4: Sandbox Self-Check
            try {
                val executionResult = sqlEngine.execute(question, question.solutionQuery)
                if (executionResult !is QueryExecutionResult.Success) {
                    Log.e(TAG, "REJECTED [${entry.id}]: Sandbox execution error on solutionQuery")
                    continue
                }

                val comparison = SqlResultComparator.compare(
                    actual = executionResult.result,
                    expected = question.expectedOutput.toQueryResult(),
                    orderSensitive = question.orderSensitive
                )

                if (!comparison.isEqual) {
                    Log.e(TAG, "REJECTED [${entry.id}]: solutionQuery output did not match expectedOutput (${comparison.reason})")
                    continue
                }
            } catch (e: Exception) {
                Log.e(TAG, "REJECTED [${entry.id}]: Sandbox check crashed", e)
                continue
            }

            // All 4 checks passed! Atomically write question to internal storage
            val saved = questionSource.saveQuestionAtomically(question.id, questionBytes)
            if (saved) {
                // Upsert Room index
                val entity = QuestionIndexEntity(
                    id = question.id,
                    title = question.title,
                    description = question.description,
                    difficulty = question.difficulty.name,
                    tags = question.tags.joinToString(","),
                    contentVersion = question.contentVersion,
                    addedAt = entry.addedAt ?: question.addedAt,
                    sourceLayer = "INTERNAL_STORAGE",
                    file = entry.file
                )
                try {
                    questionIndexDao.upsertIndex(entity)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update Room index for ${question.id}", e)
                }

                if (isNew) {
                    newQuestionsCount++
                } else {
                    updatedQuestionsCount++
                }
                Log.i(TAG, "INSTALLED [${question.id}] v${question.contentVersion} successfully!")
            }
        }

        // 5. Update stored metadata
        userPreferences.setManifestVersion(manifest.manifestVersion)
        userPreferences.setLastSyncAt(now)
        if (newQuestionsCount > 0) {
            userPreferences.setNewQuestionsBannerCount(newQuestionsCount)
        }

        return@withContext if (newQuestionsCount > 0 || updatedQuestionsCount > 0) {
            SyncResult.Success(newQuestionsCount, updatedQuestionsCount)
        } else {
            SyncResult.UpToDate(manifest.manifestVersion)
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun downloadBytes(urlString: String): ByteArray {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.instanceFollowRedirects = true

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw java.io.IOException("HTTP error: $responseCode from $urlString")
        }

        return conn.inputStream.use(InputStream::readBytes)
    }

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
