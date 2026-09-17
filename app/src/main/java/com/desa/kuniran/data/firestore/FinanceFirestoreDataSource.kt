package com.desa.kuniran.data.firestore

import com.desa.kuniran.core.common.AppError
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.Money
import com.desa.kuniran.core.model.MonthlyBalance
import com.desa.kuniran.core.model.Transaction
import com.desa.kuniran.core.model.TransactionStatus
import com.desa.kuniran.core.model.TransactionType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Sumber data Cloud Firestore untuk modul Kas & Keuangan Warga (Desa Kuniran).
 *
 * Struktur Koleksi & Dokumen Firestore:
 * 1. groups/{groupId}/finance_transactions/{transactionId}
 *    - id: String
 *    - groupId: String
 *    - accountId: String ("kas_utama")
 *    - type: String ("INCOME" / "EXPENSE" / "TRANSFER" / "ADJUSTMENT")
 *    - amountRupiah: Long (Satuan Rupiah numerik tanpa floating-point)
 *    - categoryId: String
 *    - categoryName: String
 *    - description: String
 *    - createdBy: String (User ID pembuat)
 *    - createdByName: String (Nama pembuat, misal: "Joko (Bendahara)")
 *    - approvedBy: String? (Admin persetujuan)
 *    - status: String ("APPROVED", "PENDING", "REJECTED")
 *    - periodMonth: String (Format: "YYYY-MM", contoh: "2026-09")
 *    - occurredAt: Long (Timestamp waktu kejadian transaksi)
 *    - createdAt: Long (Timestamp pencatatan sistem)
 *
 * 2. groups/{groupId}/finance_monthly_balances/{period}
 *    - period: String (Document ID: "YYYY-MM", contoh: "2026-09")
 *    - groupId: String
 *    - year: Int (2026)
 *    - month: Int (1..12)
 *    - monthName: String (contoh: "September 2026")
 *    - startingBalance: Long (Saldo kas awal bulan)
 *    - totalIncome: Long (Akumulasi penerimaan bulan berjalan)
 *    - totalExpense: Long (Akumulasi pengeluaran bulan berjalan)
 *    - endingBalance: Long (Saldo kas penutupan bulan berjalan)
 *    - transactionCount: Int (Jumlah transaksi tercatat)
 *    - updatedAt: Long (Timestamp pembaruan terakhir)
 */
interface FinanceFirestoreDataSource {
    fun observeTransactions(groupId: String): Flow<List<Transaction>>
    fun observeMonthlyBalances(groupId: String): Flow<List<MonthlyBalance>>
    suspend fun saveTransaction(transaction: Transaction): AppResult<Unit>
    suspend fun saveMonthlyBalance(balance: MonthlyBalance): AppResult<Unit>
    suspend fun deleteTransaction(groupId: String, transactionId: String): AppResult<Unit>
}

