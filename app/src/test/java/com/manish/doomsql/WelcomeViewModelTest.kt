package com.manish.doomsql

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.manish.doomsql.data.model.AuthUser
import com.manish.doomsql.data.repository.AuthRepository
import com.manish.doomsql.data.repository.AuthResult
import com.manish.doomsql.data.repository.UserPreferencesRepository
import com.manish.doomsql.ui.screens.welcome.WelcomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser
    override val isConfigured: Boolean = true

    var shouldSucceed: Boolean = true
    var errorMessage: String = "Google Sign-In failed"

    override suspend fun signInWithGoogle(context: Context): AuthResult<AuthUser> {
        return if (shouldSucceed) {
            val user = AuthUser("g123", "user@gmail.com", "SQL Master", "https://photo.url", true)
            _currentUser.value = user
            AuthResult.Success(user)
        } else {
            AuthResult.Error(errorMessage)
        }
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override suspend fun deleteAccount(
        context: Context,
        alsoResetLocalProgress: Boolean,
        onResetLocalProgress: suspend () -> Unit
    ): AuthResult<Unit> {
        return if (shouldSucceed) {
            _currentUser.value = null
            if (alsoResetLocalProgress) onResetLocalProgress()
            AuthResult.Success(Unit)
        } else {
            AuthResult.Error(errorMessage)
        }
    }
}

class FakeUserPreferencesRepository(context: Context) : UserPreferencesRepository(context) {
    private val _welcomeFlow = MutableStateFlow(false)
    override val hasCompletedWelcomeFlow: Flow<Boolean> = _welcomeFlow

    override suspend fun setCompletedWelcome(completed: Boolean) {
        _welcomeFlow.value = completed
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WelcomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var fakeUserPreferences: FakeUserPreferencesRepository
    private lateinit var viewModel: WelcomeViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        fakeRepo = FakeAuthRepository()
        fakeUserPreferences = FakeUserPreferencesRepository(context)
        viewModel = WelcomeViewModel(fakeRepo, fakeUserPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertFalse(state.isGoogleLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun testPracticeOfflineMarksWelcomeCompleted() = runTest {
        var onCompleteCalled = false
        viewModel.practiceOffline {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertTrue(onCompleteCalled)
        assertTrue(fakeUserPreferences.hasCompletedWelcomeFlow.first())
    }

    @Test
    fun testSkipAffordanceMarksWelcomeCompleted() = runTest {
        var onCompleteCalled = false
        viewModel.skip {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertTrue(onCompleteCalled)
        assertTrue(fakeUserPreferences.hasCompletedWelcomeFlow.first())
    }

    @Test
    fun testContinueWithGoogleSuccess() = runTest {
        var onCompleteCalled = false
        fakeRepo.shouldSucceed = true

        viewModel.continueWithGoogle(context) {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertTrue(onCompleteCalled)
        assertFalse(viewModel.uiState.value.isGoogleLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(fakeUserPreferences.hasCompletedWelcomeFlow.first())
    }

    @Test
    fun testContinueWithGoogleError() = runTest {
        var onCompleteCalled = false
        fakeRepo.shouldSucceed = false
        fakeRepo.errorMessage = "Network connection unavailable"

        viewModel.continueWithGoogle(context) {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertFalse(onCompleteCalled)
        assertFalse(viewModel.uiState.value.isGoogleLoading)
        assertEquals("Network connection unavailable", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testContinueWithGoogleCancelledDoesNotShowError() = runTest {
        var onCompleteCalled = false
        fakeRepo.shouldSucceed = false
        fakeRepo.errorMessage = "Google Sign-In was cancelled by user."

        viewModel.continueWithGoogle(context) {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertFalse(onCompleteCalled)
        assertFalse(viewModel.uiState.value.isGoogleLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testDismissError() {
        fakeRepo.shouldSucceed = false
        fakeRepo.errorMessage = "Failed"
        viewModel.continueWithGoogle(context) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed", viewModel.uiState.value.errorMessage)
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
