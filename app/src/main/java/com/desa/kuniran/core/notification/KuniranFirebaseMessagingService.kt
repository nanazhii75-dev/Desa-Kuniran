package com.desa.kuniran.core.notification

import android.util.Log
import com.desa.kuniran.DesaKuniranApplication
import com.desa.kuniran.core.model.NotificationChannel
import com.desa.kuniran.core.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service penerima Firebase Cloud Messaging (FCM) untuk Desa Kuniran.
 * Menangani notifikasi push pengumuman desa dan kegiatan warga secara real-time.
 */
class KuniranFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Refreshed FCM Device Token: $token")
        // Token dapat dikirimkan ke Firestore atau server desa jika diperlukan
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Menerima pesan FCM dari: ${remoteMessage.from}")

        // 1. Ambil data dari payload notifikasi atau data payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Pengumuman Baru Desa Kuniran"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "Ada warta pengumuman penting baru dari pengurus desa."

        val isPinned = remoteMessage.data["isPinned"]?.toBoolean() ?: true
        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        // 2. Tampilkan Android System Notification
        NotificationHelper.showAnnouncementNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            message = body,
            isPinned = isPinned
        )

        // 3. Simpan ke database lokal notifikasi aplikasi agar tercatat di layar Notifikasi
        try {
            val app = applicationContext as? DesaKuniranApplication
            app?.container?.let { container ->
                serviceScope.launch {
                    container.notificationRepository.addNotification(
                        title = title,
                        message = body,
                        type = NotificationType.NEW_ANNOUNCEMENT,
                        channel = NotificationChannel.FCM
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mencatat notifikasi ke database: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "KuniranFCMService"
    }
}
