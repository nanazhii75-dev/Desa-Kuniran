package com.desa.kuniran.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model domain untuk rekap saldo kas bulanan (Kas Warga).
 * Digunakan untuk pelaporan transparansi kas RT/RW per bulan di Room dan Firestore.
 */
data class MonthlyBalance(
    val id: String, // Format: "${groupId}_${period}", contoh: "group_rt02_2026-09"
    val period: String, // Format: "YYYY-MM", contoh: "2026-09"
    val groupId: String,
    val year: Int,
    val month: Int, // 1 - 12
    val monthName: String, // Contoh: "September 2026"
    val startingBalance: Money,
    val totalIncome: Money,
    val totalExpense: Money,
    val endingBalance: Money,
    val transactionCount: Int,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val netCashFlow: Money get() = totalIncome - totalExpense
    val isSurplus: Boolean get() = totalIncome.rupiah >= totalExpense.rupiah

    companion object {
        fun formatPeriod(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date(timestamp))
        }

        fun formatMonthName(year: Int, month: Int): String {
            val monthNames = arrayOf(
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
            )
            val name = if (month in 1..12) monthNames[month - 1] else "Bulan $month"
            return "$name $year"
        }
    }
}
