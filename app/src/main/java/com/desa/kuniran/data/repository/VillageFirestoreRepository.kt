package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppError
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.Complaint
import com.desa.kuniran.core.model.ComplaintCategory
import com.desa.kuniran.core.model.ComplaintStatus
import com.desa.kuniran.core.model.GroupMember
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.model.MemberStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Kontrak repositori penyimpanan data desa berbasis Cloud Firestore.
 * Sesuai Blueprint:
 * - Menangani data pengumuman warga (announcements) dan profil anggota komunitas (community member profiles).
 * - Mendukung pembaruan real-time menggunakan Flow snapshot listener.
 * - Mengembalikan AppResult untuk operasi CRUD yang aman dari uncaught exception.
 */
interface VillageFirestoreRepository {
    fun observeAnnouncements(groupId: String): Flow<List<Announcement>>
    suspend fun saveAnnouncement(announcement: Announcement): AppResult<Unit>
    suspend fun getAnnouncement(announcementId: String): AppResult<Announcement?>
    suspend fun deleteAnnouncement(announcementId: String): AppResult<Unit>

    fun observeMemberProfiles(groupId: String): Flow<List<GroupMember>>
    suspend fun saveMemberProfile(member: GroupMember): AppResult<Unit>
    suspend fun getMemberProfile(memberId: String): AppResult<GroupMember?>
    suspend fun deleteMemberProfile(memberId: String): AppResult<Unit>

    fun observeComplaints(groupId: String): Flow<List<Complaint>>
    suspend fun saveComplaint(complaint: Complaint): AppResult<Unit>
}

/**
 * Implementasi produksi dari VillageFirestoreRepository menggunakan FirebaseFirestore.
 */
class VillageFirestoreRepositoryImpl(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            null
        }
    }
) : VillageFirestoreRepository {

    private val firestore: FirebaseFirestore?
        get() = firestoreProvider()

    companion object {
        const val COLLECTION_ANNOUNCEMENTS = "announcements"
        const val COLLECTION_MEMBERS = "members"
        const val COLLECTION_COMPLAINTS = "complaints"
    }

    override fun observeAnnouncements(groupId: String): Flow<List<Announcement>> {
        val fs = firestore ?: return kotlinx.coroutines.flow.emptyFlow()
        return callbackFlow {
            val registration = try {
                val query = fs.collection(COLLECTION_ANNOUNCEMENTS)
                    .whereEqualTo("groupId", groupId)

                query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val announcements = snapshot.documents.mapNotNull { doc ->
                            doc.toAnnouncement()
                        }.sortedWith(
                            compareByDescending<Announcement> { it.isPinned }
                                .thenByDescending { it.publishedAt }
                        )
                        trySend(announcements)
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

    override suspend fun saveAnnouncement(announcement: Announcement): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            val data = mapOf(
                "id" to announcement.id,
                "groupId" to announcement.groupId,
                "title" to announcement.title,
                "content" to announcement.content,
                "authorName" to announcement.authorName,
                "isPinned" to announcement.isPinned,
                "publishedAt" to announcement.publishedAt,
                "readCount" to announcement.readCount,
                "totalRecipients" to announcement.totalRecipients
            )
            fs.collection(COLLECTION_ANNOUNCEMENTS)
                .document(announcement.id)
                .set(data)
                .await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menyimpan pengumuman ke Firestore: ${e.localizedMessage}"))
        }
    }

    override suspend fun getAnnouncement(announcementId: String): AppResult<Announcement?> {
        val fs = firestore ?: return AppResult.Success(null)
        return try {
            val snapshot = fs.collection(COLLECTION_ANNOUNCEMENTS)
                .document(announcementId)
                .get()
                .await()
            if (snapshot.exists()) {
                AppResult.Success(snapshot.toAnnouncement())
            } else {
                AppResult.Success(null)
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal memuat pengumuman dari Firestore: ${e.localizedMessage}"))
        }
    }

    override suspend fun deleteAnnouncement(announcementId: String): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            fs.collection(COLLECTION_ANNOUNCEMENTS)
                .document(announcementId)
                .delete()
                .await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menghapus pengumuman di Firestore: ${e.localizedMessage}"))
        }
    }

    override fun observeMemberProfiles(groupId: String): Flow<List<GroupMember>> {
        val fs = firestore ?: return kotlinx.coroutines.flow.emptyFlow()
        return callbackFlow {
            val registration = try {
                val query = fs.collection(COLLECTION_MEMBERS)
                    .whereEqualTo("groupId", groupId)

                query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val members = snapshot.documents.mapNotNull { doc ->
                            doc.toGroupMember()
                        }.sortedBy { it.displayName }
                        trySend(members)
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

    override suspend fun saveMemberProfile(member: GroupMember): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            val data = mapOf(
                "id" to member.id,
                "groupId" to member.groupId,
                "userId" to member.userId,
                "displayName" to member.displayName,
                "maskedPhone" to member.maskedPhone,
                "role" to member.role.name,
                "status" to member.status.name,
                "joinedAt" to member.joinedAt,
                "invitedBy" to (member.invitedBy ?: ""),
                "needsManualNotice" to member.needsManualNotice,
                "blockRt" to member.blockRt
            )
            fs.collection(COLLECTION_MEMBERS)
                .document(member.id)
                .set(data)
                .await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menyimpan profil anggota ke Firestore: ${e.localizedMessage}"))
        }
    }

    override suspend fun getMemberProfile(memberId: String): AppResult<GroupMember?> {
        val fs = firestore ?: return AppResult.Success(null)
        return try {
            val snapshot = fs.collection(COLLECTION_MEMBERS)
                .document(memberId)
                .get()
                .await()
            if (snapshot.exists()) {
                AppResult.Success(snapshot.toGroupMember())
            } else {
                AppResult.Success(null)
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal memuat profil anggota dari Firestore: ${e.localizedMessage}"))
        }
    }

    override suspend fun deleteMemberProfile(memberId: String): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            fs.collection(COLLECTION_MEMBERS)
                .document(memberId)
                .delete()
                .await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menghapus profil anggota di Firestore: ${e.localizedMessage}"))
        }
    }

    override fun observeComplaints(groupId: String): Flow<List<Complaint>> {
        val fs = firestore ?: return kotlinx.coroutines.flow.emptyFlow()
        return callbackFlow {
            val registration = try {
                val query = fs.collection(COLLECTION_COMPLAINTS)
                    .whereEqualTo("groupId", groupId)

                query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val complaints = snapshot.documents.mapNotNull { doc ->
                            doc.toComplaint()
                        }.sortedByDescending { it.createdAt }
                        trySend(complaints)
                    }
                }
            } catch (e: Exception) {
                close(e)
                null
            }

            awaitClose {
                registration?.remove()
            }
        }
    }

    override suspend fun saveComplaint(complaint: Complaint): AppResult<Unit> {
        val fs = firestore ?: return AppResult.Success(Unit)
        return try {
            val map = mapOf(
                "id" to complaint.id,
                "groupId" to complaint.groupId,
                "authorId" to complaint.authorId,
                "authorName" to complaint.authorName,
                "category" to complaint.category.name,
                "description" to complaint.description,
                "location" to complaint.location,
                "status" to complaint.status.name,
                "createdAt" to complaint.createdAt,
                "updatedAt" to complaint.updatedAt,
                "officerNotes" to complaint.officerNotes
            )
            fs.collection(COLLECTION_COMPLAINTS)
                .document(complaint.id)
                .set(map)
                .await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.ServerError("Gagal menyimpan pengaduan ke Firestore: ${e.localizedMessage}"))
        }
    }
}

