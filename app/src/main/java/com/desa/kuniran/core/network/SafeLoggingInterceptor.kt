package com.desa.kuniran.core.network

import com.desa.kuniran.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Interceptor logging jaringan yang aman dari kebocoran credential warga.
 * Sesuai Aturan Mutlak #7: Header Authorization dan session data tidak boleh
 * tercatat di log Android / Logcat dalam bentuk plaintext.
 */
object SafeLoggingInterceptor {

    fun create(isDebug: Boolean = BuildConfig.DEBUG): Interceptor {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // Sensor header sensitif agar tidak bocor ke logcat
            redactHeader(NetworkConstants.HEADER_AUTHORIZATION)
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }

        return loggingInterceptor
    }
}
