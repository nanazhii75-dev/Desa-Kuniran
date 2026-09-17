package com.desa.kuniran.feature.home

import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.AccountStatus
import com.desa.kuniran.core.model.ActivityItem
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.CashSummary
import com.desa.kuniran.core.model.Group
import com.desa.kuniran.core.model.GroupType
import com.desa.kuniran.core.model.Money
import com.desa.kuniran.core.model.ParticipantStatus
import com.desa.kuniran.core.model.Transaction
import com.desa.kuniran.core.model.TransactionType
import com.desa.kuniran.core.model.User
import com.desa.kuniran.data.repository.ActivityRepository
import com.desa.kuniran.data.repository.AnnouncementRepository
import com.desa.kuniran.data.repository.AuthRepository
import com.desa.kuniran.data.repository.FinanceRepository
import com.desa.kuniran.data.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeUser = User(
        id = "user_123",
        displayName = "Pak RT Kuniran",
        maskedPhone = "0812****890",
        avatarUrl = null,
        accountStatus = AccountStatus.ACTIVE
    )

    private val fakeGroup = Group(
        id = "group_rt02",
        name = "RT 02 / RW 04 Jogorejo",
        description = "Warga RT 02",
        groupCode = "KNR02",
        groupType = GroupType.RT,
        address = "Jogorejo, Kuniran",
        createdBy = "admin",
        memberCount = 48
    )

    private val fakeAnnouncement = Announcement(
        id = "ann_01",
        groupId = "group_rt02",
        title = "Kerja Bakti Saluran Air",
        content = "Kerja bakti pembersihan saluran air menjelang musim hujan.",
        authorName = "Pengurus RT",
        isPinned = true,
        readCount = 35,
        totalRecipients = 48
    )

    private val fakeActivity = ActivityItem(
        id = "act_01",
        groupId = "group_rt02",
        title = "Rapat Kas RT & Ronda",
        description = "Pertemuan bulanan warga membahas jadwal ronda malam.",
        dateText = "Sabtu, 20 Sept • 19.30 WIB",
        location = "Balai RT 02",
        participantsCount = 18,
        userStatus = ParticipantStatus.NONE
    )

    private val fakeCashSummary = CashSummary(
        balance = Money(411000L),
        totalIncome = Money(505000L),
        totalExpense = Money(94000L),
        transactionCount = 12,
        userFeePaid = true
    )

    private lateinit var authRepository: FakeAuthRepo
    private lateinit var groupRepository: FakeGroupRepo
    private lateinit var announcementRepository: FakeAnnouncementRepo
    private lateinit var activityRepository: FakeActivityRepo
    private lateinit var financeRepository: FakeFinanceRepo

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepo(fakeUser)
        groupRepository = FakeGroupRepo(fakeGroup)
        announcementRepository = FakeAnnouncementRepo(listOf(fakeAnnouncement))
        activityRepository = FakeActivityRepo(listOf(fakeActivity))
        financeRepository = FakeFinanceRepo(fakeCashSummary)

        viewModel = HomeViewModel(
            authRepository = authRepository,
            groupRepository = groupRepository,
            announcementRepository = announcementRepository,
            activityRepository = activityRepository,
            financeRepository = financeRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboardData populates state from repositories`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("Pak RT Kuniran", state.currentUser?.displayName)
        assertEquals("group_rt02", state.activeGroup?.id)
        assertNotNull(state.latestAnnouncement)
        assertEquals("Kerja Bakti Saluran Air", state.latestAnnouncement?.title)
        assertNotNull(state.upcomingActivity)
        assertEquals("Rapat Kas RT & Ronda", state.upcomingActivity?.title)
        assertEquals(411000L, state.cashSummary.balance.rupiah)
        assertTrue(state.cashSummary.userFeePaid)
    }

    @Test
    fun `onRsvpChanged updates participant status directly in uiState`() = runTest {
        advanceUntilIdle()
        viewModel.onRsvpChanged("act_01", ParticipantStatus.GOING)
        advanceUntilIdle()

        val updatedActivity = viewModel.uiState.value.upcomingActivity
        assertEquals(ParticipantStatus.GOING, updatedActivity?.userStatus)
        assertEquals(ParticipantStatus.GOING, activityRepository.lastUpdatedRsvpStatus)
    }

    private class FakeAuthRepo(private val user: User?) : AuthRepository {
        private val userFlow = MutableStateFlow(user)
        override fun observeCurrentUser(): Flow<User?> = userFlow
        override suspend fun requestOtp(rawPhone: String): AppResult<Int> = AppResult.Success(60)
        override suspend fun verifyOtp(rawPhone: String, otpCode: String): AppResult<User> =
            user?.let { AppResult.Success(it) } ?: AppResult.Error(com.desa.kuniran.core.common.AppError.Unauthorized("User not found"))
        override suspend fun signInWithGoogle(idToken: String, displayName: String?, email: String?, photoUrl: String?): AppResult<User> =
            user?.let { AppResult.Success(it) } ?: AppResult.Error(com.desa.kuniran.core.common.AppError.Unauthorized("User not found"))
        override suspend fun updateProfile(displayName: String): AppResult<User> =
            user?.let { AppResult.Success(it.copy(displayName = displayName)) } ?: AppResult.Error(com.desa.kuniran.core.common.AppError.Unauthorized("User not found"))
        override suspend fun logout() { userFlow.value = null }
        override fun hasSession(): Boolean = user != null
    }

    private class FakeGroupRepo(private val initialGroup: Group) : GroupRepository {
        private val activeGroupIdFlow = MutableStateFlow(initialGroup.id)
        private val groupsFlow = MutableStateFlow(listOf(initialGroup))
        override fun observeActiveGroupId(): Flow<String> = activeGroupIdFlow
        override fun observeGroups(): Flow<List<Group>> = groupsFlow
        override suspend fun getActiveGroup(): Group = initialGroup
        override suspend fun selectGroup(groupId: String) { activeGroupIdFlow.value = groupId }
        override suspend fun createGroup(group: Group): AppResult<Group> = AppResult.Success(group)
        override suspend fun joinGroupByCode(code: String): AppResult<Group> = AppResult.Success(initialGroup)
        override suspend fun initializeDefaultGroupsIfEmpty() {}
    }

    private class FakeAnnouncementRepo(private val list: List<Announcement>) : AnnouncementRepository {
        private val flow = MutableStateFlow(list)
        override fun observeAnnouncements(groupId: String): Flow<List<Announcement>> = flow
        override suspend fun createAnnouncement(groupId: String, title: String, content: String, isPinned: Boolean): AppResult<Announcement> =
            AppResult.Success(list.first())
        override suspend fun initializeDefaultAnnouncementsIfEmpty(groupId: String) {}
    }

    private class FakeActivityRepo(private val list: List<ActivityItem>) : ActivityRepository {
        private val flow = MutableStateFlow(list)
        var lastUpdatedRsvpStatus: ParticipantStatus? = null

        override fun observeActivities(groupId: String): Flow<List<ActivityItem>> = flow
        override suspend fun createActivity(groupId: String, title: String, description: String, dateText: String, location: String): AppResult<ActivityItem> =
            AppResult.Success(list.first())
        override suspend fun rsvpActivity(activityId: String, status: ParticipantStatus): AppResult<Unit> {
            lastUpdatedRsvpStatus = status
            flow.value = flow.value.map {
                if (it.id == activityId) it.copy(userStatus = status) else it
            }
            return AppResult.Success(Unit)
        }
        override suspend fun initializeDefaultActivitiesIfEmpty(groupId: String) {}
    }

    private class FakeFinanceRepo(private val summary: CashSummary) : FinanceRepository {
        private val flow = MutableStateFlow(summary)
        override fun observeTransactions(groupId: String): Flow<List<Transaction>> = MutableStateFlow(emptyList())
        override fun observeTransactionsByPeriod(groupId: String, period: String): Flow<List<Transaction>> = MutableStateFlow(emptyList())
        override fun observeCashSummary(groupId: String): Flow<CashSummary> = flow
        override fun observeMonthlyBalances(groupId: String): Flow<List<com.desa.kuniran.core.model.MonthlyBalance>> = MutableStateFlow(emptyList())
        override suspend fun recordTransaction(
            groupId: String,
            type: TransactionType,
            amount: Money,
            categoryName: String,
            description: String,
            occurredAt: Long
        ): AppResult<Transaction> =
            AppResult.Error(com.desa.kuniran.core.common.AppError.ValidationError("Not implemented"))
        override suspend fun recalculateMonthlyBalances(groupId: String) {}
        override suspend fun syncWithFirestore(groupId: String) {}
        override suspend fun initializeDefaultFinanceIfEmpty(groupId: String) {}
    }
}