/**
 * Fungsi ekstensi internal untuk pemetaan DocumentSnapshot ke model Domain secara aman.
 */
fun DocumentSnapshot.toComplaint(): Complaint? {
    if (!exists()) return null
    val categoryStr = getString("category") ?: ComplaintCategory.FASILITAS.name
    val category = try {
        ComplaintCategory.valueOf(categoryStr)
    } catch (_: Exception) {
        ComplaintCategory.FASILITAS
    }

    val statusStr = getString("status") ?: ComplaintStatus.SUBMITTED.name
    val status = try {
        ComplaintStatus.valueOf(statusStr)
    } catch (_: Exception) {
        ComplaintStatus.SUBMITTED
    }

    return Complaint(
        id = getString("id") ?: id,
        groupId = getString("groupId") ?: "",
        authorId = getString("authorId") ?: "",
        authorName = getString("authorName") ?: "Warga Desa",
        category = category,
        description = getString("description") ?: "",
        location = getString("location") ?: "",
        status = status,
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
        officerNotes = getString("officerNotes")
    )
}
fun DocumentSnapshot.toAnnouncement(): Announcement? {
    if (!exists()) return null
    return Announcement(
        id = getString("id") ?: id,
        groupId = getString("groupId") ?: "",
        title = getString("title") ?: "",
        content = getString("content") ?: "",
        authorName = getString("authorName") ?: "",
        isPinned = getBoolean("isPinned") ?: false,
        publishedAt = getLong("publishedAt") ?: System.currentTimeMillis(),
        readCount = getLong("readCount")?.toInt() ?: 0,
        totalRecipients = getLong("totalRecipients")?.toInt() ?: 0
    )
}

fun DocumentSnapshot.toGroupMember(): GroupMember? {
    if (!exists()) return null
    val roleStr = getString("role") ?: MemberRole.MEMBER.name
    val role = try {
        MemberRole.valueOf(roleStr)
    } catch (_: Exception) {
        MemberRole.MEMBER
    }

    val statusStr = getString("status") ?: MemberStatus.ACTIVE.name
    val status = try {
        MemberStatus.valueOf(statusStr)
    } catch (_: Exception) {
        MemberStatus.ACTIVE
    }

    return GroupMember(
        id = getString("id") ?: id,
        groupId = getString("groupId") ?: "",
        userId = getString("userId") ?: "",
        displayName = getString("displayName") ?: "",
        maskedPhone = getString("maskedPhone") ?: "",
        role = role,
        status = status,
        joinedAt = getLong("joinedAt") ?: System.currentTimeMillis(),
        invitedBy = getString("invitedBy")?.takeIf { it.isNotBlank() },
        needsManualNotice = getBoolean("needsManualNotice") ?: false,
        blockRt = getString("blockRt") ?: "RT 02"
    )
}
