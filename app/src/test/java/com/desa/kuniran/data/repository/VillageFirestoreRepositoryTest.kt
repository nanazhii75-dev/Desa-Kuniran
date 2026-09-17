package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.Complaint
import com.desa.kuniran.core.model.ComplaintCategory
import com.desa.kuniran.core.model.ComplaintStatus
import com.desa.kuniran.core.model.GroupMember
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.model.MemberStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VillageFirestoreRepositoryTest {

    private lateinit var repository: FakeVillageFirestoreRepository

    @Before
    fun setup() {
        repository = FakeVillageFirestoreRepository()
    }

    @Test
    fun `saveAnnouncement and observeAnnouncements filters by groupId and sorts by pinned then date`() = runTest {
        val ann1 = Announcement(
            id = "ann_1",
            groupId = "group_rt02",
            title = "Kerja Bakti Saluran Air",
            content = "Pembersihan selokan RT 02",
            authorName = "Pengurus RT 02",
            isPinned = false,
            publishedAt = 1000L
        )
        val ann2 = Announcement(
            id = "ann_2",
            groupId = "group_rt02",
            title = "PENTING: Iuran Kemerdekaan",
            content = "Persiapan HUT RI",
            authorName = "Ketua RT 02",
            isPinned = true,
            publishedAt = 2000L
        )
        val annOtherGroup = Announcement(
            id = "ann_3",
            groupId = "group_rw04",
            title = "Rapat RW 04",
            content = "Pertemuan pengurus RW",
            authorName = "Ketua RW 04",
            isPinned = false,
            publishedAt = 3000L
        )

        repository.saveAnnouncement(ann1)
        repository.saveAnnouncement(ann2)
        repository.saveAnnouncement(annOtherGroup)

        val rt02Announcements = repository.observeAnnouncements("group_rt02").first()
        assertEquals(2, rt02Announcements.size)
        // Pinned item should appear first
        assertEquals("ann_2", rt02Announcements[0].id)
        assertTrue(rt02Announcements[0].isPinned)
        assertEquals("ann_1", rt02Announcements[1].id)
    }

    @Test
    fun `getAnnouncement returns null when not found and announcement when found`() = runTest {
        val ann = Announcement(
            id = "ann_detail",
            groupId = "group_rt02",
            title = "Jadwal Ronda Malam",
            content = "Pembagian regu poskamling",
            authorName = "Seksi Keamanan"
        )
        repository.saveAnnouncement(ann)

        val found = repository.getAnnouncement("ann_detail")
        assertTrue(found is AppResult.Success)
        assertEquals("Jadwal Ronda Malam", (found as AppResult.Success).data?.title)

        val notFound = repository.getAnnouncement("unknown_id")
        assertTrue(notFound is AppResult.Success)
        assertNull((notFound as AppResult.Success).data)
    }

    @Test
    fun `deleteAnnouncement removes item from storage`() = runTest {
        val ann = Announcement(
            id = "ann_to_delete",
            groupId = "group_rt02",
            title = "Pengumuman Sementara",
            content = "Akan segera dihapus",
            authorName = "Pengurus"
        )
        repository.saveAnnouncement(ann)
        repository.deleteAnnouncement("ann_to_delete")

        val result = repository.getAnnouncement("ann_to_delete")
        assertTrue(result is AppResult.Success)
        assertNull((result as AppResult.Success).data)
    }

    @Test
    fun `saveMemberProfile and observeMemberProfiles handles community member profiles properly`() = runTest {
        val member1 = GroupMember(
            id = "mem_1",
            groupId = "group_rt02",
            userId = "usr_1",
            displayName = "Budi Hartono",
            maskedPhone = "+62 812-****-5678",
            role = MemberRole.ADMIN,
            status = MemberStatus.ACTIVE
        )
        val member2 = GroupMember(
            id = "mem_2",
            groupId = "group_rt02",
            userId = "usr_2",
            displayName = "Ahmad Rifai",
            maskedPhone = "+62 856-****-1234",
            role = MemberRole.MEMBER,
            status = MemberStatus.ACTIVE,
            needsManualNotice = true
        )

        repository.saveMemberProfile(member1)
        repository.saveMemberProfile(member2)

        val members = repository.observeMemberProfiles("group_rt02").first()
        assertEquals(2, members.size)
        // Sorted alphabetically by displayName: Ahmad Rifai, then Budi Hartono
        assertEquals("Ahmad Rifai", members[0].displayName)
        assertTrue(members[0].needsManualNotice)
        assertEquals("Budi Hartono", members[1].displayName)

        val foundProfile = repository.getMemberProfile("mem_1")
        assertTrue(foundProfile is AppResult.Success)
        assertNotNull((foundProfile as AppResult.Success).data)
        assertEquals(MemberRole.ADMIN, foundProfile.data?.role)

        repository.deleteMemberProfile("mem_2")
        val remainingMembers = repository.observeMemberProfiles("group_rt02").first()
        assertEquals(1, remainingMembers.size)
        assertEquals("Budi Hartono", remainingMembers[0].displayName)
    }

    @Test
    fun `saveComplaint and observeComplaints filters by groupId and stores correctly`() = runTest {
        val c1 = Complaint(
            id = "c_1",
            groupId = "group_rt02",
            authorId = "user_1",
            authorName = "Pak Slamet",
            category = ComplaintCategory.LAMPU,
            description = "Lampu jalan di pos kamling mati",
            location = "Pos Kamling RT 02",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 1000L
        )
        val c2 = Complaint(
            id = "c_2",
            groupId = "group_rt03",
            authorId = "user_2",
            authorName = "Pak Joko",
            category = ComplaintCategory.SAMPAH,
            description = "Tempat sampah TPS penuh",
            location = "TPS Barat",
            status = ComplaintStatus.IN_PROGRESS,
            createdAt = 2000L
        )

        repository.saveComplaint(c1)
        repository.saveComplaint(c2)

        val rt02Complaints = repository.observeComplaints("group_rt02").first()
        assertEquals(1, rt02Complaints.size)
        assertEquals("Lampu jalan di pos kamling mati", rt02Complaints[0].description)
        assertEquals("Pak Slamet", rt02Complaints[0].authorName)
    }

    private class FakeVillageFirestoreRepository : VillageFirestoreRepository {
        private val announcements = mutableMapOf<String, Announcement>()
        private val members = mutableMapOf<String, GroupMember>()
        private val complaints = mutableMapOf<String, Complaint>()

        override fun observeAnnouncements(groupId: String) = kotlinx.coroutines.flow.flow {
            emit(
                announcements.values
                    .filter { it.groupId == groupId }
                    .sortedWith(
                        compareByDescending<Announcement> { it.isPinned }
                            .thenByDescending { it.publishedAt }
                    )
            )
        }

        override suspend fun saveAnnouncement(announcement: Announcement): AppResult<Unit> {
            announcements[announcement.id] = announcement
            return AppResult.Success(Unit)
        }

        override suspend fun getAnnouncement(announcementId: String): AppResult<Announcement?> {
            return AppResult.Success(announcements[announcementId])
        }

        override suspend fun deleteAnnouncement(announcementId: String): AppResult<Unit> {
            announcements.remove(announcementId)
            return AppResult.Success(Unit)
        }

        override fun observeMemberProfiles(groupId: String) = kotlinx.coroutines.flow.flow {
            emit(
                members.values
                    .filter { it.groupId == groupId }
                    .sortedBy { it.displayName }
            )
        }

        override suspend fun saveMemberProfile(member: GroupMember): AppResult<Unit> {
            members[member.id] = member
            return AppResult.Success(Unit)
        }

        override suspend fun getMemberProfile(memberId: String): AppResult<GroupMember?> {
            return AppResult.Success(members[memberId])
        }

        override suspend fun deleteMemberProfile(memberId: String): AppResult<Unit> {
            members.remove(memberId)
            return AppResult.Success(Unit)
        }

        override fun observeComplaints(groupId: String) = kotlinx.coroutines.flow.flow {
            emit(
                complaints.values
                    .filter { it.groupId == groupId }
                    .sortedByDescending { it.createdAt }
            )
        }

        override suspend fun saveComplaint(complaint: Complaint): AppResult<Unit> {
            complaints[complaint.id] = complaint
            return AppResult.Success(Unit)
        }
    }
}
