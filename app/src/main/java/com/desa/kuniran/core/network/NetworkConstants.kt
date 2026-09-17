package com.desa.kuniran.core.network

/**
 * Konstanta konfigurasi jaringan untuk aplikasi Desa Kuniran.
 * Sesuai Blueprint Bagian 11 & 12: Endpoint aman, isolasi credential, dan header standar.
 */
object NetworkConstants {
    const val DEFAULT_BASE_URL = "https://desakuniran.app/api/v1/"

    // Headers
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_NO_AUTH = "X-No-Auth"
    const val HEADER_APP_VERSION = "X-App-Version"
    const val HEADER_PLATFORM = "X-Platform"
    const val HEADER_ACCEPT = "Accept"
    const val HEADER_CONTENT_TYPE = "Content-Type"

    // Values
    const val VALUE_PLATFORM = "Android"
    const val VALUE_APPLICATION_JSON = "application/json"
    const val VALUE_BEARER_PREFIX = "Bearer "

    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 20L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Auth retry limits
    const val MAX_AUTH_RETRIES = 2
}
