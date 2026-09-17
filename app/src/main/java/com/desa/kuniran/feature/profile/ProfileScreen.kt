package com.desa.kuniran.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desa.kuniran.core.designsystem.GreenDark
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.HarvestGold
import com.desa.kuniran.core.designsystem.StatusError
import com.desa.kuniran.core.designsystem.StatusSuccess
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.ui.VillageTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Profil & Identitas Warga Desa Kuniran.
 * Menampilkan Kartu Identitas Digital Warga yang tersimpan di Cloud Firestore
 * dan menyediakan fitur ubah data profil secara real-time.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val member = state.memberProfile
    val user = state.user
    val displayName = member?.displayName ?: user?.displayName ?: "Warga Desa Kuniran"
    val maskedPhone = member?.maskedPhone ?: user?.maskedPhone ?: "08•• •••• ••••"
    val roleName = when (member?.role) {
        MemberRole.ADMIN -> "Pengurus RT (Ketua)"
        MemberRole.TREASURER -> "Pengurus Kas (Bendahara)"
        MemberRole.SECRETARY -> "Sekretaris RT"
        else -> "Warga Terdaftar"
    }

    Scaffold(
        topBar = {
            VillageTopAppBar(
                title = "Profil & Identitas Warga",
                subtitle = "Terdaftar di Cloud Firestore Desa Kuniran"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("profile_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feedback notification message
            state.notificationMessage?.let { msg ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissNotification() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = GreenPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1. Digital Citizen ID Card (Kartu Tanda Anggota Warga Digital)
            item {
                CitizenDigitalIdCard(
                    displayName = displayName,
                    maskedPhone = maskedPhone,
                    roleName = roleName,
                    isSyncing = state.isSyncingWithFirestore,
                    lastSync = state.lastSyncTimestamp,
                    onEditClick = { viewModel.onOpenEditDialog() },
                    onSyncClick = { viewModel.syncFromFirestore() }
                )
            }

            // 2. Cloud Firestore Status Banner
            item {
                FirestoreSyncStatusCard(
                    isSyncing = state.isSyncingWithFirestore,
                    lastSync = state.lastSyncTimestamp,
                    onManualSync = { viewModel.syncFromFirestore() }
                )
            }

            // 3. Security & Privacy Info (Blueprint Bagian 11 & 37)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Keamanan Identitas Warga",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Data identitas Anda tersimpan di Cloud Firestore & diamankan dengan enkripsi standar pemerintah desa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GreenPrimary
                            )
                        }
                    }
                }
            }

            // 4. Informasi Lingkungan & Layanan
            item {
                Text(
                    text = "Bantuan & Kontak Lingkungan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                CitizenInfoRowCard(
                    title = "Pengurus RT 02 / RW 04",
                    subtitle = "Dusun Jogorejo, Desa Kuniran",
                    icon = Icons.Default.LocationOn
                )
            }

            item {
                CitizenInfoRowCard(
                    title = "Pusat Pengaduan & Layanan Warga",
                    subtitle = "Layanan tanggap cepat perangkat desa",
                    icon = Icons.Default.Call
                )
            }

            // Pengaturan Tampilan Tema Terang / Gelap
            item {
                Text(
                    text = "Preferensi Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_theme_toggle"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkMode) HarvestGold.copy(alpha = 0.2f) else GreenLight,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (isDarkMode) HarvestGold else GreenPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isDarkMode) "Mode Gelap (Malam)" else "Mode Terang (Siang)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Beralih antara tema terang dan gelap",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GreenPrimary
                            ),
                            modifier = Modifier.testTag("switch_dark_mode")
                        )
                    }
                }
            }

            item {
                CitizenInfoRowCard(
                    title = "Tentang Aplikasi Desa Kuniran",
                    subtitle = "Versi 1.0 • Gotong Royong Warga Jogorejo",
                    icon = Icons.Default.Info
                )
            }

            // 5. Tombol Keluar Akun (Logout)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_logout"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Keluar Akun",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keluar dari Akun",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Modal Sheet Ubah Profil Warga
    if (state.showEditDialog) {
        EditCitizenProfileBottomSheet(
            displayName = state.editDisplayName,
            phone = state.editPhone,
            role = state.editRole,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onNameChange = viewModel::onDisplayNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onRoleChange = viewModel::onRoleChange,
            onDismiss = viewModel::onDismissEditDialog,
            onSubmit = viewModel::saveAndSyncProfileToFirestore
        )
    }
}

