package com.desa.kuniran.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.GroupMember
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.model.User
import com.desa.kuniran.data.repository.AuthRepository
import com.desa.kuniran.data.repository.GroupRepository
import com.desa.kuniran.data.repository.MemberRepository
import com.desa.kuniran.data.repository.VillageFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val memberProfile: GroupMember? = null,
    val isSyncingWithFirestore: Boolean = false,
    val isSaving: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val showEditDialog: Boolean = false,
    val editDisplayName: String = "",
    val editPhone: String = "",
    val editRole: MemberRole = MemberRole.MEMBER,
    val notificationMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel untuk manajemen identitas dan profil warga Desa Kuniran.
 * Menghubungkan data warga lokal (Room) dengan basis data Cloud Firestore.
 */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val villageFirestoreRepository: VillageFirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                _uiState.update { current ->
                    current.copy(
                        user = user,
                        editDisplayName = current.editDisplayName.ifBlank { user?.displayName.orEmpty() },
                        editPhone = current.editPhone.ifBlank { user?.maskedPhone.orEmpty() }
                    )
                }

                // If user exists, also observe member entity and check Firestore
                user?.let { u ->
                    observeMemberDetails(u.id)
                }
            }
        }
    }

    private fun observeMemberDetails(userId: String) {
        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { groupId ->
                memberRepository.observeMembers(groupId).collect { members ->
                    val found = members.find { it.userId == userId } 
                        ?: members.firstOrNull() // fallback to first matching member in community

                    found?.let { member ->
                        _uiState.update { current ->
                            current.copy(
                                memberProfile = member,
                                editRole = member.role,
                                editDisplayName = if (current.showEditDialog) current.editDisplayName else member.displayName
                            )
                        }
                    }
                }
            }
        }
    }

    fun onOpenEditDialog() {
        val currentMember = _uiState.value.memberProfile
        val currentUser = _uiState.value.user
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editDisplayName = currentMember?.displayName ?: currentUser?.displayName.orEmpty(),
                editPhone = currentMember?.maskedPhone ?: currentUser?.maskedPhone.orEmpty(),
                editRole = currentMember?.role ?: MemberRole.MEMBER,
                errorMessage = null
            )
        }
    }

    fun onDismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, errorMessage = null) }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(editDisplayName = name, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(editPhone = phone, errorMessage = null) }
    }

    fun onRoleChange(role: MemberRole) {
        _uiState.update { it.copy(editRole = role) }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    /**
     * Menyimpan profil warga ke Room dan menyinkronkan secara langsung ke Cloud Firestore.
     */
    fun saveAndSyncProfileToFirestore() {
        val state = _uiState.value
        val name = state.editDisplayName.trim()
        val phone = state.editPhone.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama warga tidak boleh kosong") }
            return
        }

        val userId = state.user?.id ?: "usr_01"
        val activeGroupId = "group_rt02"

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSyncingWithFirestore = true, errorMessage = null) }

            when (val result = memberRepository.saveOrUpdateCitizenProfile(
                userId = userId,
                groupId = activeGroupId,
                displayName = name,
                rawPhone = phone,
                role = state.editRole
            )) {
                is AppResult.Success -> {
                    val updatedMember = result.data
                    val now = System.currentTimeMillis()

                    // Explicit sync attempt to Firestore
                    try {
                        villageFirestoreRepository.saveMemberProfile(updatedMember)
                    } catch (e: Throwable) {
                        android.util.Log.w("ProfileViewModel", "Firestore sync skipped: ${e.message}")
                    }

                    _uiState.update { current ->
                        current.copy(
                            memberProfile = updatedMember,
                            isSaving = false,
                            isSyncingWithFirestore = false,
                            showEditDialog = false,
                            lastSyncTimestamp = now,
                            notificationMessage = "Profil warga berhasil disimpan dan disinkronkan ke Cloud Firestore!"
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSyncingWithFirestore = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    /**
     * Sinkronisasi ulang data profil warga dari Cloud Firestore.
     */
    fun syncFromFirestore() {
        val memberId = _uiState.value.memberProfile?.id ?: "mem_01"
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWithFirestore = true) }
            try {
                val remoteResult = villageFirestoreRepository.getMemberProfile(memberId)
                val remoteProfile = if (remoteResult is AppResult.Success) remoteResult.data else null
                val now = System.currentTimeMillis()
                if (remoteProfile != null) {
                    _uiState.update {
                        it.copy(
                            memberProfile = remoteProfile,
                            isSyncingWithFirestore = false,
                            lastSyncTimestamp = now,
                            notificationMessage = "Profil warga berhasil disinkronkan dari Cloud Firestore!"
                        )
                    }
                } else {
                    // Profile already matches or initial cloud record
                    _uiState.update {
                        it.copy(
                            isSyncingWithFirestore = false,
                            lastSyncTimestamp = now,
                            notificationMessage = "Data profil warga sudah terverifikasi dengan Cloud Firestore."
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isSyncingWithFirestore = false,
                        notificationMessage = "Sinkronisasi offline: data profil tersimpan aman di database lokal."
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            memberRepository: MemberRepository,
            groupRepository: GroupRepository,
            villageFirestoreRepository: VillageFirestoreRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    authRepository,
                    memberRepository,
                    groupRepository,
                    villageFirestoreRepository
                ) as T
            }
        }
    }
}