class FinanceFirestoreDataSourceImpl(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            null
        }
    }
) : FinanceFirestoreDataSource {

    private val firestore: FirebaseFirestore?
        get() = firestoreProvider()

    companion object {
        const val COLLECTION_GROUPS = "groups"
        const val SUBCOLLECTION_TRANSACTIONS = "finance_transactions"
        const val SUBCOLLECTION_MONTHLY_BALANCES = "finance_monthly_balances"
    }

    override fun observeTransactions(groupId: String): Flow<List<Transaction>> {
        val fs = firestore ?: return kotlinx.coroutines.flow.emptyFlow()
        return callbackFlow {
            val registration = try {
                val query = fs.collection(COLLECTION_GROUPS)
                    .document(groupId)
                    .collection(SUBCOLLECTION_TRANSACTIONS)
                    .orderBy("occurredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

                query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val transactions = snapshot.documents.mapNotNull { doc ->
                            doc.toTransaction()
                        }
                        trySend(transactions)
                    }
                }
            } catch (e: Throwable) {
                close(e)
                null
            }

            awaitClose {
                registration?.remove()
            }
        }
    }

    override fun observeMonthlyBalances(groupId: String): Flow<List<MonthlyBalance>> {
        val fs = firestore ?: return kotlinx.coroutines.flow.emptyFlow()
        return callbackFlow {
            val registration = try {
                val query = fs.collection(COLLECTION_GROUPS)
                    .document(groupId)
                    .collection(SUBCOLLECTION_MONTHLY_BALANCES)
                    .orderBy("period", com.google.firebase.firestore.Query.Direction.DESCENDING)

                query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val balances = snapshot.documents.mapNotNull { doc ->
                            doc.toMonthlyBalance()
                        }
                        trySend(balances)
                    }
                }
            } catch (e: Throwable) {
                close(e)
                null
            }

            awaitClose {
                registration?.remove()
            }
        }
    }

    override suspend fun saveTransaction(transaction: Transaction): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            val periodMonth = MonthlyBalance.formatPeriod(transaction.occurredAt)
            val data = hashMapOf(
                "id" to transaction.id,
                "groupId" to transaction.groupId,
                "accountId" to transaction.accountId,
                "type" to transaction.type.name,
                "amountRupiah" to transaction.amount.rupiah,
                "categoryId" to transaction.categoryId,
                "categoryName" to transaction.categoryName,
                "description" to transaction.description,
                "createdBy" to transaction.createdBy,
                "createdByName" to transaction.createdByName,
                "approvedBy" to transaction.approvedBy,
                "status" to transaction.status.name,
                "periodMonth" to periodMonth,
                "occurredAt" to transaction.occurredAt,
                "createdAt" to transaction.createdAt
            )

            fs.collection(COLLECTION_GROUPS)
                .document(transaction.groupId)
                .collection(SUBCOLLECTION_TRANSACTIONS)
                .document(transaction.id)
                .set(data)
                .await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menyimpan transaksi ke Cloud Firestore: ${e.localizedMessage}"))
        }
    }

    override suspend fun saveMonthlyBalance(balance: MonthlyBalance): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            val data = hashMapOf(
                "id" to balance.id,
                "period" to balance.period,
                "groupId" to balance.groupId,
                "year" to balance.year,
                "month" to balance.month,
                "monthName" to balance.monthName,
                "startingBalance" to balance.startingBalance.rupiah,
                "totalIncome" to balance.totalIncome.rupiah,
                "totalExpense" to balance.totalExpense.rupiah,
                "endingBalance" to balance.endingBalance.rupiah,
                "transactionCount" to balance.transactionCount,
                "updatedAt" to balance.updatedAt
            )

            fs.collection(COLLECTION_GROUPS)
                .document(balance.groupId)
                .collection(SUBCOLLECTION_MONTHLY_BALANCES)
                .document(balance.period)
                .set(data)
                .await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menyimpan rekap kas bulanan ke Cloud Firestore: ${e.localizedMessage}"))
        }
    }

    override suspend fun deleteTransaction(groupId: String, transactionId: String): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            fs.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(SUBCOLLECTION_TRANSACTIONS)
                .document(transactionId)
                .delete()
                .await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menghapus transaksi dari Firestore: ${e.localizedMessage}"))
        }
    }

    private fun DocumentSnapshot.toTransaction(): Transaction? {
        val id = getString("id") ?: id
        val groupId = getString("groupId") ?: return null
        val accountId = getString("accountId") ?: "kas_utama"
        val typeStr = getString("type") ?: TransactionType.INCOME.name
        val amountRupiah = getLong("amountRupiah") ?: 0L
        val categoryId = getString("categoryId") ?: "umum"
        val categoryName = getString("categoryName") ?: "Umum"
        val description = getString("description") ?: ""
        val createdBy = getString("createdBy") ?: ""
        val createdByName = getString("createdByName") ?: "Warga"
        val approvedBy = getString("approvedBy")
        val statusStr = getString("status") ?: TransactionStatus.APPROVED.name
        val occurredAt = getLong("occurredAt") ?: System.currentTimeMillis()
        val createdAt = getLong("createdAt") ?: System.currentTimeMillis()

        val type = try {
            TransactionType.valueOf(typeStr)
        } catch (_: Exception) {
            TransactionType.INCOME
        }

        val status = try {
            TransactionStatus.valueOf(statusStr)
        } catch (_: Exception) {
            TransactionStatus.APPROVED
        }

        return Transaction(
            id = id,
            groupId = groupId,
            accountId = accountId,
            type = type,
            amount = Money(amountRupiah),
            categoryId = categoryId,
            categoryName = categoryName,
            description = description,
            createdBy = createdBy,
            createdByName = createdByName,
            approvedBy = approvedBy,
            status = status,
            occurredAt = occurredAt,
            createdAt = createdAt
        )
    }

    private fun DocumentSnapshot.toMonthlyBalance(): MonthlyBalance? {
        val period = getString("period") ?: id
        val groupId = getString("groupId") ?: return null
        val year = getLong("year")?.toInt() ?: 2026
        val month = getLong("month")?.toInt() ?: 9
        val monthName = getString("monthName") ?: MonthlyBalance.formatMonthName(year, month)
        val startingBalance = getLong("startingBalance") ?: 0L
        val totalIncome = getLong("totalIncome") ?: 0L
        val totalExpense = getLong("totalExpense") ?: 0L
        val endingBalance = getLong("endingBalance") ?: (startingBalance + totalIncome - totalExpense)
        val transactionCount = getLong("transactionCount")?.toInt() ?: 0
        val updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()

        return MonthlyBalance(
            id = "${groupId}_$period",
            period = period,
            groupId = groupId,
            year = year,
            month = month,
            monthName = monthName,
            startingBalance = Money(startingBalance),
            totalIncome = Money(totalIncome),
            totalExpense = Money(totalExpense),
            endingBalance = Money(endingBalance),
            transactionCount = transactionCount,
            updatedAt = updatedAt
        )
    }
}
