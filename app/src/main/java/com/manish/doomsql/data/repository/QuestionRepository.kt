package com.manish.doomsql.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.manish.doomsql.data.local.DoomSqlDatabase
import com.manish.doomsql.data.local.entity.DailyActivityEntity
import com.manish.doomsql.data.local.entity.QueryDraftEntity
import com.manish.doomsql.data.local.entity.QuestionProgressEntity
import com.manish.doomsql.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuestionRepository(
    private val context: Context,
    private val database: DoomSqlDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val prefs = context.getSharedPreferences("doomsql_prefs", Context.MODE_PRIVATE)

    private var cachedQuestions: List<Question>? = null

    private val progressDao = database.questionProgressDao()
    private val draftDao = database.queryDraftDao()
    private val activityDao = database.dailyActivityDao()

    val progressMapFlow: Flow<Map<String, QuestionProgressEntity>> =
        progressDao.getAllProgress().map { list ->
            list.associateBy { it.questionId }
        }

    val dailyActivitiesFlow: Flow<List<DailyActivityEntity>> =
        activityDao.getAllActivities()

    suspend fun getQuestions(): List<Question> = withContext(Dispatchers.IO) {
        cachedQuestions?.let { return@withContext it }
        val loaded = loadQuestionsFromAssets()
        cachedQuestions = loaded
        loaded
    }

    suspend fun getQuestion(id: String): Question? = withContext(Dispatchers.IO) {
        getQuestions().find { it.id == id }
    }

    private fun loadQuestionsFromAssets(): List<Question> {
        val assetManager = context.assets
        val indexJson = assetManager.open("questions/index.json").bufferedReader().use { it.readText() }
        val fileNames: List<String> = json.decodeFromString(indexJson)

        return fileNames.map { fileName ->
            val content = assetManager.open("questions/$fileName").bufferedReader().use { it.readText() }
            json.decodeFromString<Question>(content)
        }
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
