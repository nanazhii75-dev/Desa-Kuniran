package com.desa.kuniran.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.User
import com.desa.kuniran.core.security.GoogleSignInManager
import com.desa.kuniran.core.security.PhoneProtector
import com.desa.kuniran.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val step: AuthStep = AuthStep.PHONE_INPUT,
    val phoneInput: String = "",
    val normalizedPhonePreview: String = "",
    val otpInput: String = "",
    val nameInput: String = "",
    val cooldownSeconds: Int = 0,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null
)

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFICATION,
    PROFILE_SETUP,
    AUTHENTICATED
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val phoneProtector: PhoneProtector
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.update { it.copy(currentUser = user, step = AuthStep.AUTHENTICATED) }
                }
            }
        }
    }

    fun onPhoneChanged(newPhone: String) {
        val normalized = phoneProtector.normalize(newPhone)
        _uiState.update {
            it.copy(
                phoneInput = newPhone,
                normalizedPhonePreview = normalized,
                errorMessage = null
            )
        }
    }

    fun onOtpChanged(newOtp: String) {
        if (newOtp.length <= 6) {
            _uiState.update { it.copy(otpInput = newOtp, errorMessage = null) }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(nameInput = newName, errorMessage = null) }
    }

    fun requestOtp() {
        val phone = _uiState.value.phoneInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.requestOtp(phone)) {
                is AppResult.Success -> {
                    val cooldown = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = AuthStep.OTP_VERIFICATION,
                            cooldownSeconds = cooldown,
                            otpInput = ""
                        )
                    }
                    startCooldownTimer(cooldown)
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun autofillSimulatedSmsOtp() {
        // Kemudahan warga lansia/non-tech-savvy (Blueprint Bagian 12 autofill OTP)
        _uiState.update { it.copy(otpInput = "123456") }
    }

    fun verifyOtp() {
        val phone = _uiState.value.phoneInput
        val otp = _uiState.value.otpInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.verifyOtp(phone, otp)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = AuthStep.PROFILE_SETUP
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun completeProfile(onSuccess: () -> Unit) {
        val name = _uiState.value.nameInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.updateProfile(name)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = result.data,
                            step = AuthStep.AUTHENTICATED
                        )
                    }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun signInWithGoogleViaCredentialManager(
        context: Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
            val manager = GoogleSignInManager(context)
            when (val credentialResult = manager.signIn()) {
                is AppResult.Success -> {
                    val data = credentialResult.data
                    when (val authResult = authRepository.signInWithGoogle(
                        idToken = data.idToken,
                        displayName = data.displayName,
                        email = data.email,
                        photoUrl = data.photoUrl
                    )) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isGoogleLoading = false,
                                    currentUser = authResult.data,
                                    step = AuthStep.AUTHENTICATED
                                )
                            }
                            onSuccess()
                        }
                        is AppResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isGoogleLoading = false,
                                    errorMessage = authResult.error.userMessage
                                )
                            }
                        }
                        AppResult.Loading -> {}
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            errorMessage = credentialResult.error.userMessage
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update {
                AuthUiState(
                    step = AuthStep.PHONE_INPUT,
                    phoneInput = "",
                    normalizedPhonePreview = ""
                )
            }
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                delay(1000)
                current--
                _uiState.update { it.copy(cooldownSeconds = current) }
            }
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            phoneProtector: PhoneProtector
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(authRepository, phoneProtector) as T
            }
        }
    }
}
