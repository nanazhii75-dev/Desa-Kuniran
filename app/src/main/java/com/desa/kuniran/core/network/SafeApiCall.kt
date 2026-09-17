package com.desa.kuniran.core.network

import com.desa.kuniran.core.common.AppError
import com.desa.kuniran.core.common.AppResult
import retrofit2.Response
import java.io.IOException

/**
 * Eksekusi panggilan API Retrofit secara aman dengan konversi otomatis ke AppResult.
 * Sesuai Blueprint Bagian 11 & Aturan Mutlak #4 & #7:
 * Menjamin tidak ada uncaught exception jaringan yang menyebabkan crash aplikasi warga desa.
 */
suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> Response<T>
): AppResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                AppResult.Success(body)
            } else if (T::class == Unit::class) {
                @Suppress("UNCHECKED_CAST")
                AppResult.Success(Unit as T)
            } else {
                AppResult.Error(AppError.ServerError("Respon server kosong."))
            }
        } else {
            val code = response.code()
            val errorBody = response.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody)

            val appError = when (code) {
                401 -> AppError.Unauthorized()
                403 -> AppError.Forbidden()
                404 -> AppError.NotFound()
                409 -> AppError.Conflict()
                429 -> AppError.RateLimited()
                in 500..599 -> AppError.ServerError()
                else -> AppError.ValidationError(errorMessage ?: "Permintaan tidak valid ($code)")
            }
            AppResult.Error(appError)
        }
    } catch (e: IOException) {
        AppResult.Error(AppError.NetworkError("Koneksi jaringan gagal. Pastikan terhubung ke internet."))
    } catch (e: Exception) {
        AppResult.Error(AppError.UnknownError(e.localizedMessage ?: "Terjadi kesalahan yang tidak terduga."))
    }
}

/**
 * Ekstrak pesan kesalahan dari body error JSON jika tersedia.
 */
fun parseErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null
    return try {
        // Coba deteksi pola umum JSON error {"message": "..."}
        val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
        val match = messageRegex.find(errorBody)
        match?.groupValues?.getOrNull(1)
    } catch (_: Exception) {
        null
    }
}
