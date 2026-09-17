package com.desa.kuniran.feature.finance

import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.CashSummary
import com.desa.kuniran.core.model.Group
import com.desa.kuniran.core.model.GroupType
import com.desa.kuniran.core.model.Money
import com.desa.kuniran.core.model.MonthlyBalance
import com.desa.kuniran.core.model.Transaction
import com.desa.kuniran.core.model.TransactionStatus
import com.desa.kuniran.core.model.TransactionType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private val fakeCashSummary = CashSummary(
        balance = Money(411000L),
        totalIncome = Money(750000L),
        totalExpense = Money(339000L),
        transactionCount = 5,
        userFeePaid = true
    )

    private val fakeMonthlyBalanceSept = MonthlyBalance(
        id = "group_rt02_2026-09",
        period = "2026-09",
        groupId = "group_rt02",
        year = 2026,
        month = 9,
        monthName = "September 2026",
        startingBalance = Money(250000L),
        totalIncome = Money(300005L),
        totalExpense = Money(94000L),
        endingBalance = Money(456005L),
        transactionCount = 3
    )

    private val fakeTransactions = listOf(
        Transaction(
            id = "tx_01",
            groupId = "group_rt02",
            accountId = "kas_utama",
            type = TransactionType.INCOME,
            amount = Money(250000L),
            categoryId = "iuran_rt",
            categoryName = "Iuran Kas RT",
            description = "Iuran September",
            createdBy = "bendahara",
            createdByName = "Joko",
            approvedBy = "admin",
            status = TransactionStatus.APPROVED,
            occurredAt = 1725500000000L, // Sept 2026
            createdAt = 1725500000000L
        ),
        Transaction(
            id = "tx_02",
            groupId = "group_rt02",
            accountId = "kas_utama",
            type = TransactionType.EXPENSE,
            amount = Money(94000L),
            categoryId = "alat",
            categoryName = "Perlengkapan",
            description = "Beli sapu dan cat",
            createdBy = "bendahara",
            createdByName = "Joko",
            approvedBy = "admin",
            status = TransactionStatus.APPROVED,
            occurredAt = 1726000000000L, // Sept 2026
            createdAt = 1726000000000L
        )
    )

    private lateinit var financeRepository: FakeFinanceRepository
    private lateinit var groupRepository: FakeGroupRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        financeRepository = FakeFinanceRepository(
            summary = fakeCashSummary,
            transactions = fakeTransactions,
            monthlyBalances = listOf(fakeMonthlyBalanceSept)
        )
        groupRepository = FakeGroupRepository(fakeGroup)

        viewModel = FinanceViewModel(
            financeRepository = financeRepository,
            groupRepository = groupRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFinanceData loads summary, transactions, and monthly balances`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(411000L, state.summary.balance.rupiah)
        assertEquals(2, state.transactions.size)
        assertEquals(1, state.monthlyBalances.size)
        assertEquals("September 2026", state.monthlyBalances.first().monthName)
        assertEquals(FinanceViewMode.TRANSACTIONS, state.viewMode)
    }

    @Test
    fun `onViewModeChanged switches between transactions and monthly balance`() = runTest {
        advanceUntilIdle()
        assertEquals(FinanceViewMode.TRANSACTIONS, viewModel.uiState.value.viewMode)

        viewModel.onViewModeChanged(FinanceViewMode.MONTHLY_BALANCE)
        assertEquals(FinanceViewMode.MONTHLY_BALANCE, viewModel.uiState.value.viewMode)

        viewModel.onViewModeChanged(FinanceViewMode.TRANSACTIONS)
        assertEquals(FinanceViewMode.TRANSACTIONS, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `onFilterSelected filters transactions by Income or Expense`() = runTest {
        advanceUntilIdle()

        viewModel.onFilterSelected(TransactionFilter.INCOME)
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals(TransactionType.INCOME, viewModel.uiState.value.filteredTransactions.first().type)

        viewModel.onFilterSelected(TransactionFilter.EXPENSE)
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.filteredTransactions.first().type)

        viewModel.onFilterSelected(TransactionFilter.ALL)
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
    }

    @Test
    fun `submitTransaction validates empty input and rejects zero amount`() = runTest {
        advanceUntilIdle()
        viewModel.onOpenAddTransaction()
        assertTrue(viewModel.uiState.value.showAddDialog)

        viewModel.onAmountChanged("0")
        viewModel.onDescriptionChanged("")
        viewModel.submitTransaction()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `submitTransaction succeeds with valid input`() = runTest {
        advanceUntilIdle()
        viewModel.onOpenAddTransaction()
        viewModel.onAmountChanged("50000")
        viewModel.onDescriptionChanged("Donasi warga")
        viewModel.submitTransaction()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.showAddDialog)
        assertEquals(1, financeRepository.recordedCount)
    }

    private class FakeFinanceRepository(
        summary: CashSummary,
        transactions: List<Transaction>,
        monthlyBalances: List<MonthlyBalance>
    ) : FinanceRepository {
        private val summaryFlow = MutableStateFlow(summary)
        private val txFlow = MutableStateFlow(transactions)
        private val monthlyFlow = MutableStateFlow(monthlyBalances)
        var recordedCount = 0

        override fun observeTransactions(groupId: String): Flow<List<Transaction>> = txFlow
        override fun observeTransactionsByPeriod(groupId: String, period: String): Flow<List<Transaction>> = txFlow
        override fun observeCashSummary(groupId: String): Flow<CashSummary> = summaryFlow
        override fun observeMonthlyBalances(groupId: String): Flow<List<MonthlyBalance>> = monthlyFlow

        override suspend fun recordTransaction(
            groupId: String,
            type: TransactionType,
            amount: Money,
            categoryName: String,
            description: String,
            occurredAt: Long
        ): AppResult<Transaction> {
            recordedCount++
            val tx = Transaction(
                id = "tx_new_$recordedCount",
                groupId = groupId,
                accountId = "kas_utama",
                type = type,
                amount = amount,
                categoryId = "cat_test",
                categoryName = categoryName,
                description = description,
                createdBy = "user",
                createdByName = "User",
                approvedBy = "admin",
                status = TransactionStatus.APPROVED,
                occurredAt = occurredAt,
                createdAt = occurredAt
            )
            return AppResult.Success(tx)
        }

        override suspend fun recalculateMonthlyBalances(groupId: String) {}
        override suspend fun syncWithFirestore(groupId: String) {}
        override suspend fun initializeDefaultFinanceIfEmpty(groupId: String) {}
    }

    private class FakeGroupRepository(private val group: Group) : GroupRepository {
        private val activeIdFlow = MutableStateFlow(group.id)
        override fun observeGroups(): Flow<List<Group>> = MutableStateFlow(listOf(group))
        override fun observeActiveGroupId(): Flow<String> = activeIdFlow
        override suspend fun getActiveGroup(): Group? = group
        override suspend fun selectGroup(groupId: String) {
            activeIdFlow.value = groupId
        }
        override suspend fun createGroup(group: Group): AppResult<Group> = AppResult.Success(group)
        override suspend fun joinGroupByCode(code: String): AppResult<Group> = AppResult.Success(group)
        override suspend fun initializeDefaultGroupsIfEmpty() {}
    }
}
