package com.desa.kuniran.core.model

data class Announcement(
    val id: String,
    val groupId: String,
    val title: String,
    val content: String,
    val authorName: String,
    val isPinned: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis(),
    val readCount: Int = 0,
    val totalRecipients: Int = 0
)

enum class AnnouncementTemplate(val title: String, val defaultContent: String) {
    KERJA_BAKTI(
        "Kerja Bakti Lingkungan",
        "Diberitahukan kepada seluruh warga untuk mengikuti kegiatan kerja bakti pada hari Minggu pukul 07.00 WIB. Mohon membawa peralatan kebersihan masing-masing."
    ),
    IURAN_WARGA(
        "Pembayaran Iuran Kas Lingkungan",
        "Mengingatkan kepada bapak/ibu warga bahwa penarikan iuran kas bulanan dapat diserahkan kepada pengurus RT atau ditransfer ke rekening kas."
    ),
    PENGUMUMAN_UMUM(
        "Pengumuman Warga RT/RW",
        "Disampaikan informasi penting kepada seluruh warga lingkungan RT/RW Desa Kuniran mengenai kegiatan lingkungan mendatang."
    )
}
