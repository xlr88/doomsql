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

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val KEY_REVIEW_PROMPTED = booleanPreferencesKey("has_prompted_review")
        const val DEFAULT_WEEKLY_GOAL = 10
    }

    val weeklyGoalFlow: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[KEY_WEEKLY_GOAL] ?: DEFAULT_WEEKLY_GOAL
    }

    val hasPromptedReviewFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[KEY_REVIEW_PROMPTED] ?: false
    }

    suspend fun setWeeklyGoal(goal: Int) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_WEEKLY_GOAL] = goal
        }
    }

    suspend fun markReviewPrompted() {
        context.userDataStore.edit { preferences ->
            preferences[KEY_REVIEW_PROMPTED] = true
        }
    }
}
