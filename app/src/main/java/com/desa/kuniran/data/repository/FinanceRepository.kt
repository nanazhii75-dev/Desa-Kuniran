package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppError
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.database.dao.MonthlyBalanceDao
import com.desa.kuniran.core.database.dao.TransactionDao
import com.desa.kuniran.core.database.entity.MonthlyBalanceEntity
import com.desa.kuniran.core.database.entity.TransactionEntity
import com.desa.kuniran.core.model.CashSummary
import com.desa.kuniran.core.model.Money
import com.desa.kuniran.core.model.MonthlyBalance
import com.desa.kuniran.core.model.Transaction
import com.desa.kuniran.core.model.TransactionStatus
import com.desa.kuniran.core.model.TransactionType
import com.desa.kuniran.data.firestore.FinanceFirestoreDataSource
import com.desa.kuniran.data.firestore.FinanceFirestoreDataSourceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

interface FinanceRepository {
    fun observeTransactions(groupId: String): Flow<List<Transaction>>
    fun observeTransactionsByPeriod(groupId: String, period: String): Flow<List<Transaction>>
    fun observeCashSummary(groupId: String): Flow<CashSummary>
    fun observeMonthlyBalances(groupId: String): Flow<List<MonthlyBalance>>
    suspend fun recordTransaction(
        groupId: String,
        type: TransactionType,
        amount: Money,
        categoryName: String,
        description: String,
        occurredAt: Long = System.currentTimeMillis()
    ): AppResult<Transaction>
    suspend fun recalculateMonthlyBalances(groupId: String)
    suspend fun syncWithFirestore(groupId: String)
    suspend fun initializeDefaultFinanceIfEmpty(groupId: String)
}

class FinanceRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val monthlyBalanceDao: MonthlyBalanceDao,
    private val firestoreDataSource: FinanceFirestoreDataSource = FinanceFirestoreDataSourceImpl(),
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : FinanceRepository {

    override fun observeTransactions(groupId: String): Flow<List<Transaction>> {
        return transactionDao.observeTransactions(groupId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeTransactionsByPeriod(groupId: String, period: String): Flow<List<Transaction>> {
        return transactionDao.observeTransactionsByPeriod(groupId, period).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeCashSummary(groupId: String): Flow<CashSummary> {
        return transactionDao.observeTransactions(groupId).map { list ->
            var income = 0L
            var expense = 0L
            for (item in list) {
                if (item.status == TransactionStatus.APPROVED.name) {
                    if (item.type == TransactionType.INCOME.name) {
                        income += item.amountRupiah
                    } else if (item.type == TransactionType.EXPENSE.name) {
                        expense += item.amountRupiah
                    }
                }
            }
            val balance = income - expense
            CashSummary(
                balance = Money(balance),
                totalIncome = Money(income),
                totalExpense = Money(expense),
                transactionCount = list.size,
                userFeePaid = true
            )
        }
    }

    override fun observeMonthlyBalances(groupId: String): Flow<List<MonthlyBalance>> {
        return monthlyBalanceDao.observeMonthlyBalances(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordTransaction(
        groupId: String,
        type: TransactionType,
        amount: Money,
        categoryName: String,
        description: String,
        occurredAt: Long
    ): AppResult<Transaction> {
        if (amount.rupiah <= 0) {
            return AppResult.Error(AppError.ValidationError("Jumlah nominal harus lebih dari Rp 0."))
        }
        if (description.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Keterangan transaksi tidak boleh kosong."))
        }

        val periodMonth = MonthlyBalance.formatPeriod(occurredAt)
        val txId = "tx_${System.currentTimeMillis()}"
        val entity = TransactionEntity(
            id = txId,
            groupId = groupId,
            accountId = "kas_utama",
            type = type.name,
            amountRupiah = amount.rupiah,
            categoryId = "cat_${categoryName.lowercase().replace(" ", "_")}",
            categoryName = categoryName,
            description = description,
            createdBy = "user_bendahara",
            createdByName = "Joko (Bendahara)",
            approvedBy = "user_admin",
            status = TransactionStatus.APPROVED.name,
            occurredAt = occurredAt,
            createdAt = System.currentTimeMillis(),
            syncState = "SYNCED",
            periodMonth = periodMonth
        )

        // 1. Simpan ke Room Database lokal
        transactionDao.insertTransaction(entity)

        // 2. Hitung ulang rekap saldo bulanan (Monthly Balance)
        recalculateMonthlyBalances(groupId)

        val domainTx = entity.toDomain()

        // 3. Sinkronkan ke Cloud Firestore di latar belakang
        externalScope.launch {
            firestoreDataSource.saveTransaction(domainTx)
            val currentMonthly = monthlyBalanceDao.getMonthlyBalance(groupId, periodMonth)
            if (currentMonthly != null) {
                firestoreDataSource.saveMonthlyBalance(currentMonthly.toDomain())
            }
        }

        return AppResult.Success(domainTx)
    }

    override suspend fun recalculateMonthlyBalances(groupId: String) {
        val allTx = transactionDao.getAllTransactions(groupId)
        if (allTx.isEmpty()) return

        // Kelompokkan berdasarkan period ("YYYY-MM")
        val grouped = allTx.groupBy {
            if (it.periodMonth.isNotBlank()) it.periodMonth
            else MonthlyBalance.formatPeriod(it.occurredAt)
        }

        // Urutkan periode secara kronologis ascending (lama ke baru)
        val sortedPeriods = grouped.keys.sorted()

        var runningBalance = 0L
        val balanceEntities = mutableListOf<MonthlyBalanceEntity>()

        for (period in sortedPeriods) {
            val transactionsInMonth = grouped[period].orEmpty()
            val startBalance = runningBalance

            var income = 0L
            var expense = 0L
            for (tx in transactionsInMonth) {
                if (tx.status == TransactionStatus.APPROVED.name) {
                    if (tx.type == TransactionType.INCOME.name) {
                        income += tx.amountRupiah
                    } else if (tx.type == TransactionType.EXPENSE.name) {
                        expense += tx.amountRupiah
                    }
                }
            }

            val endBalance = startBalance + income - expense
            runningBalance = endBalance

            val parts = period.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 9
            val monthName = MonthlyBalance.formatMonthName(year, month)

            balanceEntities.add(
                MonthlyBalanceEntity(
                    id = "${groupId}_$period",
                    period = period,
                    groupId = groupId,
                    year = year,
                    month = month,
                    monthName = monthName,
                    startingBalanceRupiah = startBalance,
                    totalIncomeRupiah = income,
                    totalExpenseRupiah = expense,
                    endingBalanceRupiah = endBalance,
                    transactionCount = transactionsInMonth.size,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        monthlyBalanceDao.insertMonthlyBalances(balanceEntities)
    }

    override suspend fun syncWithFirestore(groupId: String) {
        externalScope.launch {
            try {
                firestoreDataSource.observeTransactions(groupId).collect { remoteList ->
                    if (remoteList.isNotEmpty()) {
                        val entities = remoteList.map { tx ->
                            TransactionEntity(
                                id = tx.id,
                                groupId = tx.groupId,
                                accountId = tx.accountId,
                                type = tx.type.name,
                                amountRupiah = tx.amount.rupiah,
                                categoryId = tx.categoryId,
                                categoryName = tx.categoryName,
                                description = tx.description,
                                createdBy = tx.createdBy,
                                createdByName = tx.createdByName,
                                approvedBy = tx.approvedBy,
                                status = tx.status.name,
                                occurredAt = tx.occurredAt,
                                createdAt = tx.createdAt,
                                syncState = "SYNCED",
                                periodMonth = MonthlyBalance.formatPeriod(tx.occurredAt)
                            )
                        }
                        transactionDao.insertTransactions(entities)
                        recalculateMonthlyBalances(groupId)
                    }
                }
            } catch (_: Exception) {
                // Fallback offline jika Firestore belum terhubung
            }
        }
    }

    override suspend fun initializeDefaultFinanceIfEmpty(groupId: String) {
        val existing = transactionDao.getAllTransactions(groupId)
        if (existing.isNotEmpty()) {
            recalculateMonthlyBalances(groupId)
            return
        }

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        // Bulan berjalan (September 2026)
        cal.set(Calendar.DAY_OF_MONTH, 5)
        val sept05 = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 10)
        val sept10 = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 14)
        val sept14 = cal.timeInMillis

        // Bulan lalu (Agustus 2026)
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 10)
        val aug10 = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 17)
        val aug17 = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 25)
        val aug25 = cal.timeInMillis

        val augPeriod = MonthlyBalance.formatPeriod(aug10)
        val septPeriod = MonthlyBalance.formatPeriod(sept05)

        val baseline = listOf(
            // Transaksi Agustus 2026
            TransactionEntity(
                id = "tx_aug_01",
                groupId = groupId,
                accountId = "kas_utama",
                type = TransactionType.INCOME.name,
                amountRupiah = 450000L,
                categoryId = "iuran_rt",
                categoryName = "Iuran Kas RT",
                description = "Iuran bulanan warga Agustus",
                createdBy = "admin_rt",
                createdByName = "Budi (Admin)",
                approvedBy = "admin_rt",
                status = TransactionStatus.APPROVED.name,
                occurredAt = aug10,
                createdAt = aug10,
                syncState = "SYNCED",
                periodMonth = augPeriod
            ),
            TransactionEntity(
                id = "tx_aug_02",
                groupId = groupId,
                accountId = "kas_utama",
                type = TransactionType.INCOME.name,
                amountRupiah = 150000L,
                categoryId = "sumbangan",
                categoryName = "Sumbangan Warga",
                description = "Donasi peringatan HUT RI",
                createdBy = "admin_rt",
                createdByName = "Joko (Bendahara)",
                approvedBy = "admin_rt",
                status = TransactionStatus.APPROVED.name,
                occurredAt = aug17,
                createdAt = aug17,
                syncState = "SYNCED",
                periodMonth = augPeriod
            ),
            TransactionEntity(
                id = "tx_aug_03",
                groupId = groupId,
                accountId = "kas_utama",
                type = TransactionType.EXPENSE.name,
                amountRupiah = 350000L,
                categoryId = "kegiatan",
                categoryName = "Perlengkapan",
                description = "Pentas seni & hadiah lomba 17 Agustus",
                createdBy = "admin_rt",
                createdByName = "Joko (Bendahara)",
                approvedBy = "admin_rt",
                status = TransactionStatus.APPROVED.name,
                occurredAt = aug25,
                createdAt = aug25,
                syncState = "SYNCED",
                periodMonth = augPeriod
            ),

            // Transaksi September 2026
            TransactionEntity(
                id = "tx_sept_01",
                groupId = groupId,
                accountId = "kas_utama",
                type = TransactionType.INCOME.name,
                amountRupiah = 250000L,
                categoryId = "iuran_rt",
                categoryName = "Iuran Kas RT",
                description = "Kas bulanan warga September",
                createdBy = "admin_rt",
                createdByName = "Budi (Admin)",
                approvedBy = "admin_rt",
                status = TransactionStatus.APPROVED.name,
                occurredAt = sept05,
                createdAt = sept05,
                syncState = "SYNCED",
                periodMonth = septPeriod
            ),
            TransactionEntity(
                id = "tx_sept_02",
                groupId = groupId,
                accountId = "kas_utama",
                type = TransactionType.INCOME.name,
                amountRupiah = 50005L,
                categoryId = "jimpitan",
                categoryName = "Jimpitan Warga",
                description = "Jimpitan ronda malam minggu ke-1 & ke-2",
                createdBy = "admin_rt",
                createdByName = "Joko (Bendahara)",
                approvedBy = "admin_rt",
                status = TransactionStatus.APPROVED.name,
                occurredAt = sept10,
                createdAt = sept10,
                syncState = "SYNCED",
                periodMonth = septPeriod
            ),
            TransactionEntity(
                id = "tx_sept_03",
                groupId = groupId,
                accountId = "kas_utama",
                type = TransactionType.EXPENSE.name,
                amountRupiah = 94000L,
                categoryId = "kegiatan",
                categoryName = "Perlengkapan",
                description = "Peralatan gotong royong & konsumsi kerja bakti",
                createdBy = "admin_rt",
                createdByName = "Joko (Bendahara)",
                approvedBy = "admin_rt",
                status = TransactionStatus.APPROVED.name,
                occurredAt = sept14,
                createdAt = sept14,
                syncState = "SYNCED",
                periodMonth = septPeriod
            )
        )

        transactionDao.insertTransactions(baseline)
        recalculateMonthlyBalances(groupId)

        // Asynchronously mirror to Firestore structure
        externalScope.launch {
            try {
                for (tx in baseline) {
                    firestoreDataSource.saveTransaction(tx.toDomain())
                }
            } catch (e: Throwable) {
                android.util.Log.w("FinanceRepo", "Async mirror failed: ${e.message}")
            }
        }
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        groupId = groupId,
        accountId = accountId,
        type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.INCOME },
        amount = Money(amountRupiah),
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        createdBy = createdBy,
        createdByName = createdByName,
        approvedBy = approvedBy,
        status = try { TransactionStatus.valueOf(status) } catch (_: Exception) { TransactionStatus.APPROVED },
        occurredAt = occurredAt,
        createdAt = createdAt
    )

    private fun MonthlyBalanceEntity.toDomain() = MonthlyBalance(
        id = id,
        period = period,
        groupId = groupId,
        year = year,
        month = month,
        monthName = monthName,
        startingBalance = Money(startingBalanceRupiah),
        totalIncome = Money(totalIncomeRupiah),
        totalExpense = Money(totalExpenseRupiah),
        endingBalance = Money(endingBalanceRupiah),
        transactionCount = transactionCount,
        updatedAt = updatedAt
    )
}
