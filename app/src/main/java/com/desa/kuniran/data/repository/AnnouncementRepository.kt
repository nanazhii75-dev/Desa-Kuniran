package com.desa.kuniran.data.repository

import android.content.Context
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.database.dao.AnnouncementDao
import com.desa.kuniran.core.database.entity.AnnouncementEntity
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.NotificationChannel
import com.desa.kuniran.core.model.NotificationType
import com.desa.kuniran.core.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AnnouncementRepository {
    fun observeAnnouncements(groupId: String): Flow<List<Announcement>>
    suspend fun createAnnouncement(
        groupId: String,
        title: String,
        content: String,
        isPinned: Boolean
    ): AppResult<Announcement>
    suspend fun initializeDefaultAnnouncementsIfEmpty(groupId: String)
}

class AnnouncementRepositoryImpl(
    private val announcementDao: AnnouncementDao,
    private val firestoreRepo: VillageFirestoreRepository? = null,
    private val notificationRepository: NotificationRepository? = null,
    private val context: Context? = null
) : AnnouncementRepository {

    override fun observeAnnouncements(groupId: String): Flow<List<Announcement>> {
        return announcementDao.observeAnnouncements(groupId).map { list ->
            list.map {
                Announcement(
                    id = it.id,
                    groupId = it.groupId,
                    title = it.title,
                    content = it.content,
                    authorName = it.authorName,
                    isPinned = it.isPinned,
                    publishedAt = it.publishedAt,
                    readCount = it.readCount,
                    totalRecipients = it.totalRecipients
                )
            }
        }
    }

    override suspend fun createAnnouncement(
        groupId: String,
        title: String,
        content: String,
        isPinned: Boolean
    ): AppResult<Announcement> {
        val entity = AnnouncementEntity(
            id = "ann_${System.currentTimeMillis()}",
            groupId = groupId,
            title = title,
            content = content,
            authorName = "Pengurus RT 02",
            isPinned = isPinned,
            publishedAt = System.currentTimeMillis(),
            readCount = 1,
            totalRecipients = 47
        )
        announcementDao.insertAnnouncement(entity)

        val announcement = Announcement(
            id = entity.id,
            groupId = entity.groupId,
            title = entity.title,
            content = entity.content,
            authorName = entity.authorName,
            isPinned = entity.isPinned,
            publishedAt = entity.publishedAt,
            readCount = entity.readCount,
            totalRecipients = entity.totalRecipients
        )

        // Sinkronisasi ke Firestore
        firestoreRepo?.saveAnnouncement(announcement)

        // Catat ke riwayat notifikasi lokal warga
        notificationRepository?.addNotification(
            title = if (isPinned) "📌 [PENTING] $title" else "📢 $title",
            message = content,
            type = NotificationType.NEW_ANNOUNCEMENT,
            channel = NotificationChannel.FCM
        )

        // Tampilkan push notification otomatis
        context?.let { ctx ->
            NotificationHelper.showAnnouncementNotification(
                context = ctx,
                notificationId = (System.currentTimeMillis() % 100000).toInt(),
                title = title,
                message = content,
                isPinned = isPinned
            )
        }

        return AppResult.Success(announcement)
    }

    override suspend fun initializeDefaultAnnouncementsIfEmpty(groupId: String) {
        // Sesuai Blueprint Bagian 26 & 30 (Template siap pakai)
        val baseline = listOf(
            AnnouncementEntity(
                id = "ann_01",
                groupId = groupId,
                title = "Karnaval Budaya & Peringatan Hari Desa",
                content = "Dimohon perwakilan tiap dasawisma untuk berkumpul di rumah Bapak RT pada Jumat malam pukul 19.30 WIB guna pembagian tugas panggung dan tarian.",
                authorName = "Budi Santoso (Ketua RT)",
                isPinned = true,
                publishedAt = System.currentTimeMillis() - 86400000L * 1,
                readCount = 42,
                totalRecipients = 47
            ),
            AnnouncementEntity(
                id = "ann_02",
                groupId = groupId,
                title = "Pengangkutan Sampah Terpadu",
                content = "Jadwal pengangkutan sampah anorganik dan pilah plastik dilaksanakan besok pagi pukul 06.30 WIB. Mohon karung diletakkan di depan pagar rumah.",
                authorName = "Joko (Seksi Lingkungan)",
                isPinned = false,
                publishedAt = System.currentTimeMillis() - 86400000L * 3,
                readCount = 39,
                totalRecipients = 47
            )
        )
        announcementDao.insertAnnouncements(baseline)
    }
}
