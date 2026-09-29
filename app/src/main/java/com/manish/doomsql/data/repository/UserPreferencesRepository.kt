package com.manish.doomsql.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "doomsql_preferences")

open class UserPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val KEY_REVIEW_PROMPTED = booleanPreferencesKey("has_prompted_review")
        val KEY_HAS_COMPLETED_WELCOME = booleanPreferencesKey("has_completed_welcome")
        val KEY_LAST_SYNC_AT = androidx.datastore.preferences.core.longPreferencesKey("last_sync_at")
        val KEY_MANIFEST_VERSION = intPreferencesKey("manifest_version")
        val KEY_NEW_QUESTIONS_BANNER_COUNT = intPreferencesKey("new_questions_banner_count")
        val KEY_IS_SUPPORTER = booleanPreferencesKey("is_supporter")
        val KEY_TIPS_COUNT = intPreferencesKey("tips_count")
        const val DEFAULT_WEEKLY_GOAL = 10
    }

    open val isSupporterFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[KEY_IS_SUPPORTER] ?: false
    }

    open val tipsCountFlow: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[KEY_TIPS_COUNT] ?: 0
    }

    open val weeklyGoalFlow: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[KEY_WEEKLY_GOAL] ?: DEFAULT_WEEKLY_GOAL
    }

    open val hasPromptedReviewFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[KEY_REVIEW_PROMPTED] ?: false
    }

    open val hasCompletedWelcomeFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[KEY_HAS_COMPLETED_WELCOME] ?: false
    }

    open val lastSyncAtFlow: Flow<Long> = context.userDataStore.data.map { preferences ->
        preferences[KEY_LAST_SYNC_AT] ?: 0L
    }

    open val manifestVersionFlow: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[KEY_MANIFEST_VERSION] ?: 1
    }

    open val newQuestionsBannerCountFlow: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[KEY_NEW_QUESTIONS_BANNER_COUNT] ?: 0
    }

    open suspend fun setWeeklyGoal(goal: Int) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_WEEKLY_GOAL] = goal
        }
    }

    open suspend fun markReviewPrompted() {
        context.userDataStore.edit { preferences ->
            preferences[KEY_REVIEW_PROMPTED] = true
        }
    }

    open suspend fun setCompletedWelcome(completed: Boolean = true) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_HAS_COMPLETED_WELCOME] = completed
        }
    }

    open suspend fun setLastSyncAt(timestamp: Long) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_AT] = timestamp
        }
    }

    open suspend fun setManifestVersion(version: Int) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_MANIFEST_VERSION] = version
        }
    }

    open suspend fun setNewQuestionsBannerCount(count: Int) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_NEW_QUESTIONS_BANNER_COUNT] = count
        }
    }

    open suspend fun getLastSyncAt(): Long {
        return lastSyncAtFlow.first()
    }

    open suspend fun getManifestVersion(): Int {
        return manifestVersionFlow.first()
    }

    open suspend fun recordTipSuccess() {
        context.userDataStore.edit { preferences ->
            preferences[KEY_IS_SUPPORTER] = true
            val current = preferences[KEY_TIPS_COUNT] ?: 0
            preferences[KEY_TIPS_COUNT] = current + 1
        }
    }
}
