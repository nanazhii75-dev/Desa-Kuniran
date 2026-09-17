package com.desa.kuniran.core.model

data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val displayName: String,
    val maskedPhone: String,
    val role: MemberRole = MemberRole.MEMBER,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val joinedAt: Long = System.currentTimeMillis(),
    val invitedBy: String? = null,
    val needsManualNotice: Boolean = false, // Warga non-smartphone (Blueprint Bagian 32)
    val blockRt: String = "RT 02" // Blok / RT domisili warga untuk direktori warga
)

enum class MemberRole(val label: String) {
    OWNER("Pemilik"),
    ADMIN("Admin"),
    TREASURER("Bendahara"),
    SECRETARY("Sekretaris"),
    MODERATOR("Moderator"),
    MEMBER("Anggota");

    val isPrivileged: Boolean
        get() = this == OWNER || this == ADMIN || this == TREASURER || this == SECRETARY
}

enum class MemberStatus(val label: String) {
    PENDING("Menunggu"),
    ACTIVE("Aktif"),
    SUSPENDED("Ditangguhkan"),
    LEFT("Keluar"),
    REMOVED("Dikeluarkan")
}
