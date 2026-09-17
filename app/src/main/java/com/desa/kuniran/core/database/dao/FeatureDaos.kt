package com.desa.kuniran.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.desa.kuniran.core.database.entity.ActivityEntity
import com.desa.kuniran.core.database.entity.AnnouncementEntity
import com.desa.kuniran.core.database.entity.ComplaintEntity
import com.desa.kuniran.core.database.entity.MonthlyBalanceEntity
import com.desa.kuniran.core.database.entity.NotificationEntity
import com.desa.kuniran.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE groupId = :groupId ORDER BY occurredAt DESC")
    fun observeTransactions(groupId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND (periodMonth = :period OR :period = '') ORDER BY occurredAt DESC")
    fun observeTransactionsByPeriod(groupId: String, period: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId = :groupId ORDER BY occurredAt ASC")
    suspend fun getAllTransactions(groupId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND periodMonth = :period ORDER BY occurredAt ASC")
    suspend fun getTransactionsByPeriod(groupId: String, period: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface MonthlyBalanceDao {
    @Query("SELECT * FROM monthly_balances WHERE groupId = :groupId ORDER BY period DESC")
    fun observeMonthlyBalances(groupId: String): Flow<List<MonthlyBalanceEntity>>

    @Query("SELECT * FROM monthly_balances WHERE groupId = :groupId AND period = :period LIMIT 1")
    suspend fun getMonthlyBalance(groupId: String, period: String): MonthlyBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBalances(balances: List<MonthlyBalanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBalance(balance: MonthlyBalanceEntity)

    @Query("DELETE FROM monthly_balances WHERE groupId = :groupId")
    suspend fun clearMonthlyBalances(groupId: String)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeActivities(groupId: String): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements WHERE groupId = :groupId ORDER BY isPinned DESC, publishedAt DESC")
    fun observeAnnouncements(groupId: String): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeComplaints(groupId: String): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaints(complaints: List<ComplaintEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}
