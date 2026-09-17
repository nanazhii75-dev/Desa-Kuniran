package com.desa.kuniran.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.model.ActivityItem
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.CashSummary
import com.desa.kuniran.core.model.Group
import com.desa.kuniran.core.model.Money
import com.desa.kuniran.core.model.ParticipantStatus
import com.desa.kuniran.core.model.User
import com.desa.kuniran.data.repository.ActivityRepository
import com.desa.kuniran.data.repository.AnnouncementRepository
import com.desa.kuniran.data.repository.AuthRepository
import com.desa.kuniran.data.repository.FinanceRepository
import com.desa.kuniran.data.repository.GroupRepository
import com.desa.kuniran.data.repository.VillageFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentUser: User? = null,
    val activeGroup: Group? = null,
    val allGroups: List<Group> = emptyList(),
    val latestAnnouncement: Announcement? = null,
    val announcements: List<Announcement> = emptyList(),
    val upcomingActivity: ActivityItem? = null,
    val cashSummary: CashSummary = CashSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0),
    val isLoading: Boolean = false,
    val showSwitchGroupDialog: Boolean = false,
    val showJoinGroupDialog: Boolean = false,
    val groupCodeInput: String = "",
    val joinErrorMessage: String? = null,
    val joinSuccessMessage: String? = null
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val announcementRepository: AnnouncementRepository,
    private val activityRepository: ActivityRepository,
    private val financeRepository: FinanceRepository,
    private val villageFirestoreRepository: VillageFirestoreRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }

        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { activeId ->
                val active = groupRepository.getActiveGroup()
                _uiState.update { it.copy(activeGroup = active) }
                observeGroupDependentData(activeId)
            }
        }

        viewModelScope.launch {
            groupRepository.observeGroups().collect { groups ->
                _uiState.update { it.copy(allGroups = groups, isLoading = false) }
            }
        }
    }

    private fun observeGroupDependentData(groupId: String) {
        viewModelScope.launch {
            announcementRepository.observeAnnouncements(groupId).collect { list ->
                _uiState.update { 
                    it.copy(
                        announcements = list,
                        latestAnnouncement = list.firstOrNull()
                    ) 
                }
            }
        }

        viewModelScope.launch {
            activityRepository.observeActivities(groupId).collect { list ->
                _uiState.update { it.copy(upcomingActivity = list.firstOrNull()) }
            }
        }

        viewModelScope.launch {
            financeRepository.observeCashSummary(groupId).collect { summary ->
                _uiState.update { it.copy(cashSummary = summary) }
            }
        }
    }

    fun createAnnouncement(title: String, content: String, isPinned: Boolean) {
        val groupId = _uiState.value.activeGroup?.id ?: "group_rt02"
        viewModelScope.launch {
            when (val result = announcementRepository.createAnnouncement(groupId, title, content, isPinned)) {
                is com.desa.kuniran.core.common.AppResult.Success -> {
                    // Sync real-time to Cloud Firestore
                    try {
                        villageFirestoreRepository?.saveAnnouncement(result.data)
                    } catch (e: Throwable) {
                        android.util.Log.w("HomeViewModel", "Firestore sync skipped: ${e.message}")
                    }
                }
                else -> {}
            }
        }
    }

    fun markAnnouncementAsRead(announcementId: String) {
        // Updated local state feedback
    }

    fun onOpenSwitchGroup() {
        _uiState.update { it.copy(showSwitchGroupDialog = true) }
    }

    fun onDismissSwitchGroup() {
        _uiState.update { it.copy(showSwitchGroupDialog = false) }
    }

    fun onSelectGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.selectGroup(groupId)
            _uiState.update { it.copy(showSwitchGroupDialog = false) }
        }
    }

    fun onOpenJoinGroup() {
        _uiState.update {
            it.copy(
                showJoinGroupDialog = true,
                groupCodeInput = "",
                joinErrorMessage = null,
                joinSuccessMessage = null
            )
        }
    }

    fun onDismissJoinGroup() {
        _uiState.update { it.copy(showJoinGroupDialog = false) }
    }

    fun onGroupCodeChange(code: String) {
        _uiState.update { it.copy(groupCodeInput = code, joinErrorMessage = null) }
    }

    fun submitJoinGroupByCode() {
        val code = _uiState.value.groupCodeInput
        viewModelScope.launch {
            when (val result = groupRepository.joinGroupByCode(code)) {
                is com.desa.kuniran.core.common.AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            joinSuccessMessage = "Berhasil bergabung ke group ${result.data.name}!",
                            showJoinGroupDialog = false
                        )
                    }
                }
                is com.desa.kuniran.core.common.AppResult.Error -> {
                    _uiState.update { it.copy(joinErrorMessage = result.error.userMessage) }
                }
                com.desa.kuniran.core.common.AppResult.Loading -> {}
            }
        }
    }

    fun onRsvpChanged(activityId: String, status: ParticipantStatus) {
        viewModelScope.launch {
            activityRepository.rsvpActivity(activityId, status)
            // Update local state directly for responsive feedback
            _uiState.update { current ->
                val updatedAct = current.upcomingActivity?.let {
                    if (it.id == activityId) it.copy(userStatus = status) else it
                }
                current.copy(upcomingActivity = updatedAct)
            }
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            groupRepository: GroupRepository,
            announcementRepository: AnnouncementRepository,
            activityRepository: ActivityRepository,
            financeRepository: FinanceRepository,
            villageFirestoreRepository: VillageFirestoreRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    authRepository,
                    groupRepository,
                    announcementRepository,
                    activityRepository,
                    financeRepository,
                    villageFirestoreRepository
                ) as T
            }
        }
    }
}
