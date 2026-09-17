package com.desa.kuniran.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val accountId: String,
    val type: String,
    val amountRupiah: Long, // Numeric Long amount (Blueprint Bagian 22 Money)
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val createdBy: String,
    val createdByName: String,
    val approvedBy: String?,
    val status: String,
    val occurredAt: Long,
    val createdAt: Long,
    val syncState: String = "SYNCED", // SYNCED, PENDING, FAILED (Blueprint Bagian 34)
    val periodMonth: String = "" // "YYYY-MM", contoh: "2026-09"
)

@Entity(tableName = "monthly_balances")
data class MonthlyBalanceEntity(
    @PrimaryKey val id: String, // "${groupId}_${period}"
    val period: String, // "YYYY-MM"
    val groupId: String,
    val year: Int,
    val month: Int,
    val monthName: String,
    val startingBalanceRupiah: Long,
    val totalIncomeRupiah: Long,
    val totalExpenseRupiah: Long,
    val endingBalanceRupiah: Long,
    val transactionCount: Int,
    val updatedAt: Long
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val dateText: String,
    val location: String,
    val participantsCount: Int,
    val committeeCount: Int,
    val checklistSerialized: String, // JSON list of checklist items
    val userStatus: String,
    val createdAt: Long
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val content: String,
    val authorName: String,
    val isPinned: Boolean,
    val publishedAt: Long,
    val readCount: Int,
    val totalRecipients: Int
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val authorId: String,
    val authorName: String,
    val category: String,
    val description: String,
    val location: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val officerNotes: String?
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val channelUsed: String
)
