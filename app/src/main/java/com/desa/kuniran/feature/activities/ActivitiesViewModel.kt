package com.desa.kuniran.feature.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.ActivityItem
import com.desa.kuniran.core.model.ParticipantStatus
import com.desa.kuniran.data.repository.ActivityRepository
import com.desa.kuniran.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val activities: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val newTitle: String = "",
    val newDescription: String = "",
    val newDateText: String = "",
    val newLocation: String = "",
    val errorMessage: String? = null
)

class ActivitiesViewModel(
    private val activityRepository: ActivityRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivitiesUiState(isLoading = true))
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()

    private var activeGroupId: String = "group_rt02"

    init {
        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { groupId ->
                activeGroupId = groupId
                loadActivities(groupId)
            }
        }
    }

    private fun loadActivities(groupId: String) {
        viewModelScope.launch {
            activityRepository.observeActivities(groupId).collect { list ->
                _uiState.update { it.copy(activities = list, isLoading = false) }
            }
        }
    }

    fun onOpenAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                newTitle = "",
                newDescription = "",
                newDateText = "",
                newLocation = "",
                errorMessage = null
            )
        }
    }

    fun onDismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(newTitle = title) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(newDescription = desc) }
    }

    fun onDateTextChange(date: String) {
        _uiState.update { it.copy(newDateText = date) }
    }

    fun onLocationChange(loc: String) {
        _uiState.update { it.copy(newLocation = loc) }
    }

    fun submitActivity() {
        val state = _uiState.value
        if (state.newTitle.isBlank() || state.newDateText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama kegiatan dan jadwal wajib diisi.") }
            return
        }

        viewModelScope.launch {
            when (val result = activityRepository.createActivity(
                groupId = activeGroupId,
                title = state.newTitle,
                description = state.newDescription,
                dateText = state.newDateText,
                location = state.newLocation.ifBlank { "Lingkungan RT 02" }
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(showAddDialog = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.userMessage) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun onRsvpChanged(activityId: String, status: ParticipantStatus) {
        viewModelScope.launch {
            activityRepository.rsvpActivity(activityId, status)
            _uiState.update { current ->
                val updated = current.activities.map {
                    if (it.id == activityId) it.copy(userStatus = status) else it
                }
                current.copy(activities = updated)
            }
        }
    }

    companion object {
        fun provideFactory(
            activityRepository: ActivityRepository,
            groupRepository: GroupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ActivitiesViewModel(activityRepository, groupRepository) as T
            }
        }
    }
}
