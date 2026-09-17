package com.desa.kuniran.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val maskedPhone: String,
    val avatarUrl: String?,
    val accountStatus: String,
    val createdAt: Long,
    val lastLoginAt: Long
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val groupType: String,
    val address: String,
    val createdBy: String,
    val status: String,
    val memberCount: Int,
    val activityCount: Int,
    val announcementCount: Int,
    val joinApprovalRequired: Boolean,
    val groupCode: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "group_members")
data class GroupMemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val userId: String,
    val displayName: String,
    val maskedPhone: String, // Masked phone only (Blueprint Bagian 37)
    val role: String,
    val status: String,
    val joinedAt: Long,
    val invitedBy: String?,
    val needsManualNotice: Boolean,
    val blockRt: String = "RT 02"
)
