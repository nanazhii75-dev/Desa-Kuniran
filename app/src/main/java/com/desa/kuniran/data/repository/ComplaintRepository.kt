package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.database.dao.ComplaintDao
import com.desa.kuniran.core.database.entity.ComplaintEntity
import com.desa.kuniran.core.model.Complaint
import com.desa.kuniran.core.model.ComplaintCategory
import com.desa.kuniran.core.model.ComplaintStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ComplaintRepository {
    fun observeComplaints(groupId: String): Flow<List<Complaint>>
    suspend fun submitComplaint(
        groupId: String,
        category: ComplaintCategory,
        description: String,
        location: String,
        authorName: String = "Warga RT 02"
    ): AppResult<Complaint>
    suspend fun initializeDefaultComplaintsIfEmpty(groupId: String)
}

class ComplaintRepositoryImpl(
    private val complaintDao: ComplaintDao,
    private val firestoreRepo: VillageFirestoreRepository? = null
) : ComplaintRepository {

    override fun observeComplaints(groupId: String): Flow<List<Complaint>> {
        return complaintDao.observeComplaints(groupId).map { list ->
            list.map {
                Complaint(
                    id = it.id,
                    groupId = it.groupId,
                    authorId = it.authorId,
                    authorName = it.authorName,
                    category = ComplaintCategory.valueOf(it.category),
                    description = it.description,
                    location = it.location,
                    status = ComplaintStatus.valueOf(it.status),
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    officerNotes = it.officerNotes
                )
            }
        }
    }

    override suspend fun submitComplaint(
        groupId: String,
        category: ComplaintCategory,
        description: String,
        location: String,
        authorName: String
    ): AppResult<Complaint> {
        val complaint = Complaint(
            id = "cmp_${System.currentTimeMillis()}",
            groupId = groupId,
            authorId = "usr_warga",
            authorName = authorName,
            category = category,
            description = description,
            location = location,
            status = ComplaintStatus.SUBMITTED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            officerNotes = "Laporan masuk ke pengurus lingkungan."
        )
        val entity = ComplaintEntity(
            id = complaint.id,
            groupId = complaint.groupId,
            authorId = complaint.authorId,
            authorName = complaint.authorName,
            category = complaint.category.name,
            description = complaint.description,
            location = complaint.location,
            status = complaint.status.name,
            createdAt = complaint.createdAt,
            updatedAt = complaint.updatedAt,
            officerNotes = complaint.officerNotes
        )
        complaintDao.insertComplaint(entity)
        firestoreRepo?.saveComplaint(complaint)
        return AppResult.Success(complaint)
    }

    override suspend fun initializeDefaultComplaintsIfEmpty(groupId: String) {
        val baseline = listOf(
            ComplaintEntity(
                id = "cmp_01",
                groupId = groupId,
                authorId = "usr_siti",
                authorName = "Siti Aminah",
                category = ComplaintCategory.LAMPU.name,
                description = "Lampu penerangan jalan di pertigaan gang 2 mati, jalanan gelap saat malam hari.",
                location = "Pertigaan Gang 2 RT 02",
                status = ComplaintStatus.IN_PROGRESS.name,
                createdAt = System.currentTimeMillis() - 86400000L * 2,
                updatedAt = System.currentTimeMillis() - 86400000L * 1,
                officerNotes = "Bohlam baru sudah dibeli, akan dipasang malam ini oleh petugas ronda."
            ),
            ComplaintEntity(
                id = "cmp_02",
                groupId = groupId,
                authorId = "usr_andi",
                authorName = "Andi Pratama",
                category = ComplaintCategory.JALAN.name,
                description = "Saluran air tersumbat sampah ranting setelah hujan deras kemarin.",
                location = "Depan Pos Ronda",
                status = ComplaintStatus.RESOLVED.name,
                createdAt = System.currentTimeMillis() - 86400000L * 5,
                updatedAt = System.currentTimeMillis() - 86400000L * 3,
                officerNotes = "Telah dibersihkan bersama saat kerja bakti."
            )
        )
        complaintDao.insertComplaints(baseline)
    }
}
