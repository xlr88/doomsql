package com.manish.doomsql.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.manish.doomsql.data.engine.QuestionSelfCheck
import com.manish.doomsql.data.engine.SandboxSqlEngine
import com.manish.doomsql.data.engine.SqlExecutionEngine
import com.manish.doomsql.data.local.DoomSqlDatabase
import com.manish.doomsql.data.repository.QuestionRepository
import com.manish.doomsql.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface AppContainer {
    val repository: QuestionRepository
    val sqlEngine: SqlExecutionEngine
    val userPreferences: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val database: DoomSqlDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            DoomSqlDatabase::class.java,
            "doomsql.db"
        ).fallbackToDestructiveMigration().build()
    }

    override val repository: QuestionRepository by lazy {
        QuestionRepository(context.applicationContext, database)
    }

    override val sqlEngine: SqlExecutionEngine by lazy {
        SandboxSqlEngine()
    }

    override val userPreferences: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.applicationContext)
    }

    init {
        // Debug-only self-check that runs at app start
        applicationScope.launch {
            try {
                val questions = repository.getQuestions()
                QuestionSelfCheck.runCheck(questions, sqlEngine)
            } catch (e: Exception) {
                Log.e("DoomSQL", "Self-check initialization error", e)
            }
        }
    }
}
