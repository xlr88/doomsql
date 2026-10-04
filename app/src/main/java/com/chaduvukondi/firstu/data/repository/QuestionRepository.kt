package com.chaduvukondi.firstu.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.chaduvukondi.firstu.data.engine.SandboxSqlEngine
import com.chaduvukondi.firstu.data.engine.SqlExecutionEngine
import com.chaduvukondi.firstu.data.local.DoomSqlDatabase
import com.chaduvukondi.firstu.data.local.entity.DailyActivityEntity
import com.chaduvukondi.firstu.data.local.entity.QueryDraftEntity
import com.chaduvukondi.firstu.data.local.entity.QuestionIndexEntity
import com.chaduvukondi.firstu.data.local.entity.QuestionProgressEntity
import com.chaduvukondi.firstu.data.model.Question
import com.chaduvukondi.firstu.data.remote.QuestionSyncManager
import com.chaduvukondi.firstu.data.remote.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuestionRepository(
    private val context: Context,
    private val database: DoomSqlDatabase,
    private val questionSource: QuestionSource = QuestionSource(context),
    private val userPreferences: UserPreferencesRepository = UserPreferencesRepository(context),
    private val sqlEngine: SqlExecutionEngine = SandboxSqlEngine()
) {
    private val prefs = context.getSharedPreferences("doomsql_prefs", Context.MODE_PRIVATE)

    private val progressDao = database.questionProgressDao()
    private val draftDao = database.queryDraftDao()
    private val activityDao = database.dailyActivityDao()
    private val indexDao = database.questionIndexDao()

    private val syncManager = QuestionSyncManager(
        context = context,
        questionSource = questionSource,
        questionIndexDao = indexDao,
        userPreferences = userPreferences,
        sqlEngine = sqlEngine
    )

    private val _questionsFlow = MutableStateFlow<List<Question>>(emptyList())
    val questionsFlow: StateFlow<List<Question>> = _questionsFlow.asStateFlow()

    val progressMapFlow: Flow<Map<String, QuestionProgressEntity>> =
        progressDao.getAllProgress().map { list ->
            list.associateBy { it.questionId }
        }

    val dailyActivitiesFlow: Flow<List<DailyActivityEntity>> =
        activityDao.getAllActivities()

    /**
     * Retrieves questions resolving Layer 1 (internal storage) first, then Layer 2 (bundled assets).
     * Populates Room index and local in-memory StateFlow.
     */
    suspend fun getQuestions(): List<Question> = withContext(Dispatchers.IO) {
        if (_questionsFlow.value.isNotEmpty()) {
            return@withContext _questionsFlow.value
        }

        val loaded = questionSource.loadAllQuestions()
        _questionsFlow.value = loaded

        // Populate / update Room searchable index in background
        try {
            val entities = loaded.map { q ->
                QuestionIndexEntity(
                    id = q.id,
                    title = q.title,
                    description = q.description,
                    difficulty = q.difficulty.name,
                    tags = q.tags.joinToString(","),
                    contentVersion = q.contentVersion,
                    addedAt = q.addedAt,
                    sourceLayer = if (questionSource.hasInternalFile(q.id)) "INTERNAL_STORAGE" else "ASSETS",
                    file = "${q.id}.json"
                )
            }
            indexDao.upsertAllIndex(entities)
        } catch (_: Exception) {}

        loaded
    }

    /**
     * Resolves a single Question by ID (Layer 1 -> Layer 2).
     */
    suspend fun getQuestion(id: String): Question? = withContext(Dispatchers.IO) {
        _questionsFlow.value.find { it.id == id } ?: questionSource.loadQuestion(id)
    }

    /**
     * Performs remote synchronization from GitHub + jsDelivr CDN.
     * Guaranteed never to touch progress tables.
     */
    suspend fun syncQuestions(isManual: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        val result = syncManager.sync(isManual)
        if (result is SyncResult.Success) {
            // Reload all questions and emit through StateFlow
            val reloaded = questionSource.loadAllQuestions()
            _questionsFlow.value = reloaded
        }
        result
    }

    suspend fun dismissNewQuestionsBanner() {
        userPreferences.setNewQuestionsBannerCount(0)
    }

    suspend fun getDraft(questionId: String): String? = withContext(Dispatchers.IO) {
        draftDao.getDraftSync(questionId)?.queryText
    }

    suspend fun saveDraft(questionId: String, queryText: String) = withContext(Dispatchers.IO) {
        draftDao.upsertDraft(
            QueryDraftEntity(
                questionId = questionId,
                queryText = queryText,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markSolutionViewed(questionId: String) = withContext(Dispatchers.IO) {
        val existing = progressDao.getProgressSync(questionId)
        val updated = (existing ?: QuestionProgressEntity(questionId = questionId)).copy(
            solutionViewed = true
        )
        progressDao.upsertProgress(updated)
    }

    suspend fun recordAttempt(questionId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val today = getTodayDateString()

        database.withTransaction {
            val progress = progressDao.getProgressSync(questionId)
            val updatedProgress = (progress ?: QuestionProgressEntity(questionId = questionId)).copy(
                lastAttemptedAt = now,
                attemptCount = (progress?.attemptCount ?: 0) + 1
            )
            progressDao.upsertProgress(updatedProgress)

            val activity = activityDao.getActivitySync(today)
            val updatedActivity = (activity ?: DailyActivityEntity(date = today)).copy(
                queriesRun = (activity?.queriesRun ?: 0) + 1
            )
            activityDao.upsertActivity(updatedActivity)
        }
    }

    suspend fun recordSolve(questionId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val today = getTodayDateString()

        database.withTransaction {
            val progress = progressDao.getProgressSync(questionId)
            val wasAlreadySolved = progress?.isSolved == true

            if (!wasAlreadySolved) {
                val updatedProgress = (progress ?: QuestionProgressEntity(questionId = questionId)).copy(
                    isSolved = true,
                    firstSolvedAt = progress?.firstSolvedAt ?: now,
                    lastAttemptedAt = now
                )
                progressDao.upsertProgress(updatedProgress)

                val activity = activityDao.getActivitySync(today)
                val updatedActivity = (activity ?: DailyActivityEntity(date = today)).copy(
                    questionsSolved = (activity?.questionsSolved ?: 0) + 1
                )
                activityDao.upsertActivity(updatedActivity)
            }
        }
    }

    fun setLastOpenedQuestionId(questionId: String) {
        prefs.edit().putString("last_opened_id", questionId).apply()
    }

    fun getLastOpenedQuestionId(): String? {
        return prefs.getString("last_opened_id", null)
    }

    suspend fun getSolvedCount(): Int = withContext(Dispatchers.IO) {
        progressDao.getSolvedCount()
    }

    suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
        database.withTransaction {
            progressDao.clearAll()
            draftDao.clearAll()
            activityDao.clearAll()
        }
        prefs.edit().remove("last_opened_id").apply()
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
