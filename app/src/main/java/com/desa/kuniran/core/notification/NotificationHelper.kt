package com.desa.kuniran.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.desa.kuniran.MainActivity
import com.desa.kuniran.R
import com.desa.kuniran.core.model.Announcement
import com.google.firebase.messaging.FirebaseMessaging

object NotificationHelper {
    const val CHANNEL_ID_PENGUMUMAN = "pengumuman_desa"
    const val CHANNEL_NAME_PENGUMUMAN = "Warta & Pengumuman Warga Desa Kuniran"
    const val CHANNEL_DESC_PENGUMUMAN = "Notifikasi otomatis saat ada warta resmi atau pengumuman penting dari pengurus desa."
    const val TOPIC_PENGUMUMAN = "pengumuman_desa"

    private const val TAG = "NotificationHelper"

    /**
     * Membuat NotificationChannel untuk Android 8.0 (API 26) ke atas.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PENGUMUMAN,
                CHANNEL_NAME_PENGUMUMAN,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_PENGUMUMAN
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Mendaftarkan perangkat ke Firebase Cloud Messaging topic "pengumuman_desa".
     */
    fun subscribeToAnnouncementTopic() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_PENGUMUMAN)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Berhasil berlangganan ke topik FCM: $TOPIC_PENGUMUMAN")
                    } else {
                        Log.w(TAG, "Gagal berlangganan topik FCM: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "FCM belum siap atau Google Play Services tidak tersedia: ${e.message}")
        }
    }

    /**
     * Menampilkan notifikasi push di status bar Android ketika ada pengumuman baru/penting.
     */
    fun showAnnouncementNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        isPinned: Boolean = false
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_screen", "announcements")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefix = if (isPinned) "📌 [PENTING] " else "📢 "
        val displayTitle = prefix + title

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PENGUMUMAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isPinned) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Izin POST_NOTIFICATIONS belum diberikan: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menampilkan notifikasi: ${e.message}")
        }
    }
}
