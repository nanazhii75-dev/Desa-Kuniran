package com.desa.kuniran.data.repository

import com.desa.kuniran.core.database.dao.NotificationDao
import com.desa.kuniran.core.database.entity.NotificationEntity
import com.desa.kuniran.core.model.NotificationChannel
import com.desa.kuniran.core.model.NotificationItem
import com.desa.kuniran.core.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NotificationRepository {
    fun observeNotifications(): Flow<List<NotificationItem>>
    suspend fun markAsRead(id: String)
    suspend fun markInformedManually(id: String)
    suspend fun addNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.NEW_ANNOUNCEMENT,
        channel: NotificationChannel = NotificationChannel.FCM
    )
    suspend fun initializeDefaultNotificationsIfEmpty()
}

class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<NotificationItem>> {
        return notificationDao.observeNotifications().map { list ->
            list.map {
                NotificationItem(
                    id = it.id,
                    type = NotificationType.valueOf(it.type),
                    title = it.title,
                    message = it.message,
                    timestamp = it.timestamp,
                    isRead = it.isRead,
                    channelUsed = NotificationChannel.valueOf(it.channelUsed)
                )
            }
        }
    }

    override suspend fun addNotification(
        title: String,
        message: String,
        type: NotificationType,
        channel: NotificationChannel
    ) {
        val entity = NotificationEntity(
            id = "notif_${System.currentTimeMillis()}_${(100..999).random()}",
            type = type.name,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            channelUsed = channel.name
        )
        notificationDao.insertNotification(entity)
    }

    override suspend fun markAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    override suspend fun markInformedManually(id: String) {
        notificationDao.markAsRead(id)
    }

    override suspend fun initializeDefaultNotificationsIfEmpty() {
        // Multi-channel fallback notifications (Blueprint Bagian 32)
        val baseline = listOf(
            NotificationEntity(
                id = "notif_01",
                type = NotificationType.ACTIVITY_REMINDER.name,
                title = "Pengingat Kerja Bakti",
                message = "Kerja bakti lingkungan RT 02 besok pagi pukul 07.00 WIB. Disampaikan via WhatsApp & Notifikasi.",
                timestamp = System.currentTimeMillis() - 3600000L * 4,
                isRead = false,
                channelUsed = NotificationChannel.WHATSAPP.name
            ),
            NotificationEntity(
                id = "notif_02",
                type = NotificationType.NEW_ANNOUNCEMENT.name,
                title = "Pengumuman Karnaval Budaya",
                message = "Rencana perayaan hari desa telah diumumkan di aplikasi.",
                timestamp = System.currentTimeMillis() - 86400000L * 1,
                isRead = true,
                channelUsed = NotificationChannel.FCM.name
            )
        )
        notificationDao.insertNotifications(baseline)
    }
}
