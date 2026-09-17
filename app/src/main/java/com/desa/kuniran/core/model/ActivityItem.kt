package com.desa.kuniran.core.model

data class ActivityItem(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val dateText: String,
    val location: String,
    val participantsCount: Int = 0,
    val committeeCount: Int = 0,
    val checklist: List<ActivityChecklistItem> = emptyList(),
    val userStatus: ParticipantStatus = ParticipantStatus.NONE,
    val createdAt: Long = System.currentTimeMillis()
)

data class ActivityChecklistItem(
    val id: String,
    val label: String,
    val isChecked: Boolean = false
)

enum class ParticipantStatus(val label: String) {
    GOING("Hadir"),
    MAYBE("Mungkin"),
    NOT_GOING("Tidak Hadir"),
    NONE("Belum Memilih")
}
