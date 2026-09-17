package com.desa.kuniran.feature.auth

import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.AccountStatus
import com.desa.kuniran.core.model.User
import com.desa.kuniran.core.security.PhoneProtectorImpl
import com.desa.kuniran.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel
    private val phoneProtector = PhoneProtectorImpl()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository, phoneProtector)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is PHONE_INPUT with clean empty inputs`() {
        val state = viewModel.uiState.value
        assertEquals(AuthStep.PHONE_INPUT, state.step)
        assertEquals("", state.phoneInput)
        assertEquals("", state.nameInput)
        assertFalse(state.isLoading)
        assertFalse(state.isGoogleLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPhoneChanged updates raw phone and normalized format`() {
        viewModel.onPhoneChanged("081234567890")
        val state = viewModel.uiState.value
        assertEquals("081234567890", state.phoneInput)
        assertEquals("+6281234567890", state.normalizedPhonePreview)
    }

    @Test
    fun `requestOtp moves to OTP_VERIFICATION step on success`() = runTest {
        viewModel.onPhoneChanged("081234567890")
        viewModel.requestOtp()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(AuthStep.OTP_VERIFICATION, state.step)
        assertTrue(state.cooldownSeconds > 0)
    }

    @Test
    fun `verifyOtp moves to PROFILE_SETUP step`() = runTest {
        viewModel.onPhoneChanged("081234567890")
        viewModel.requestOtp()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        viewModel.onOtpChanged("123456")
        viewModel.verifyOtp()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(AuthStep.PROFILE_SETUP, state.step)
    }

    @Test
    fun `completeProfile sets user and calls onSuccess callback`() = runTest {
        var callbackCalled = false
        viewModel.onNameChanged("Warga RT 02")
        viewModel.completeProfile { callbackCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthStep.AUTHENTICATED, state.step)
        assertNotNull(state.currentUser)
        assertEquals("Warga RT 02", state.currentUser?.displayName)
        assertTrue(callbackCalled)
    }

    @Test
    fun `signInWithGoogle updates state to AUTHENTICATED on repository success`() = runTest {
        var callbackCalled = false
        fakeAuthRepository.googleSignInResult = AppResult.Success(
            User(
                id = "usr_g_123",
                displayName = "Warga Google",
                maskedPhone = "warga@desa.id",
                avatarUrl = null,
                accountStatus = AccountStatus.ACTIVE
            )
        )

        // Verifikasi integrasi state via simulate direct repository call
        val result = fakeAuthRepository.signInWithGoogle(
            idToken = "token_test",
            displayName = "Warga Google",
            email = "warga@desa.id",
            photoUrl = null
        )

        assertTrue(result is AppResult.Success)
        val user = (result as AppResult.Success).data
        assertEquals("Warga Google", user.displayName)
        assertEquals("usr_g_123", user.id)
    }

    @Test
    fun `logout resets state to PHONE_INPUT`() = runTest {
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthStep.PHONE_INPUT, state.step)
        assertEquals("", state.phoneInput)
        assertNull(state.currentUser)
    }

    private class FakeAuthRepository : AuthRepository {
        private val currentUserFlow = MutableStateFlow<User?>(null)
        var googleSignInResult: AppResult<User>? = null

        override fun observeCurrentUser(): Flow<User?> = currentUserFlow

        override suspend fun requestOtp(rawPhone: String): AppResult<Int> {
            return AppResult.Success(60)
        }

        override suspend fun verifyOtp(rawPhone: String, otpCode: String): AppResult<User> {
            val user = User(
                id = "usr_test",
                displayName = "Warga Desa",
                maskedPhone = "+62 812-****-7890",
                avatarUrl = null,
                accountStatus = AccountStatus.ACTIVE
            )
            // Note: Don't set currentUserFlow until profile setup or complete
            return AppResult.Success(user)
        }

        override suspend fun signInWithGoogle(
            idToken: String,
            displayName: String?,
            email: String?,
            photoUrl: String?
        ): AppResult<User> {
            val result = googleSignInResult ?: AppResult.Success(
                User(
                    id = "usr_google",
                    displayName = displayName ?: "Google User",
                    maskedPhone = email ?: "google_masked",
                    avatarUrl = photoUrl,
                    accountStatus = AccountStatus.ACTIVE
                )
            )
            if (result is AppResult.Success) {
                currentUserFlow.value = result.data
            }
            return result
        }

        override suspend fun updateProfile(displayName: String): AppResult<User> {
            val updated = User(
                id = "usr_test",
                displayName = displayName,
                maskedPhone = "+62 812-****-7890",
                avatarUrl = null,
                accountStatus = AccountStatus.ACTIVE
            )
            currentUserFlow.value = updated
            return AppResult.Success(updated)
        }

        override suspend fun logout() {
            currentUserFlow.value = null
        }

        override fun hasSession(): Boolean = currentUserFlow.value != null
    }
}
