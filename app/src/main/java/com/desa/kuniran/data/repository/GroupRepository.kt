package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppError
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.database.dao.GroupDao
import com.desa.kuniran.core.database.entity.GroupEntity
import com.desa.kuniran.core.model.Group
import com.desa.kuniran.core.model.GroupStatus
import com.desa.kuniran.core.model.GroupType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

interface GroupRepository {
    fun observeGroups(): Flow<List<Group>>
    fun observeActiveGroupId(): Flow<String>
    suspend fun getActiveGroup(): Group?
    suspend fun selectGroup(groupId: String)
    suspend fun createGroup(group: Group): AppResult<Group>
    suspend fun joinGroupByCode(code: String): AppResult<Group>
    suspend fun initializeDefaultGroupsIfEmpty()
}

class GroupRepositoryImpl(
    private val groupDao: GroupDao
) : GroupRepository {

    private val _activeGroupId = MutableStateFlow("group_rt02")
    override fun observeActiveGroupId(): Flow<String> = _activeGroupId.asStateFlow()

    override fun observeGroups(): Flow<List<Group>> {
        return groupDao.observeAllGroups().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getActiveGroup(): Group? {
        val id = _activeGroupId.value
        return groupDao.getGroupById(id)?.toDomain()
    }

    override suspend fun selectGroup(groupId: String) {
        _activeGroupId.value = groupId
    }

    override suspend fun createGroup(group: Group): AppResult<Group> {
        val entity = group.toEntity()
        groupDao.insertGroup(entity)
        _activeGroupId.value = group.id
        return AppResult.Success(group)
    }

    override suspend fun joinGroupByCode(code: String): AppResult<Group> {
        val cleanCode = code.trim().uppercase()
        val found = groupDao.getGroupByCode(cleanCode)
        return if (found != null) {
            _activeGroupId.value = found.id
            AppResult.Success(found.toDomain())
        } else {
            AppResult.Error(AppError.NotFound("Kode group tidak ditemukan atau salah. Periksa kembali kodenya."))
        }
    }

    override suspend fun initializeDefaultGroupsIfEmpty() {
        // Blueprint Bagian 2: Group sebagai root entity
        val existing = groupDao.getGroupById("group_rt02")
        if (existing == null) {
            val defaultGroups = listOf(
                GroupEntity(
                    id = "group_rt02",
                    name = "RT 02 / RW 04",
                    description = "Lingkungan Warga RT 02 / RW 04 Jogorejo, Desa Kuniran",
                    avatarUrl = null,
                    groupType = GroupType.RT.name,
                    address = "Dusun Jogorejo, Desa Kuniran",
                    createdBy = "admin_rt",
                    status = GroupStatus.ACTIVE.name,
                    memberCount = 47,
                    activityCount = 8,
                    announcementCount = 3,
                    joinApprovalRequired = true,
                    groupCode = "KNR02",
                    createdAt = System.currentTimeMillis() - 86400000L * 30,
                    updatedAt = System.currentTimeMillis()
                ),
                GroupEntity(
                    id = "group_karang_taruna",
                    name = "Karang Taruna Hastungkara",
                    description = "Wadah pemuda dan kegiatan kreatif pemudi Desa Kuniran",
                    avatarUrl = null,
                    groupType = GroupType.COMMUNITY.name,
                    address = "Balai Desa Kuniran",
                    createdBy = "admin_kt",
                    status = GroupStatus.ACTIVE.name,
                    memberCount = 35,
                    activityCount = 5,
                    announcementCount = 2,
                    joinApprovalRequired = false,
                    groupCode = "KTKNR",
                    createdAt = System.currentTimeMillis() - 86400000L * 60,
                    updatedAt = System.currentTimeMillis()
                ),
                GroupEntity(
                    id = "group_rw04",
                    name = "RW 04 Jogorejo",
                    description = "Forum Gabungan RT 01 - RT 04 Desa Kuniran",
                    avatarUrl = null,
                    groupType = GroupType.RW.name,
                    address = "Dusun Jogorejo, Desa Kuniran",
                    createdBy = "admin_rw",
                    status = GroupStatus.ACTIVE.name,
                    memberCount = 180,
                    activityCount = 12,
                    announcementCount = 6,
                    joinApprovalRequired = true,
                    groupCode = "RW04J",
                    createdAt = System.currentTimeMillis() - 86400000L * 90,
                    updatedAt = System.currentTimeMillis()
                )
            )
            groupDao.insertGroups(defaultGroups)
        }
    }

    private fun GroupEntity.toDomain() = Group(
        id = id,
        name = name,
        description = description,
        avatarUrl = avatarUrl,
        groupType = GroupType.valueOf(groupType),
        address = address,
        createdBy = createdBy,
        status = GroupStatus.valueOf(status),
        memberCount = memberCount,
        activityCount = activityCount,
        announcementCount = announcementCount,
        joinApprovalRequired = joinApprovalRequired,
        groupCode = groupCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Group.toEntity() = GroupEntity(
        id = id,
        name = name,
        description = description,
        avatarUrl = avatarUrl,
        groupType = groupType.name,
        address = address,
        createdBy = createdBy,
        status = status.name,
        memberCount = memberCount,
        activityCount = activityCount,
        announcementCount = announcementCount,
        joinApprovalRequired = joinApprovalRequired,
        groupCode = groupCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
