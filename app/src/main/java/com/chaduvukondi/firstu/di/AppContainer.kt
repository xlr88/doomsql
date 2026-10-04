package com.chaduvukondi.firstu.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.chaduvukondi.firstu.data.billing.BillingManager
import com.chaduvukondi.firstu.data.engine.QuestionSelfCheck
import com.chaduvukondi.firstu.data.engine.SandboxSqlEngine
import com.chaduvukondi.firstu.data.engine.SqlExecutionEngine
import com.chaduvukondi.firstu.data.local.DoomSqlDatabase
import com.chaduvukondi.firstu.data.repository.AuthRepository
import com.chaduvukondi.firstu.data.repository.FirebaseAuthRepository
import com.chaduvukondi.firstu.data.repository.QuestionRepository
import com.chaduvukondi.firstu.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface AppContainer {
    val repository: QuestionRepository
    val sqlEngine: SqlExecutionEngine
    val userPreferences: UserPreferencesRepository
    val authRepository: AuthRepository
    val billingManager: BillingManager
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

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(context.applicationContext)
    }

    override val billingManager: BillingManager by lazy {
        BillingManager(context.applicationContext, userPreferences).apply {
            startConnection()
        }
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
