package com.desa.kuniran.core.common

sealed class AppError(open val userMessage: String) {
    data class NetworkError(
        override val userMessage: String = "Koneksi internet bermasalah. Periksa jaringan Anda."
    ) : AppError(userMessage)

    data class Unauthorized(
        override val userMessage: String = "Sesi Anda telah berakhir. Silakan masuk kembali."
    ) : AppError(userMessage)

    data class Forbidden(
        override val userMessage: String = "Anda tidak memiliki izin untuk melakukan tindakan ini."
    ) : AppError(userMessage)

    data class NotFound(
        override val userMessage: String = "Data tidak ditemukan."
    ) : AppError(userMessage)

    data class ValidationError(
        override val userMessage: String
    ) : AppError(userMessage)

    data class Conflict(
        override val userMessage: String = "Terjadi konflik data. Silakan coba kembali."
    ) : AppError(userMessage)

    data class RateLimited(
        override val userMessage: String = "Terlalu banyak percobaan. Silakan tunggu beberapa saat."
    ) : AppError(userMessage)

    data class ServerError(
        override val userMessage: String = "Terjadi masalah pada server desa. Coba beberapa saat lagi."
    ) : AppError(userMessage)

    data class UnknownError(
        override val userMessage: String = "Terjadi kesalahan yang tidak terduga. Silakan coba lagi."
    ) : AppError(userMessage)
}
