package com.desa.kuniran.core.model

data class Group(
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String? = null,
    val groupType: GroupType = GroupType.RT,
    val address: String,
    val createdBy: String,
    val status: GroupStatus = GroupStatus.ACTIVE,
    val memberCount: Int = 1,
    val activityCount: Int = 0,
    val announcementCount: Int = 0,
    val joinApprovalRequired: Boolean = true,
    val groupCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class GroupType(val label: String) {
    RT("Rukun Tetangga (RT)"),
    RW("Rukun Warga (RW)"),
    COMMUNITY("Komunitas"),
    ORGANIZATION("Organisasi"),
    FAMILY("Keluarga"),
    BUSINESS("Usaha Warga"),
    OTHER("Lainnya")
}

enum class GroupStatus {
    ACTIVE,
    ARCHIVED
}
