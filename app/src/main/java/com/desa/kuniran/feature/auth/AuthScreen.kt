package com.desa.kuniran.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desa.kuniran.R
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.StatusError
import com.desa.kuniran.core.ui.PrimaryLargeButton
import com.desa.kuniran.core.ui.SecondaryLargeButton

/**
 * Layar Autentikasi Utama Desa Kuniran.
 * Sesuai Blueprint:
 * - Mengintegrasikan Google Sign-In via AndroidX Credential Manager & Firebase Auth.
 * - Menyediakan alur alternatif SMS OTP untuk inklusi warga lansia / non-Google.
 * - Sepenuhnya reaktif tersambung ke AuthViewModel -> AuthRepository -> Firebase Auth / TokenStorage.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emblem Resmi Desa Kuniran
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_fg),
                    contentDescription = "Logo Resmi Desa Kuniran",
                    modifier = Modifier.size(68.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Desa Kuniran",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Portal Komunitas Warga & Pengurus RT/RW",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card Form Autentikasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state.step) {
                        AuthStep.PHONE_INPUT -> {
                            AuthenticationOptionsStep(
                                phone = state.phoneInput,
                                normalizedPreview = state.normalizedPhonePreview,
                                isLoading = state.isLoading,
                                isGoogleLoading = state.isGoogleLoading,
                                errorMessage = state.errorMessage,
                                onPhoneChange = viewModel::onPhoneChanged,
                                onPhoneSubmit = viewModel::requestOtp,
                                onGoogleSignInClick = {
                                    viewModel.signInWithGoogleViaCredentialManager(
                                        context = context,
                                        onSuccess = onAuthSuccess
                                    )
                                }
                            )
                        }
                        AuthStep.OTP_VERIFICATION -> {
                            OtpVerificationStep(
                                phone = state.phoneInput,
                                otp = state.otpInput,
                                cooldownSeconds = state.cooldownSeconds,
                                isLoading = state.isLoading,
                                errorMessage = state.errorMessage,
                                onOtpChange = viewModel::onOtpChanged,
                                onAutofill = viewModel::autofillSimulatedSmsOtp,
                                onResend = viewModel::requestOtp,
                                onSubmit = viewModel::verifyOtp,
                                onBackToOptions = viewModel::logout
                            )
                        }
                        AuthStep.PROFILE_SETUP -> {
                            ProfileSetupStep(
                                name = state.nameInput,
                                isLoading = state.isLoading,
                                errorMessage = state.errorMessage,
                                onNameChange = viewModel::onNameChanged,
                                onSubmit = { viewModel.completeProfile(onAuthSuccess) }
                            )
                        }
                        AuthStep.AUTHENTICATED -> {
                            onAuthSuccess()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Catatan Keamanan Warga (Blueprint Bagian 11 & 30)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GreenLight.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Data login dan nomor HP diproteksi enkripsi Keystore dan verifikasi identitas resmi warga.",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Langkah Pilihan Autentikasi:
 * 1. Google Sign-In modern dengan AndroidX Credential Manager & Firebase Auth.
 * 2. Opsi alternatif Masuk dengan Nomor Handphone (SMS OTP).
 */
@Composable
private fun AuthenticationOptionsStep(
    phone: String,
    normalizedPreview: String,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    errorMessage: String?,
    onPhoneChange: (String) -> Unit,
    onPhoneSubmit: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    Text(
        text = "Pintu Masuk Warga",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Gunakan akun Google untuk masuk instan atau gunakan nomor HP yang terdaftar.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Tombol Google Sign-In dengan Credential Manager
    GoogleSignInButton(
        isLoading = isGoogleLoading,
        onClick = onGoogleSignInClick
    )

    Spacer(modifier = Modifier.height(22.dp))

    // Pemisah Visual Elegan
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "ATAU NOMOR HP",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Input Nomor Handphone
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Nomor Handphone Warga") },
        placeholder = { Text("Contoh: 081234567890") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = GreenPrimary)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_input_field"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenPrimary,
            focusedLabelColor = GreenPrimary
        )
    )

    if (normalizedPreview.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Format internasional: $normalizedPreview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = StatusError.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = StatusError,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    PrimaryLargeButton(
        text = "Kirim Kode Masuk (SMS)",
        onClick = onPhoneSubmit,
        isLoading = isLoading,
        enabled = phone.isNotBlank() && !isGoogleLoading,
        icon = Icons.Default.Sms,
        modifier = Modifier.testTag("submit_phone_button")
    )
}

/**
 * Komponen Tombol Google Sign-In Modern yang Terintegrasi dengan Credential Manager.
 */
@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .testTag("google_sign_in_button"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Masuk dengan Akun Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Akses instan via Credential Manager",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = GreenPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Lanjut Google",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Langkah Verifikasi Kode OTP SMS.
 */
@Composable
private fun OtpVerificationStep(
    phone: String,
    otp: String,
    cooldownSeconds: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onOtpChange: (String) -> Unit,
    onAutofill: () -> Unit,
    onResend: () -> Unit,
    onSubmit: () -> Unit,
    onBackToOptions: () -> Unit
) {
    Text(
        text = "Verifikasi Kode Masuk",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Kode 6 digit telah dikirimkan ke nomor $phone",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedTextField(
        value = otp,
        onValueChange = onOtpChange,
        label = { Text("Kode OTP (6 Digit)") },
        placeholder = { Text("123456") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GreenPrimary)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("otp_input_field"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenPrimary,
            focusedLabelColor = GreenPrimary
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Tombol autofill ramah warga lansia
    SecondaryLargeButton(
        text = "Isi Otomatis dari SMS",
        onClick = onAutofill,
        icon = Icons.Default.Sms,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("autofill_otp_button")
    )

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = StatusError,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    PrimaryLargeButton(
        text = "Verifikasi & Lanjut",
        onClick = onSubmit,
        isLoading = isLoading,
        enabled = otp.length == 6,
        icon = Icons.Default.Check,
        modifier = Modifier.testTag("verify_otp_button")
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (cooldownSeconds > 0) {
        Text(
            text = "Kirim ulang kode dalam $cooldownSeconds detik",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        TextButton(onClick = onResend) {
            Text(
                text = "Kirim Ulang Kode SMS",
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimary
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    TextButton(onClick = onBackToOptions) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pilih Cara Masuk Lain",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Langkah Melengkapi Profil Nama Warga untuk Pengguna Baru.
 */
@Composable
private fun ProfileSetupStep(
    name: String,
    isLoading: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Text(
        text = "Lengkapi Nama Warga",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Tuliskan nama lengkap agar dikenali oleh tetangga dan pengurus RT/RW Desa Kuniran.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Nama Lengkap Warga") },
        placeholder = { Text("Masukkan nama Anda") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = GreenPrimary)
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("name_input_field"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenPrimary,
            focusedLabelColor = GreenPrimary
        )
    )

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = StatusError,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    PrimaryLargeButton(
        text = "Mulai Masuk Aplikasi",
        onClick = onSubmit,
        isLoading = isLoading,
        enabled = name.isNotBlank(),
        icon = Icons.Default.Check,
        modifier = Modifier.testTag("complete_profile_button")
    )
}
