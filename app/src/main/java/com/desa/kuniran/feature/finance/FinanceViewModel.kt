package com.desa.kuniran.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desa.kuniran.core.common.AppResult
import com.desa.kuniran.core.model.CashSummary
import com.desa.kuniran.core.model.Money
import com.desa.kuniran.core.model.MonthlyBalance
import com.desa.kuniran.core.model.Transaction
import com.desa.kuniran.core.model.TransactionType
import com.desa.kuniran.data.repository.FinanceRepository
import com.desa.kuniran.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FinanceViewMode(val label: String) {
    TRANSACTIONS("Mutasi Transaksi"),
    MONTHLY_BALANCE("Rekap Kas Bulanan")
}

enum class TransactionFilter(val label: String) {
    ALL("Semua"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}

data class FinanceUiState(
    val summary: CashSummary = CashSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0),
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val monthlyBalances: List<MonthlyBalance> = emptyList(),
    val viewMode: FinanceViewMode = FinanceViewMode.TRANSACTIONS,
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedPeriod: String? = null, // e.g. "2026-09" or null for all
    val availablePeriods: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val newType: TransactionType = TransactionType.INCOME,
    val newAmountInput: String = "",
    val newCategoryInput: String = "Iuran Kas RT",
    val newDescriptionInput: String = "",
    val errorMessage: String? = null
)

class FinanceViewModel(
    private val financeRepository: FinanceRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private var activeGroupId: String = "group_rt02"

    init {
        viewModelScope.launch {
            groupRepository.observeActiveGroupId().collect { groupId ->
                activeGroupId = groupId
                loadFinanceData(groupId)
            }
        }
    }

    private fun loadFinanceData(groupId: String) {
        viewModelScope.launch {
            financeRepository.observeCashSummary(groupId).collect { summary ->
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            }
        }

        viewModelScope.launch {
            financeRepository.observeMonthlyBalances(groupId).collect { monthlyList ->
                _uiState.update { state ->
                    val periods = monthlyList.map { it.period }.distinct()
                    state.copy(
                        monthlyBalances = monthlyList,
                        availablePeriods = periods
                    )
                }
            }
        }

        viewModelScope.launch {
            financeRepository.observeTransactions(groupId).collect { list ->
                _uiState.update { state ->
                    val periodsFromTx = list.map { MonthlyBalance.formatPeriod(it.occurredAt) }.distinct().sortedDescending()
                    val mergedPeriods = (state.availablePeriods + periodsFromTx).distinct()
                    val filtered = applyFilterAndPeriod(list, state.selectedFilter, state.selectedPeriod)
                    state.copy(
                        transactions = list,
                        filteredTransactions = filtered,
                        availablePeriods = mergedPeriods
                    )
                }
            }
        }
    }

    fun onViewModeChanged(mode: FinanceViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onPeriodSelected(period: String?) {
        _uiState.update { state ->
            val filtered = applyFilterAndPeriod(state.transactions, state.selectedFilter, period)
            state.copy(selectedPeriod = period, filteredTransactions = filtered)
        }
    }

    fun onFilterSelected(filter: TransactionFilter) {
        _uiState.update { state ->
            val filtered = applyFilterAndPeriod(state.transactions, filter, state.selectedPeriod)
            state.copy(selectedFilter = filter, filteredTransactions = filtered)
        }
    }

    private fun applyFilterAndPeriod(
        list: List<Transaction>,
        filter: TransactionFilter,
        period: String?
    ): List<Transaction> {
        return list.filter { tx ->
            val matchesType = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOME -> tx.type == TransactionType.INCOME
                TransactionFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
            }
            val matchesPeriod = if (period.isNullOrBlank()) {
                true
            } else {
                MonthlyBalance.formatPeriod(tx.occurredAt) == period
            }
            matchesType && matchesPeriod
        }
    }

    fun onOpenAddTransaction() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                newType = TransactionType.INCOME,
                newAmountInput = "",
                newCategoryInput = "Iuran Kas RT",
                newDescriptionInput = "",
                errorMessage = null
            )
        }
    }

    fun onDismissAddTransaction() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onTypeChanged(type: TransactionType) {
        val defaultCategory = if (type == TransactionType.INCOME) "Iuran Kas RT" else "Perlengkapan"
        _uiState.update { it.copy(newType = type, newCategoryInput = defaultCategory) }
    }

    fun onAmountChanged(amount: String) {
        val clean = amount.filter { it.isDigit() }
        _uiState.update { it.copy(newAmountInput = clean, errorMessage = null) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(newCategoryInput = category) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(newDescriptionInput = desc, errorMessage = null) }
    }

    fun submitTransaction() {
        val state = _uiState.value
        val amountNum = state.newAmountInput.toLongOrNull() ?: 0L
        if (amountNum <= 0) {
            _uiState.update { it.copy(errorMessage = "Nominal harus lebih dari Rp 0") }
            return
        }
        if (state.newDescriptionInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Keterangan transaksi tidak boleh kosong") }
            return
        }

        viewModelScope.launch {
            when (val result = financeRepository.recordTransaction(
                groupId = activeGroupId,
                type = state.newType,
                amount = Money(amountNum),
                categoryName = state.newCategoryInput,
                description = state.newDescriptionInput,
                occurredAt = System.currentTimeMillis()
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(showAddDialog = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.userMessage) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    companion object {
        fun provideFactory(
            financeRepository: FinanceRepository,
            groupRepository: GroupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FinanceViewModel(financeRepository, groupRepository) as T
            }
        }
    }
}
