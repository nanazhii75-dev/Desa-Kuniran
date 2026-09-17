package com.desa.kuniran.core.network

import com.desa.kuniran.core.security.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor untuk menyematkan token otentikasi Bearer secara aman ke header HTTP.
 * Sesuai Aturan Mutlak #7 & Blueprint Bagian 11:
 * - Token dibaca langsung dari TokenStorage (Encrypted / Keystore-backed) saat runtime.
 * - Endpoint publik (OTP, refresh token) otomatis di-skip atau diidentifikasi lewat header internal.
 * - Header internal dibersihkan sebelum diteruskan ke server jaringan.
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage,
    private val appVersionName: String = "1.0"
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Sematkan header platform standar desa
        requestBuilder.header(NetworkConstants.HEADER_ACCEPT, NetworkConstants.VALUE_APPLICATION_JSON)
        requestBuilder.header(NetworkConstants.HEADER_PLATFORM, NetworkConstants.VALUE_PLATFORM)
        requestBuilder.header(NetworkConstants.HEADER_APP_VERSION, appVersionName)

        val isExplicitNoAuth = originalRequest.header(NetworkConstants.HEADER_NO_AUTH) != null
        val path = originalRequest.url.encodedPath

        // Cek apakah endpoint adalah public auth (request OTP, verify OTP, refresh token)
        val isPublicAuthPath = path.endsWith("/auth/request-otp") ||
                path.endsWith("/auth/verify-otp") ||
                path.endsWith("/auth/refresh-token")

        if (isExplicitNoAuth || isPublicAuthPath) {
            // Bersihkan header internal jika ada sebelum request dikirim
            requestBuilder.removeHeader(NetworkConstants.HEADER_NO_AUTH)
            return chain.proceed(requestBuilder.build())
        }

        // Ambil access token dari penyimpanan terenkripsi
        val token = tokenStorage.getAccessToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.header(
                NetworkConstants.HEADER_AUTHORIZATION,
                "${NetworkConstants.VALUE_BEARER_PREFIX}$token"
            )
        }

        return chain.proceed(requestBuilder.build())
    }
}
