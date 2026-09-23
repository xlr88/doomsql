package com.manish.doomsql

import android.content.Context
import com.manish.doomsql.data.model.AuthUser
import com.manish.doomsql.data.repository.AuthRepository
import com.manish.doomsql.data.repository.AuthResult
import com.manish.doomsql.ui.screens.auth.SignInViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    var errorMessage: String = "Operation failed"

    override suspend fun signInWithGoogle(context: Context): AuthResult<AuthUser> {
        return if (shouldSucceed) {
            val user = AuthUser("g123", "google@example.com", "Google User", null, true)
            _currentUser.value = user
            AuthResult.Success(user)
        } else {
            AuthResult.Error(errorMessage)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult<AuthUser> {
        return if (shouldSucceed) {
            val user = AuthUser("e123", email, null, null, false)
            _currentUser.value = user
            AuthResult.Success(user)
        } else {
            AuthResult.Error(errorMessage)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult<AuthUser> {
        return if (shouldSucceed) {
            val user = AuthUser("e123", email, null, null, false)
            _currentUser.value = user
            AuthResult.Success(user)
        } else {
            AuthResult.Error(errorMessage)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        return if (shouldSucceed) AuthResult.Success(Unit) else AuthResult.Error(errorMessage)
    }

    override suspend fun sendEmailVerification(): AuthResult<Unit> {
        return if (shouldSucceed) AuthResult.Success(Unit) else AuthResult.Error(errorMessage)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override suspend fun deleteAccount(
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        viewModel = SignInViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertFalse(state.isSignUpMode)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("", state.email)
        assertEquals("", state.password)
    }

    @Test
    fun testToggleMode() {
        assertFalse(viewModel.uiState.value.isSignUpMode)
        viewModel.toggleMode()
        assertTrue(viewModel.uiState.value.isSignUpMode)
        viewModel.toggleMode()
        assertFalse(viewModel.uiState.value.isSignUpMode)
    }

    @Test
    fun testEmptyEmailValidation() = runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("password123")
        var calledSuccess = false
        viewModel.submitEmailAuth { calledSuccess = true }
        advanceUntilIdle()

        assertFalse(calledSuccess)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("email", ignoreCase = true))
    }

    @Test
    fun testShortPasswordSignUpValidation() = runTest {
        viewModel.toggleMode() // sign up mode
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")

        var calledSuccess = false
        viewModel.submitEmailAuth { calledSuccess = true }
        advanceUntilIdle()

        assertFalse(calledSuccess)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("6 characters", ignoreCase = true))
    }

    @Test
    fun testPasswordMismatchSignUpValidation() = runTest {
        viewModel.toggleMode() // sign up mode
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("different123")

        var calledSuccess = false
        viewModel.submitEmailAuth { calledSuccess = true }
        advanceUntilIdle()

        assertFalse(calledSuccess)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("match", ignoreCase = true))
    }

    @Test
    fun testSuccessfulSignIn() = runTest {
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")

        var calledSuccess = false
        viewModel.submitEmailAuth { calledSuccess = true }
        advanceUntilIdle()

        assertTrue(calledSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
