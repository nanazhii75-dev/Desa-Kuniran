package com.desa.kuniran.data.repository

import com.desa.kuniran.core.common.AppError
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.database.dao.MemberDao
import com.desa.kuniran.core.database.entity.GroupMemberEntity
import com.desa.kuniran.core.model.GroupMember
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.model.MemberStatus
import com.desa.kuniran.core.security.PhoneProtector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MemberRepository {
    fun observeMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun addMemberByPhone(
        groupId: String,
        displayName: String,
        rawPhone: String,
        role: MemberRole,
        needsManualNotice: Boolean = false,
        blockRt: String = "RT 02"
    ): AppResult<GroupMember>
    suspend fun saveOrUpdateCitizenProfile(
        userId: String,
        groupId: String,
        displayName: String,
        rawPhone: String,
        role: MemberRole,
        blockRt: String = "RT 02"
    ): AppResult<GroupMember>
    suspend fun removeMember(memberId: String): AppResult<Unit>
    suspend fun initializeDefaultMembersIfEmpty(groupId: String)
}

class MemberRepositoryImpl(
    private val memberDao: MemberDao,
    private val phoneProtector: PhoneProtector,
    private val villageFirestoreRepository: VillageFirestoreRepository? = null
) : MemberRepository {

    override fun observeMembers(groupId: String): Flow<List<GroupMember>> {
        return memberDao.observeMembersByGroup(groupId).map { list ->
            list.map { entity ->
                GroupMember(
                    id = entity.id,
                    groupId = entity.groupId,
                    userId = entity.userId,
                    displayName = entity.displayName,
                    maskedPhone = entity.maskedPhone,
                    role = MemberRole.valueOf(entity.role),
                    status = MemberStatus.valueOf(entity.status),
                    joinedAt = entity.joinedAt,
                    invitedBy = entity.invitedBy,
                    needsManualNotice = entity.needsManualNotice,
                    blockRt = entity.blockRt
                )
            }
        }
    }

    override suspend fun addMemberByPhone(
        groupId: String,
        displayName: String,
        rawPhone: String,
        role: MemberRole,
        needsManualNotice: Boolean,
        blockRt: String
    ): AppResult<GroupMember> {
        val normalized = phoneProtector.normalize(rawPhone)
        if (!phoneProtector.isValidIndonesianPhone(normalized)) {
            return AppResult.Error(AppError.ValidationError("Nomor HP tidak valid. Masukkan nomor HP Indonesia yang aktif."))
        }

        // Masking HP untuk privasi warga (Blueprint Bagian 20)
        val masked = phoneProtector.maskPhone(normalized)
        val memberId = "mem_${System.currentTimeMillis()}"
        val entity = GroupMemberEntity(
            id = memberId,
            groupId = groupId,
            userId = "usr_${normalized.takeLast(6)}",
            displayName = displayName.ifBlank { "Warga Lingkungan" },
            maskedPhone = masked,
            role = role.name,
            status = MemberStatus.ACTIVE.name,
            joinedAt = System.currentTimeMillis(),
            invitedBy = "admin_current",
            needsManualNotice = needsManualNotice,
            blockRt = blockRt
        )
        memberDao.insertMember(entity)

        val member = GroupMember(
            id = entity.id,
            groupId = entity.groupId,
            userId = entity.userId,
            displayName = entity.displayName,
            maskedPhone = entity.maskedPhone,
            role = role,
            status = MemberStatus.ACTIVE,
            joinedAt = entity.joinedAt,
            needsManualNotice = entity.needsManualNotice,
            blockRt = blockRt
        )

        // Sinkronisasi realtime ke Cloud Firestore (Direktori Warga)
        try {
            villageFirestoreRepository?.saveMemberProfile(member)
        } catch (e: Throwable) {
            android.util.Log.w("MemberRepository", "Firestore member sync: ${e.message}")
        }

        return AppResult.Success(member)
    }

    override suspend fun saveOrUpdateCitizenProfile(
        userId: String,
        groupId: String,
        displayName: String,
        rawPhone: String,
        role: MemberRole,
        blockRt: String
    ): AppResult<GroupMember> {
        val normalized = if (rawPhone.contains("•") || rawPhone.isBlank()) {
            "081234567890"
        } else {
            phoneProtector.normalize(rawPhone)
        }
        val masked = if (rawPhone.contains("•")) rawPhone else phoneProtector.maskPhone(normalized)
        val memberId = "mem_${userId.replace("-", "_")}"
        val entity = GroupMemberEntity(
            id = memberId,
            groupId = groupId,
            userId = userId,
            displayName = displayName.trim(),
            maskedPhone = masked,
            role = role.name,
            status = MemberStatus.ACTIVE.name,
            joinedAt = System.currentTimeMillis(),
            invitedBy = null,
            needsManualNotice = false,
            blockRt = blockRt
        )
        memberDao.insertMember(entity)

        val member = GroupMember(
            id = entity.id,
            groupId = entity.groupId,
            userId = entity.userId,
            displayName = entity.displayName,
            maskedPhone = entity.maskedPhone,
            role = role,
            status = MemberStatus.ACTIVE,
            joinedAt = entity.joinedAt,
            needsManualNotice = false,
            blockRt = blockRt
        )

        // Sync to Cloud Firestore in real-time
        try {
            villageFirestoreRepository?.saveMemberProfile(member)
        } catch (e: Throwable) {
            android.util.Log.w("MemberRepository", "Firestore profile sync skipped: ${e.message}")
        }

        return AppResult.Success(member)
    }

    override suspend fun removeMember(memberId: String): AppResult<Unit> {
        memberDao.deleteMember(memberId)
        return AppResult.Success(Unit)
    }

    override suspend fun initializeDefaultMembersIfEmpty(groupId: String) {
        val baseline = listOf(
            GroupMemberEntity(
                id = "mem_01",
                groupId = groupId,
                userId = "usr_01",
                displayName = "Ketua RT 02 Dusun Jogorejo",
                maskedPhone = phoneProtector.maskPhone("081234567890"),
                role = MemberRole.ADMIN.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 25,
                invitedBy = null,
                needsManualNotice = false,
                blockRt = "RT 02"
            ),
            GroupMemberEntity(
                id = "mem_02",
                groupId = groupId,
                userId = "usr_02",
                displayName = "Bendahara Kas RT 02",
                maskedPhone = phoneProtector.maskPhone("081398765432"),
                role = MemberRole.TREASURER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 24,
                invitedBy = null,
                needsManualNotice = false,
                blockRt = "RT 02"
            ),
            GroupMemberEntity(
                id = "mem_03",
                groupId = groupId,
                userId = "usr_03",
                displayName = "Kader Posyandu RT 01",
                maskedPhone = phoneProtector.maskPhone("082187654321"),
                role = MemberRole.MEMBER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 20,
                invitedBy = "usr_01",
                needsManualNotice = false,
                blockRt = "RT 01"
            ),
            GroupMemberEntity(
                id = "mem_04",
                groupId = groupId,
                userId = "usr_04",
                displayName = "Kader Lingkungan RT 02 (Non-HP)",
                maskedPhone = phoneProtector.maskPhone("085611223344"),
                role = MemberRole.MEMBER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 15,
                invitedBy = "usr_01",
                needsManualNotice = true,
                blockRt = "RT 02"
            ),
            GroupMemberEntity(
                id = "mem_05",
                groupId = groupId,
                userId = "usr_05",
                displayName = "Koordinator Pemuda RT 03",
                maskedPhone = phoneProtector.maskPhone("087799887766"),
                role = MemberRole.MEMBER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 10,
                invitedBy = "usr_01",
                needsManualNotice = false,
                blockRt = "RT 03"
            ),
            GroupMemberEntity(
                id = "mem_06",
                groupId = groupId,
                userId = "usr_06",
                displayName = "Warga Blok A (Jl. Mawar No. 04)",
                maskedPhone = phoneProtector.maskPhone("081298811223"),
                role = MemberRole.MEMBER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 8,
                invitedBy = "usr_01",
                needsManualNotice = false,
                blockRt = "Blok A"
            ),
            GroupMemberEntity(
                id = "mem_07",
                groupId = groupId,
                userId = "usr_07",
                displayName = "Warga Blok B (Jl. Melati No. 12)",
                maskedPhone = phoneProtector.maskPhone("081377889900"),
                role = MemberRole.MEMBER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 6,
                invitedBy = "usr_01",
                needsManualNotice = false,
                blockRt = "Blok B"
            ),
            GroupMemberEntity(
                id = "mem_08",
                groupId = groupId,
                userId = "usr_08",
                displayName = "Warga Blok C (Jl. Kenanga No. 08)",
                maskedPhone = phoneProtector.maskPhone("085733445566"),
                role = MemberRole.MEMBER.name,
                status = MemberStatus.ACTIVE.name,
                joinedAt = System.currentTimeMillis() - 86400000L * 4,
                invitedBy = "usr_01",
                needsManualNotice = false,
                blockRt = "Blok C"
            )
        )
        memberDao.insertMembers(baseline)

        // Sync baseline ke Firestore agar cloud direktori langsung aktif
        baseline.forEach { entity ->
            try {
                villageFirestoreRepository?.saveMemberProfile(
                    GroupMember(
                        id = entity.id,
                        groupId = entity.groupId,
                        userId = entity.userId,
                        displayName = entity.displayName,
                        maskedPhone = entity.maskedPhone,
                        role = MemberRole.valueOf(entity.role),
                        status = MemberStatus.valueOf(entity.status),
                        joinedAt = entity.joinedAt,
                        invitedBy = entity.invitedBy,
                        needsManualNotice = entity.needsManualNotice,
                        blockRt = entity.blockRt
                    )
                )
            } catch (_: Throwable) {}
        }
    }
}
