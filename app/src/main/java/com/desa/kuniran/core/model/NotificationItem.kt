package com.desa.kuniran.core.model

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val channelUsed: NotificationChannel = NotificationChannel.FCM
)

enum class NotificationType {
    NEW_ANNOUNCEMENT,
    GROUP_INVITATION,
    ACTIVITY_REMINDER,
    PAYMENT_REMINDER,
    COMPLAINT_UPDATE,
    SYSTEM
}

enum class NotificationChannel(val label: String) {
    FCM("Push Notifikasi"),
    WHATSAPP("WhatsApp"),
    SMS("SMS"),
    MANUAL("Manual (Lisan/Kertas)")
}
