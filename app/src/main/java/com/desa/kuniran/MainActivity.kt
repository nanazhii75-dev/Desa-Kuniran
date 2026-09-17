package com.desa.kuniran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desa.kuniran.core.designsystem.DesaKuniranTheme
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.ui.VillageTopAppBar
import com.desa.kuniran.feature.activities.ActivitiesScreen
import com.desa.kuniran.feature.activities.ActivitiesViewModel
import com.desa.kuniran.feature.announcements.AnnouncementsScreen
import com.desa.kuniran.feature.announcements.AnnouncementsViewModel
import com.desa.kuniran.feature.auth.AuthScreen
import com.desa.kuniran.feature.auth.AuthStep
import com.desa.kuniran.feature.auth.AuthViewModel
import com.desa.kuniran.feature.complaints.ComplaintsScreen
import com.desa.kuniran.feature.complaints.ComplaintsViewModel
import com.desa.kuniran.feature.environment.EnvironmentScreen
import com.desa.kuniran.feature.finance.FinanceScreen
import com.desa.kuniran.feature.finance.FinanceViewModel
import com.desa.kuniran.feature.home.HomeScreen
import com.desa.kuniran.feature.home.HomeViewModel
import com.desa.kuniran.feature.members.MemberListScreen
import com.desa.kuniran.feature.members.MembersViewModel
import com.desa.kuniran.feature.notifications.NotificationsScreen
import com.desa.kuniran.feature.profile.ProfileScreen
import com.desa.kuniran.feature.profile.ProfileViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DesaKuniranApplication
        val container = app.container

        setContent {
            val isDarkMode by container.themeManager.isDarkMode.collectAsState()
            DesaKuniranTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.provideFactory(
                        container.authRepository,
                        container.phoneProtector
                    )
                )
                val authState by authViewModel.uiState.collectAsState()

                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        container.authRepository,
                        container.groupRepository,
                        container.announcementRepository,
                        container.activityRepository,
                        container.financeRepository,
                        container.villageFirestoreRepository
                    )
                )

                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.provideFactory(
                        container.authRepository,
                        container.memberRepository,
                        container.groupRepository,
                        container.villageFirestoreRepository
                    )
                )

                val financeViewModel: FinanceViewModel = viewModel(
                    factory = FinanceViewModel.provideFactory(
                        container.financeRepository,
                        container.groupRepository
                    )
                )

                val activitiesViewModel: ActivitiesViewModel = viewModel(
                    factory = ActivitiesViewModel.provideFactory(
                        container.activityRepository,
                        container.groupRepository
                    )
                )

                val membersViewModel: MembersViewModel = viewModel(
                    factory = MembersViewModel.provideFactory(
                        container.memberRepository,
                        container.groupRepository,
                        container.phoneProtector
                    )
                )

                val announcementsViewModel: AnnouncementsViewModel = viewModel(
                    factory = AnnouncementsViewModel.provideFactory(
                        container.announcementRepository,
                        container.groupRepository
                    )
                )

                val complaintsViewModel: ComplaintsViewModel = viewModel(
                    factory = ComplaintsViewModel.provideFactory(
                        container.complaintRepository,
                        container.groupRepository
                    )
                )

                var currentTab by remember { mutableStateOf(NavTab.HOME) }
                var subScreen by remember { mutableStateOf<SubScreen?>(null) }

                if (authState.step != AuthStep.AUTHENTICATED && authState.currentUser == null) {
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthSuccess = {
                            currentTab = NavTab.HOME
                            subScreen = null
                        }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            if (subScreen == null) {
                                VillageBottomNavigation(
                                    currentTab = currentTab,
                                    onTabSelect = { tab ->
                                        currentTab = tab
                                        subScreen = null
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = subScreen to currentTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "ScreenTransition"
                            ) { (activeSubScreen, activeTab) ->
                                when {
                                    activeSubScreen == SubScreen.ANNOUNCEMENTS -> {
                                        AnnouncementsScreen(
                                            viewModel = announcementsViewModel,
                                            onBackClick = { subScreen = null }
                                        )
                                    }
                                    activeSubScreen == SubScreen.MEMBERS -> {
                                        MemberListScreen(
                                            viewModel = membersViewModel,
                                            onBackClick = { subScreen = null }
                                        )
                                    }
                                    activeSubScreen == SubScreen.COMPLAINTS -> {
                                        ComplaintsScreen(
                                            viewModel = complaintsViewModel,
                                            onBackClick = { subScreen = null }
                                        )
                                    }
                                    activeSubScreen == SubScreen.ENVIRONMENT -> {
                                        EnvironmentScreen(
                                            onBackClick = { subScreen = null }
                                        )
                                    }
                                    activeSubScreen == SubScreen.NOTIFICATIONS -> {
                                        NotificationsScreen(
                                            notificationRepository = container.notificationRepository,
                                            onBackClick = { subScreen = null }
                                        )
                                    }
                                    activeTab == NavTab.HOME -> {
                                        HomeScreen(
                                            viewModel = homeViewModel,
                                            isDarkMode = isDarkMode,
                                            onToggleDarkMode = { container.themeManager.toggleDarkMode() },
                                            onNavigateToActivities = { currentTab = NavTab.ACTIVITIES },
                                            onNavigateToFinance = { currentTab = NavTab.FINANCE },
                                            onNavigateToAnnouncements = { subScreen = SubScreen.ANNOUNCEMENTS },
                                            onNavigateToMembers = { subScreen = SubScreen.MEMBERS },
                                            onNavigateToComplaints = { subScreen = SubScreen.COMPLAINTS },
                                            onNavigateToEnvironment = { subScreen = SubScreen.ENVIRONMENT },
                                            onNavigateToNotifications = { subScreen = SubScreen.NOTIFICATIONS }
                                        )
                                    }
                                    activeTab == NavTab.ACTIVITIES -> {
                                        ActivitiesScreen(
                                            viewModel = activitiesViewModel,
                                            onBackClick = null
                                        )
                                    }
                                    activeTab == NavTab.FINANCE -> {
                                        FinanceScreen(
                                            viewModel = financeViewModel,
                                            onBackClick = null
                                        )
                                    }
                                    activeTab == NavTab.SERVICES -> {
                                        ServicesDirectoryScreen(
                                            onNavigateToAnnouncements = { subScreen = SubScreen.ANNOUNCEMENTS },
                                            onNavigateToMembers = { subScreen = SubScreen.MEMBERS },
                                            onNavigateToComplaints = { subScreen = SubScreen.COMPLAINTS },
                                            onNavigateToEnvironment = { subScreen = SubScreen.ENVIRONMENT }
                                        )
                                    }
                                    activeTab == NavTab.PROFILE -> {
                                        ProfileScreen(
                                            viewModel = profileViewModel,
                                            isDarkMode = isDarkMode,
                                            onToggleDarkMode = { container.themeManager.toggleDarkMode() },
                                            onLogout = { authViewModel.logout() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Default.Home),
    ACTIVITIES("Kegiatan", Icons.Default.Event),
    FINANCE("Kas", Icons.Default.Payment),
    SERVICES("Layanan", Icons.Default.MiscellaneousServices),
    PROFILE("Profil", Icons.Default.Person)
}

enum class SubScreen {
    ANNOUNCEMENTS,
    MEMBERS,
    COMPLAINTS,
    ENVIRONMENT,
    NOTIFICATIONS
}

@Composable
fun VillageBottomNavigation(
    currentTab: NavTab,
    onTabSelect: (NavTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenPrimary,
                    selectedTextColor = GreenPrimary,
                    indicatorColor = GreenLight,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun ServicesDirectoryScreen(
    onNavigateToAnnouncements: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToEnvironment: () -> Unit
) {
    Scaffold(
        topBar = {
            VillageTopAppBar(
                title = "Layanan & Fasilitas Warga",
                subtitle = "Menu Warga RT 02 / RW 04"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Pilih Layanan Lingkungan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            com.desa.kuniran.feature.profile.ServiceMenuCard(
                title = "Pengumuman Warga",
                subtitle = "Informasi resmi lingkungan dari pengurus RT/RW",
                icon = Icons.Default.Campaign,
                onClick = onNavigateToAnnouncements
            )

            Spacer(modifier = Modifier.height(10.dp))

            com.desa.kuniran.feature.profile.ServiceMenuCard(
                title = "Daftar Warga RT 02",
                subtitle = "Informasi anggota dan tetangga lingkungan",
                icon = Icons.Default.Groups,
                onClick = onNavigateToMembers
            )

            Spacer(modifier = Modifier.height(10.dp))

            com.desa.kuniran.feature.profile.ServiceMenuCard(
                title = "Pengaduan & Fasilitas Umum",
                subtitle = "Lapor jalan rusak, lampu mati, atau sampah menumpuk",
                icon = Icons.Default.ReportProblem,
                onClick = onNavigateToComplaints
            )

            Spacer(modifier = Modifier.height(10.dp))

            com.desa.kuniran.feature.profile.ServiceMenuCard(
                title = "Jadwal Sampah & Bank Sampah",
                subtitle = "Informasi pengangkutan sampah & pilah plastik/kardus",
                icon = Icons.Default.Delete,
                onClick = onNavigateToEnvironment
            )
        }
    }
}
