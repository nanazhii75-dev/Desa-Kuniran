package com.desa.kuniran.core.model

data class Transaction(
    val id: String,
    val groupId: String,
    val accountId: String = "kas_utama",
    val type: TransactionType,
    val amount: Money,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val createdBy: String,
    val createdByName: String,
    val approvedBy: String? = null,
    val status: TransactionStatus = TransactionStatus.APPROVED,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer"),
    ADJUSTMENT("Penyesuaian")
}

enum class TransactionStatus(val label: String) {
    PENDING("Menunggu Persetujuan"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak"),
    CANCELLED("Dibatalkan")
}

data class CashSummary(
    val balance: Money,
    val totalIncome: Money,
    val totalExpense: Money,
    val transactionCount: Int,
    val userFeePaid: Boolean = true
)
