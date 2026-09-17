package com.desa.kuniran.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.GroupMember
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.security.PhoneProtector
import com.desa.kuniran.data.repository.GroupRepository
import com.desa.kuniran.data.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MembersUiState(
    val members: List<GroupMember> = emptyList(),
    val filteredMembers: List<GroupMember> = emptyList(),
    val searchQuery: String = "",
    val selectedBlockFilter: String = "Semua",
    val selectedRoleFilter: String = "Semua",
    val availableBlocks: List<String> = listOf("Semua", "RT 01", "RT 02", "RT 03", "Blok A", "Blok B", "Blok C"),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val newName: String = "",
    val newPhone: String = "",
    val newBlockRt: String = "RT 02",
    val normalizedPreview: String = "",
    val newRole: MemberRole = MemberRole.MEMBER,
    val needsManualNotice: Boolean = false,
    val errorMessage: String? = null
)

class MembersViewModel(
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val phoneProtector: PhoneProtector
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState(isLoading = true))
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    private var activeGroupId: String = "group_rt02"

    init {
        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { groupId ->
                activeGroupId = groupId
                loadMembers(groupId)
            }
        }
    }

    private fun loadMembers(groupId: String) {
        viewModelScope.launch {
            memberRepository.observeMembers(groupId).collect { list ->
                _uiState.update { state ->
                    val detectedBlocks = (listOf("Semua") + list.map { it.blockRt }.filter { it.isNotBlank() }.distinct()).distinct()
                    val filtered = applyFilters(
                        list = list,
                        query = state.searchQuery,
                        blockFilter = state.selectedBlockFilter,
                        roleFilter = state.selectedRoleFilter
                    )
                    state.copy(
                        members = list,
                        filteredMembers = filtered,
                        availableBlocks = if (detectedBlocks.size > 1) detectedBlocks else state.availableBlocks,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(
                list = state.members,
                query = query,
                blockFilter = state.selectedBlockFilter,
                roleFilter = state.selectedRoleFilter
            )
            state.copy(searchQuery = query, filteredMembers = filtered)
        }
    }

    fun onBlockFilterSelected(block: String) {
        _uiState.update { state ->
            val filtered = applyFilters(
                list = state.members,
                query = state.searchQuery,
                blockFilter = block,
                roleFilter = state.selectedRoleFilter
            )
            state.copy(selectedBlockFilter = block, filteredMembers = filtered)
        }
    }

    fun onRoleFilterSelected(role: String) {
        _uiState.update { state ->
            val filtered = applyFilters(
                list = state.members,
                query = state.searchQuery,
                blockFilter = state.selectedBlockFilter,
                roleFilter = role
            )
            state.copy(selectedRoleFilter = role, filteredMembers = filtered)
        }
    }

    private fun applyFilters(
        list: List<GroupMember>,
        query: String,
        blockFilter: String,
        roleFilter: String
    ): List<GroupMember> {
        return list.filter { member ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                member.displayName.contains(query, ignoreCase = true) ||
                        member.blockRt.contains(query, ignoreCase = true) ||
                        member.role.label.contains(query, ignoreCase = true) ||
                        member.maskedPhone.contains(query)
            }

            val matchesBlock = if (blockFilter == "Semua" || blockFilter.isBlank()) {
                true
            } else {
                member.blockRt.equals(blockFilter, ignoreCase = true)
            }

            val matchesRole = when (roleFilter) {
                "Pengurus" -> member.role.isPrivileged
                "Warga" -> !member.role.isPrivileged
                else -> true
            }

            matchesQuery && matchesBlock && matchesRole
        }
    }

    fun onOpenAddMember() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                newName = "",
                newPhone = "",
                newBlockRt = if (it.selectedBlockFilter != "Semua") it.selectedBlockFilter else "RT 02",
                normalizedPreview = "",
                newRole = MemberRole.MEMBER,
                needsManualNotice = false,
                errorMessage = null
            )
        }
    }

    fun onDismissAddMember() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(newName = name) }
    }

    fun onBlockRtChanged(blockRt: String) {
        _uiState.update { it.copy(newBlockRt = blockRt) }
    }

    fun onPhoneChanged(phone: String) {
        val normalized = phoneProtector.normalize(phone)
        _uiState.update {
            it.copy(
                newPhone = phone,
                normalizedPreview = normalized,
                errorMessage = null
            )
        }
    }

    fun onRoleChanged(role: MemberRole) {
        _uiState.update { it.copy(newRole = role) }
    }

    fun onManualNoticeToggled(checked: Boolean) {
        _uiState.update { it.copy(needsManualNotice = checked) }
    }

    fun submitAddMember() {
        val state = _uiState.value
        if (state.newName.isBlank() || state.newPhone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama dan nomor HP warga wajib diisi.") }
            return
        }

        viewModelScope.launch {
            when (val result = memberRepository.addMemberByPhone(
                groupId = activeGroupId,
                displayName = state.newName,
                rawPhone = state.newPhone,
                role = state.newRole,
                needsManualNotice = state.needsManualNotice,
                blockRt = state.newBlockRt.ifBlank { "RT 02" }
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
            memberRepository: MemberRepository,
            groupRepository: GroupRepository,
            phoneProtector: PhoneProtector
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MembersViewModel(memberRepository, groupRepository, phoneProtector) as T
            }
        }
    }
}
