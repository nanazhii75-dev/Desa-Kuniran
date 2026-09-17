package com.desa.kuniran.core.security

/**
 * PhoneProtector menangani normalisasi dan masking nomor HP di client (Android).
 *
 * PENTING (Blueprint v2 Bagian 11 & 15):
 * - Normalisasi ke format E.164 (+62...) aman dilakukan di client tanpa secret.
 * - Client TIDAK PERNAH menghitung HMAC lookup atau AES-256-GCM.
 *   Seluruh operasi bersecret dieksekusi secara terpercaya di BACKEND via KMS.
 */
interface PhoneProtector {
    fun normalize(phone: String): String
    fun maskPhone(phone: String): String
    fun isValidIndonesianPhone(phone: String): Boolean
}

class PhoneProtectorImpl : PhoneProtector {

    override fun normalize(phone: String): String {
        // Hapus karakter pemisah umum: spasi, tanda hubung, kurung
        val cleaned = phone.replace(Regex("[\\s\\-\\(\\)\\.]"), "")

        return when {
            cleaned.startsWith("+62") -> cleaned
            cleaned.startsWith("62") -> "+$cleaned"
            cleaned.startsWith("08") -> "+62" + cleaned.substring(1)
            cleaned.startsWith("8") -> "+62$cleaned"
            else -> cleaned
        }
    }

    override fun maskPhone(phone: String): String {
        val normalized = normalize(phone)
        // Ubah format +6281234567890 menjadi format lokal ramah warga: 0812••••7890
        val localDisplay = if (normalized.startsWith("+62")) {
            "0" + normalized.substring(3)
        } else {
            normalized
        }

        return if (localDisplay.length >= 8) {
            val prefix = localDisplay.take(4)
            val suffix = localDisplay.takeLast(4)
            val maskLength = (localDisplay.length - 8).coerceAtLeast(3)
            val dots = "•".repeat(maskLength)
            "$prefix$dots$suffix"
        } else {
            localDisplay
        }
    }

    override fun isValidIndonesianPhone(phone: String): Boolean {
        val normalized = normalize(phone)
        // Standar nomor seluler Indonesia: +628 diikuti 8 sampai 11 digit angka
        val regex = Regex("^\\+628[1-9][0-9]{7,10}\$")
        return regex.matches(normalized)
    }
}
