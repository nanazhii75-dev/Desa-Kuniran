package com.desa.kuniran.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.desa.kuniran.core.database.dao.ActivityDao
import com.desa.kuniran.core.database.dao.AnnouncementDao
import com.desa.kuniran.core.database.dao.ComplaintDao
import com.desa.kuniran.core.database.dao.GroupDao
import com.desa.kuniran.core.database.dao.MemberDao
import com.desa.kuniran.core.database.dao.MonthlyBalanceDao
import com.desa.kuniran.core.database.dao.NotificationDao
import com.desa.kuniran.core.database.dao.TransactionDao
import com.desa.kuniran.core.database.dao.UserDao
import com.desa.kuniran.core.database.entity.ActivityEntity
import com.desa.kuniran.core.database.entity.AnnouncementEntity
import com.desa.kuniran.core.database.entity.ComplaintEntity
import com.desa.kuniran.core.database.entity.GroupEntity
import com.desa.kuniran.core.database.entity.GroupMemberEntity
import com.desa.kuniran.core.database.entity.MonthlyBalanceEntity
import com.desa.kuniran.core.database.entity.NotificationEntity
import com.desa.kuniran.core.database.entity.TransactionEntity
import com.desa.kuniran.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        TransactionEntity::class,
        MonthlyBalanceEntity::class,
        ActivityEntity::class,
        AnnouncementEntity::class,
        ComplaintEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DesaKuniranDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBalanceDao(): MonthlyBalanceDao
    abstract fun activityDao(): ActivityDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: DesaKuniranDatabase? = null

        fun getInstance(context: Context): DesaKuniranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DesaKuniranDatabase::class.java,
                    "desa_kuniran.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
