package com.desa.kuniran

import android.app.Application
import android.util.Log
import com.desa.kuniran.core.di.AppContainer
import com.desa.kuniran.core.di.DefaultAppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class DesaKuniranApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized()
        container = DefaultAppContainer(this)
        com.desa.kuniran.core.notification.NotificationHelper.createNotificationChannels(this)
        com.desa.kuniran.core.notification.NotificationHelper.subscribeToAnnouncementTopic()
    }

    private fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.desa.kuniran")
                    .setProjectId("desa-kuniran")
                    .setApiKey("AIzaSyFakeKeyForLocalFallbackOnly1234567")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.i("DesaKuniran", "FirebaseApp initialized with fallback options")
            }
        } catch (e: Throwable) {
            Log.w("DesaKuniran", "FirebaseApp initialization skipped: ${e.message}")
        }
    }
}
