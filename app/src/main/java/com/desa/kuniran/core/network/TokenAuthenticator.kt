package com.desa.kuniran.core.network

import com.desa.kuniran.core.security.TokenStorage
import com.squareup.moshi.Moshi
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * TokenAuthenticator menangani error HTTP 401 Unauthorized secara aman dan otomatis.
 * Sesuai Blueprint Bagian 11 & 12:
 * 1. Thread-safe: Sinkronisasi pembaruan token mencegah 'thundering herd' banyak request refresh bersamaan.
 * 2. Menggunakan client OkHttp terisolasi tanpa authenticator rekursif.
 * 3. Menghapus sesi dan memberitahu SessionManager bila refresh token kadaluarsa.
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val moshi: Moshi,
    private val baseUrl: String = NetworkConstants.DEFAULT_BASE_URL
) : Authenticator {

    private val refreshLock = Any()

    // Client terisolasi untuk request refresh token agar tidak terjadi rekursi tak terbatas
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Cegah perulangan percobaan melebihi batas
        if (responseCount(response) >= NetworkConstants.MAX_AUTH_RETRIES) {
            return null
        }

        val failedAuthorizationHeader = response.request.header(NetworkConstants.HEADER_AUTHORIZATION)
        val failedToken = failedAuthorizationHeader?.removePrefix(NetworkConstants.VALUE_BEARER_PREFIX)?.trim()

        synchronized(refreshLock) {
            val currentToken = tokenStorage.getAccessToken()

            // Jika thread lain sudah berhasil memperbarui token saat request ini antre
            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header(
                        NetworkConstants.HEADER_AUTHORIZATION,
                        "${NetworkConstants.VALUE_BEARER_PREFIX}$currentToken"
                    )
                    .build()
            }

            // Ambil refresh token
            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                handleSessionExpired()
                return null
            }

            // Lakukan pemanggilan jaringan sinkron ke endpoint refresh token
            val newTokens = performTokenRefresh(refreshToken)
            if (newTokens != null) {
                val newAccessToken = newTokens.accessToken
                val newRefreshToken = newTokens.refreshToken ?: refreshToken
                val userId = tokenStorage.getUserId().orEmpty()

                tokenStorage.saveTokens(newAccessToken, newRefreshToken, userId)

                return response.request.newBuilder()
                    .header(
                        NetworkConstants.HEADER_AUTHORIZATION,
                        "${NetworkConstants.VALUE_BEARER_PREFIX}$newAccessToken"
                    )
                    .build()
            } else {
                handleSessionExpired()
                return null
            }
        }
    }

    private fun performTokenRefresh(refreshToken: String): RefreshTokenResult? {
        return try {
            val jsonPayload = "{\"refreshToken\":\"$refreshToken\"}"
            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

            val cleanBaseUrl = baseUrl.trimEnd('/')
            val refreshRequest = Request.Builder()
                .url("$cleanBaseUrl/auth/refresh-token")
                .post(requestBody)
                .header(NetworkConstants.HEADER_NO_AUTH, "true")
                .header(NetworkConstants.HEADER_ACCEPT, NetworkConstants.VALUE_APPLICATION_JSON)
                .header(NetworkConstants.HEADER_CONTENT_TYPE, NetworkConstants.VALUE_APPLICATION_JSON)
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val responseBodyString = refreshResponse.body?.string()
                if (!responseBodyString.isNullOrBlank()) {
                    val adapter = moshi.adapter(RefreshTokenResponseDto::class.java)
                    val dto = adapter.fromJson(responseBodyString)
                    if (dto != null && dto.accessToken.isNotBlank()) {
                        return RefreshTokenResult(
                            accessToken = dto.accessToken,
                            refreshToken = dto.refreshToken
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun handleSessionExpired() {
        tokenStorage.clearTokens()
        sessionManager.notifySessionExpired()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    data class RefreshTokenResult(
        val accessToken: String,
        val refreshToken: String?
    )
}
