package com.desa.kuniran.feature.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.AnnouncementTemplate
import com.desa.kuniran.data.repository.AnnouncementRepository
import com.desa.kuniran.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val newTitle: String = "",
    val newContent: String = "",
    val isPinned: Boolean = false,
    val errorMessage: String? = null
)

class AnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementsUiState(isLoading = true))
    val uiState: StateFlow<AnnouncementsUiState> = _uiState.asStateFlow()

    private var activeGroupId: String = "group_rt02"

    init {
        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { groupId ->
                activeGroupId = groupId
                loadAnnouncements(groupId)
            }
        }
    }

    private fun loadAnnouncements(groupId: String) {
        viewModelScope.launch {
            announcementRepository.observeAnnouncements(groupId).collect { list ->
                _uiState.update { it.copy(announcements = list, isLoading = false) }
            }
        }
    }

    fun onOpenAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                newTitle = "",
                newContent = "",
                isPinned = false,
                errorMessage = null
            )
        }
    }

    fun onDismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onApplyTemplate(template: AnnouncementTemplate) {
        _uiState.update {
            it.copy(
                newTitle = template.title,
                newContent = template.defaultContent
            )
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(newTitle = title) }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(newContent = content) }
    }

    fun onPinnedToggle(pinned: Boolean) {
        _uiState.update { it.copy(isPinned = pinned) }
    }

    fun submitAnnouncement() {
        val state = _uiState.value
        if (state.newTitle.isBlank() || state.newContent.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Judul dan isi pengumuman wajib diisi.") }
            return
        }

        viewModelScope.launch {
            when (val result = announcementRepository.createAnnouncement(
                groupId = activeGroupId,
                title = state.newTitle,
                content = state.newContent,
                isPinned = state.isPinned
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

    companion object {
        fun provideFactory(
            announcementRepository: AnnouncementRepository,
            groupRepository: GroupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnnouncementsViewModel(announcementRepository, groupRepository) as T
            }
        }
    }
}
