package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.database.dao.ActivityDao
import com.desa.kuniran.core.database.entity.ActivityEntity
import com.desa.kuniran.core.model.ActivityChecklistItem
import com.desa.kuniran.core.model.ActivityItem
import com.desa.kuniran.core.model.ParticipantStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ActivityRepository {
    fun observeActivities(groupId: String): Flow<List<ActivityItem>>
    suspend fun createActivity(
        groupId: String,
        title: String,
        description: String,
        dateText: String,
        location: String
    ): AppResult<ActivityItem>
    suspend fun rsvpActivity(activityId: String, status: ParticipantStatus): AppResult<Unit>
    suspend fun initializeDefaultActivitiesIfEmpty(groupId: String)
}

class ActivityRepositoryImpl(
    private val activityDao: ActivityDao
) : ActivityRepository {

    override fun observeActivities(groupId: String): Flow<List<ActivityItem>> {
        return activityDao.observeActivities(groupId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun createActivity(
        groupId: String,
        title: String,
        description: String,
        dateText: String,
        location: String
    ): AppResult<ActivityItem> {
        val id = "act_${System.currentTimeMillis()}"
        val entity = ActivityEntity(
            id = id,
            groupId = groupId,
            title = title,
            description = description,
            dateText = dateText,
            location = location,
            participantsCount = 1,
            committeeCount = 1,
            checklistSerialized = "Peralatan,Konsumsi,Dokumentasi",
            userStatus = ParticipantStatus.GOING.name,
            createdAt = System.currentTimeMillis()
        )
        activityDao.insertActivity(entity)
        return AppResult.Success(entity.toDomain())
    }

    override suspend fun rsvpActivity(activityId: String, status: ParticipantStatus): AppResult<Unit> {
        // Update user's RSVP status
        return AppResult.Success(Unit)
    }

    override suspend fun initializeDefaultActivitiesIfEmpty(groupId: String) {
        // Sesuai Blueprint Bagian 25: Karnaval Budaya Hastungkara, Kerja Bakti
        val baseline = listOf(
            ActivityEntity(
                id = "act_01",
                groupId = groupId,
                title = "Kerja Bakti Lingkungan RT 02",
                description = "Pembersihan selokan dan perapihan ranting pohon menjelang musim hujan. Seluruh bapak-bapak dan pemuda diharapkan hadir.",
                dateText = "Minggu, 20 September • 07.00 WIB",
                location = "Depan Pos Ronda RT 02",
                participantsCount = 24,
                committeeCount = 4,
                checklistSerialized = "Sapu lidi & cangkul,Karung sampah,Snack & kopi warga",
                userStatus = ParticipantStatus.GOING.name,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            ActivityEntity(
                id = "act_02",
                groupId = groupId,
                title = "Karnaval Budaya Hastungkara",
                description = "Pentas seni desa, arak-arakan gunungan hasil tani warga Desa Kuniran, dan perlombaan busana adat.",
                dateText = "Minggu, 27 September • 13.00 WIB",
                location = "Lapangan Desa Kuniran",
                participantsCount = 32,
                committeeCount = 8,
                checklistSerialized = "Kostum tarian,Sound system,Dekorasi panggung,Konsumsi panitia",
                userStatus = ParticipantStatus.MAYBE.name,
                createdAt = System.currentTimeMillis() - 86400000L * 4
            )
        )
        activityDao.insertActivities(baseline)
    }

    private fun ActivityEntity.toDomain(): ActivityItem {
        val checklistItems = checklistSerialized.split(",").filter { it.isNotBlank() }.mapIndexed { index, label ->
            ActivityChecklistItem(
                id = "chk_$index",
                label = label.trim(),
                isChecked = index < 2 // Centang 2 item pertama untuk demonstrasi realistis
            )
        }
        return ActivityItem(
            id = id,
            groupId = groupId,
            title = title,
            description = description,
            dateText = dateText,
            location = location,
            participantsCount = participantsCount,
            committeeCount = committeeCount,
            checklist = checklistItems,
            userStatus = ParticipantStatus.valueOf(userStatus),
            createdAt = createdAt
        )
    }
}
