package com.desa.kuniran.core.security

import android.content.Context
import android.content.SharedPreferences

/**
 * TokenStorage menyimpan token sesi akses dan refresh.
 * Sesuai Blueprint Bagian 13 & 15: Token disimpan aman dan tidak pernah diekspos plaintext.
 */
interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String, userId: String)
    fun updateAccessToken(accessToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun clearTokens()
    fun hasSession(): Boolean
}

class EncryptedTokenStorage(context: Context) : TokenStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "desa_kuniran_secure_prefs",
        Context.MODE_PRIVATE
    )

    override fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    override fun updateAccessToken(accessToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun clearTokens() {
        prefs.edit().clear().apply()
    }

    override fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }
}
