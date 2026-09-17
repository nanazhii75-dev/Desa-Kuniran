package com.desa.kuniran.feature.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desa.kuniran.R
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.HarvestGold
import com.desa.kuniran.feature.home.components.AnnouncementBoardSection
import com.desa.kuniran.feature.home.components.DashboardActivitySummaryCard
import com.desa.kuniran.feature.home.components.DashboardAnnouncementCard
import com.desa.kuniran.feature.home.components.DashboardFinanceAccessCard
import com.desa.kuniran.feature.home.components.DashboardQuickActionsGrid
import com.desa.kuniran.feature.home.components.JoinGroupDialog
import com.desa.kuniran.feature.home.components.SwitchGroupDialog

/**
 * Komponen Dashboard Utama Aplikasi Desa Kuniran.
 * Menampilkan ringkasan kegiatan desa, pengumuman terbaru, dan akses cepat ke fitur keuangan
 * setelah pengguna berhasil login.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onNavigateToActivities: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToEnvironment: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Header Profil Warga & Selector Group Aktif
        item {
            HomeHeaderSection(
                userName = state.currentUser?.displayName ?: "Warga Desa",
                groupName = state.activeGroup?.name ?: "RT 02 / RW 04 Jogorejo",
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onSwitchGroupClick = viewModel::onOpenSwitchGroup,
                onJoinGroupClick = viewModel::onOpenJoinGroup,
                onNotificationsClick = onNavigateToNotifications
            )
        }

        // 2. Banner Identitas Desa Kuniran
        item {
            VillageHeroBanner(groupName = state.activeGroup?.name ?: "RT 02 / RW 04")
        }

        // 3. Akses Cepat ke Fitur Keuangan (Saldo Kas, Status Iuran, Tombol Cepat)
        item {
            DashboardFinanceAccessCard(
                cashSummary = state.cashSummary,
                onNavigateToFinance = onNavigateToFinance
            )
        }

        // 4. Fitur Papan Pengumuman (Announcement Board) Real-time di Layar Utama
        item {
            Spacer(modifier = Modifier.height(10.dp))
            AnnouncementBoardSection(
                announcements = state.announcements,
                onViewAllAnnouncements = onNavigateToAnnouncements,
                onCreateAnnouncement = viewModel::createAnnouncement,
                onMarkAsRead = viewModel::markAnnouncementAsRead
            )
        }

        // 5. Ringkasan Kegiatan Warga Desa Terdekat (Jadwal, Lokasi, RSVP Kehadiran)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            DashboardActivitySummaryCard(
                activity = state.upcomingActivity,
                onRsvpSelected = { status ->
                    state.upcomingActivity?.let { act ->
                        viewModel.onRsvpChanged(act.id, status)
                    }
                },
                onViewAllActivities = onNavigateToActivities
            )
        }

        // 6. Grid Akses Layanan & Modul Desa
        item {
            Spacer(modifier = Modifier.height(14.dp))
            DashboardQuickActionsGrid(
                onAnnouncementsClick = onNavigateToAnnouncements,
                onActivitiesClick = onNavigateToActivities,
                onFinanceClick = onNavigateToFinance,
                onComplaintsClick = onNavigateToComplaints,
                onMembersClick = onNavigateToMembers,
                onEnvironmentClick = onNavigateToEnvironment
            )
        }

        // 7. Jadwal Lingkungan & Kebersihan Sampah
        item {
            Spacer(modifier = Modifier.height(6.dp))
            EnvironmentScheduleSummaryCard(onClick = onNavigateToEnvironment)
        }
    }

    // Dialog Ganti Group Lingkungan RT/RW
    if (state.showSwitchGroupDialog) {
        SwitchGroupDialog(
            groups = state.allGroups,
            activeGroupId = state.activeGroup?.id ?: "",
            onSelect = viewModel::onSelectGroup,
            onDismiss = viewModel::onDismissSwitchGroup,
            onJoinNew = {
                viewModel.onDismissSwitchGroup()
                viewModel.onOpenJoinGroup()
            }
        )
    }

    // Dialog Gabung Group dengan Kode / Undangan
    if (state.showJoinGroupDialog) {
        JoinGroupDialog(
            code = state.groupCodeInput,
            errorMessage = state.joinErrorMessage,
            onCodeChange = viewModel::onGroupCodeChange,
            onSubmit = viewModel::submitJoinGroupByCode,
            onDismiss = viewModel::onDismissJoinGroup
        )
    }
}

@Composable
private fun HomeHeaderSection(
    userName: String,
    groupName: String,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onSwitchGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sugeng Rawuh,",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_toggle_dark_mode")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = if (isDarkMode) "Beralih ke Mode Terang" else "Beralih ke Mode Gelap",
                            tint = if (isDarkMode) HarvestGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onJoinGroupClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_scan_qr_group")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Pindai QR Group",
                            tint = GreenPrimary
                        )
                    }
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Group Switcher Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GreenLight,
                modifier = Modifier
                    .clickable(onClick = onSwitchGroupClick)
                    .testTag("btn_switch_group")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ganti Group",
                        tint = GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VillageHeroBanner(groupName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("hero_village_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GreenPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "DESA KUNIRAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Guyub Rukun, Aman, & Transparan",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pengelolaan kas terbuka dan informasi terpercaya warga $groupName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_fg),
                    contentDescription = "Logo Desa Kuniran",
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun EnvironmentScheduleSummaryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_environment_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Jadwal Angkut Sampah",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Besok Pagi • 06.30 WIB (Pilah Plastik & Kardus)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
