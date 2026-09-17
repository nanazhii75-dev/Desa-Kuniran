package com.desa.kuniran.core.model

data class Complaint(
    val id: String,
    val groupId: String,
    val authorId: String,
    val authorName: String,
    val category: ComplaintCategory,
    val description: String,
    val location: String,
    val status: ComplaintStatus = ComplaintStatus.SUBMITTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val officerNotes: String? = null
)

enum class ComplaintCategory(val label: String) {
    SAMPAH("Sampah & Kebersihan"),
    JALAN("Jalan & Saluran Air"),
    LAMPU("Penerangan Jalan"),
    AIR("Air Bersih"),
    KEAMANAN("Keamanan Lingkungan"),
    FASILITAS("Fasilitas Umum"),
    LINGKUNGAN("Ketertiban Lingkungan"),
    LAINNYA("Lainnya")
}

enum class ComplaintStatus(val label: String) {
    SUBMITTED("Terkirim"),
    VERIFIED("Diverifikasi"),
    IN_PROGRESS("Diproses"),
    RESOLVED("Selesai"),
    REJECTED("Ditolak"),
    CLOSED("Ditutup")
}