/**
 * Kartu Identitas Digital Warga Desa Kuniran (Digital Citizen Card).
 */
@Composable
private fun CitizenDigitalIdCard(
    displayName: String,
    maskedPhone: String,
    roleName: String,
    isSyncing: Boolean,
    lastSync: Long?,
    onEditClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("citizen_digital_id_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(GreenPrimary, GreenDark)
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Kartu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KARTU WARGA DIGITAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HarvestGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Desa Kuniran • RT 02 / RW 04",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code Warga",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar & Nama Warga
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = maskedPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = roleName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons on Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_edit_profile"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ubah Profil",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_sync_profile"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncing) "Sinkron..." else "Sinkron Awan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu status sinkronisasi Cloud Firestore.
 */
@Composable
private fun FirestoreSyncStatusCard(
    isSyncing: Boolean,
    lastSync: Long?,
    onManualSync: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
    val syncText = if (lastSync != null) {
        "Terakhir disinkronkan: ${dateFormat.format(Date(lastSync))}"
    } else {
        "Terhubung dengan Cloud Firestore Desa"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Cloud Firestore Terdaftar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = syncText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onManualSync,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sinkronkan",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Baris item informasi warga.
 */
@Composable
private fun CitizenInfoRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bottom Sheet Ubah Profil Warga dengan Sinkronisasi Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCitizenProfileBottomSheet(
    displayName: String,
    phone: String,
    role: MemberRole,
    isSaving: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRoleChange: (MemberRole) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("edit_citizen_profile_sheet")
        ) {
            Text(
                text = "Ubah Data Profil Warga",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Perubahan akan otomatis disimpan dan disinkronkan ke Cloud Firestore Desa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nama Lengkap Warga
            OutlinedTextField(
                value = displayName,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap Warga") },
                placeholder = { Text("Contoh: Warga RT 02") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_edit_name"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Nomor HP
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Nomor Handphone / WhatsApp") },
                placeholder = { Text("Contoh: 081234567890") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_edit_phone"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Peran Komunitas (Dropdown)
            ExposedDropdownMenuBox(
                expanded = roleDropdownExpanded,
                onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = when (role) {
                        MemberRole.ADMIN -> "Ketua RT / Admin"
                        MemberRole.TREASURER -> "Bendahara Kas"
                        MemberRole.SECRETARY -> "Sekretaris RT"
                        MemberRole.OWNER -> "Ketua RW / Tokoh Desa"
                        MemberRole.MODERATOR -> "Koordinator Wilayah"
                        else -> "Warga Anggota"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Peran di Komunitas") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = roleDropdownExpanded,
                    onDismissRequest = { roleDropdownExpanded = false }
                ) {
                    MemberRole.entries.forEach { r ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (r) {
                                        MemberRole.ADMIN -> "Ketua RT / Admin"
                                        MemberRole.TREASURER -> "Bendahara Kas"
                                        MemberRole.SECRETARY -> "Sekretaris RT"
                                        MemberRole.OWNER -> "Ketua RW / Tokoh Desa"
                                        MemberRole.MODERATOR -> "Koordinator Wilayah"
                                        else -> "Warga Anggota"
                                    }
                                )
                            },
                            onClick = {
                                onRoleChange(r)
                                roleDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .weight(1.6f)
                        .testTag("btn_save_profile_firestore"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Menyimpan...")
                    } else {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan & Sinkron", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Kartu menu layanan untuk direktori layanan warga.
 */
@Composable
fun ServiceMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
