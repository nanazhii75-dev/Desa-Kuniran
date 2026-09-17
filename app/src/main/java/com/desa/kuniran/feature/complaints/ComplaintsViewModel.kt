package com.desa.kuniran.feature.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.Complaint
import com.desa.kuniran.core.model.ComplaintCategory
import com.desa.kuniran.data.repository.ComplaintRepository
import com.desa.kuniran.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComplaintsUiState(
    val complaints: List<Complaint> = emptyList(),
    val filteredComplaints: List<Complaint> = emptyList(),
    val selectedCategoryFilter: ComplaintCategory? = null,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val newCategory: ComplaintCategory = ComplaintCategory.LAMPU,
    val newAuthorName: String = "",
    val newDescription: String = "",
    val newLocation: String = "",
    val errorMessage: String? = null
)

class ComplaintsViewModel(
    private val complaintRepository: ComplaintRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComplaintsUiState(isLoading = true))
    val uiState: StateFlow<ComplaintsUiState> = _uiState.asStateFlow()

    private var activeGroupId: String = "group_rt02"

    init {
        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { groupId ->
                activeGroupId = groupId
                loadComplaints(groupId)
            }
        }
    }

    private fun loadComplaints(groupId: String) {
        viewModelScope.launch {
            complaintRepository.observeComplaints(groupId).collect { list ->
                _uiState.update { current ->
                    val filtered = if (current.selectedCategoryFilter == null) {
                        list
                    } else {
                        list.filter { it.category == current.selectedCategoryFilter }
                    }
                    current.copy(
                        complaints = list,
                        filteredComplaints = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSelectCategoryFilter(category: ComplaintCategory?) {
        _uiState.update { current ->
            val filtered = if (category == null) {
                current.complaints
            } else {
                current.complaints.filter { it.category == category }
            }
            current.copy(
                selectedCategoryFilter = category,
                filteredComplaints = filtered
            )
        }
    }

    fun onOpenAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                newCategory = ComplaintCategory.LAMPU,
                newAuthorName = "",
                newDescription = "",
                newLocation = "",
                errorMessage = null
            )
        }
    }

    fun onDismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onCategoryChange(category: ComplaintCategory) {
        _uiState.update { it.copy(newCategory = category) }
    }

    fun onAuthorNameChange(name: String) {
        _uiState.update { it.copy(newAuthorName = name) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(newDescription = desc) }
    }

    fun onLocationChange(loc: String) {
        _uiState.update { it.copy(newLocation = loc) }
    }

    fun submitComplaint() {
        val state = _uiState.value
        if (state.newDescription.isBlank() || state.newLocation.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lokasi dan uraian pengaduan wajib diisi.") }
            return
        }

        viewModelScope.launch {
            val author = state.newAuthorName.trim().ifEmpty { "Warga Lingkungan" }
            when (val result = complaintRepository.submitComplaint(
                groupId = activeGroupId,
                category = state.newCategory,
                description = state.newDescription.trim(),
                location = state.newLocation.trim(),
                authorName = author
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
            complaintRepository: ComplaintRepository,
            groupRepository: GroupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ComplaintsViewModel(complaintRepository, groupRepository) as T
            }
        }
    }
}
