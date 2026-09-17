package com.desa.kuniran.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.StatusError
import com.desa.kuniran.core.designsystem.StatusSuccess
import com.desa.kuniran.core.designsystem.WarmSand
import com.desa.kuniran.core.model.Transaction
import com.desa.kuniran.core.model.TransactionType
import com.desa.kuniran.core.ui.CurrencyDisplay
import com.desa.kuniran.core.ui.SyncStatusBadge
import com.desa.kuniran.core.ui.VillageTopAppBar
import com.desa.kuniran.feature.finance.components.MonthlyBalanceView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                VillageTopAppBar(
                    title = "Kas & Keuangan Warga",
                    subtitle = "Transparan, Realtime & Akuntabel",
                    onBackClick = onBackClick
                )

                // Tab Navigasi: Mutasi Transaksi vs Rekap Kas Bulanan
                TabRow(
                    selectedTabIndex = if (state.viewMode == FinanceViewMode.TRANSACTIONS) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GreenPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (state.viewMode == FinanceViewMode.TRANSACTIONS) 0 else 1]),
                            color = GreenPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = state.viewMode == FinanceViewMode.TRANSACTIONS,
                        onClick = { viewModel.onViewModeChanged(FinanceViewMode.TRANSACTIONS) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buku Mutasi",
                                    fontWeight = if (state.viewMode == FinanceViewMode.TRANSACTIONS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_mutasi_transaksi")
                    )

                    Tab(
                        selected = state.viewMode == FinanceViewMode.MONTHLY_BALANCE,
                        onClick = { viewModel.onViewModeChanged(FinanceViewMode.MONTHLY_BALANCE) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rekap Bulanan",
                                    fontWeight = if (state.viewMode == FinanceViewMode.MONTHLY_BALANCE) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_rekap_bulanan")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onOpenAddTransaction,
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Kas", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        when (state.viewMode) {
            FinanceViewMode.MONTHLY_BALANCE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    item {
                        MonthlyBalanceView(
                            monthlyBalances = state.monthlyBalances,
                            onSelectMonthForTransactions = { period ->
                                viewModel.onPeriodSelected(period)
                                viewModel.onViewModeChanged(FinanceViewMode.TRANSACTIONS)
                            }
                        )
                    }
                }
            }

            FinanceViewMode.TRANSACTIONS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                        .testTag("transactions_list_view"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Saldo Card Kas Lingkungan
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("general_balance_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SALDO KAS LINGKUNGAN",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    SyncStatusBadge(isSynced = true)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                CurrencyDisplay(
                                    money = state.summary.balance,
                                    style = MaterialTheme.typography.displayLarge.copy(color = Color.White)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Pemasukan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "+ ${state.summary.totalIncome.toFormattedRupiah()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Total Pengeluaran",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "- ${state.summary.totalExpense.toFormattedRupiah()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Filter Bar: Period filter chip (if selected) + Type filter chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Active month filter tag
                            if (state.selectedPeriod != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InputChip(
                                        selected = true,
                                        onClick = { viewModel.onPeriodSelected(null) },
                                        label = { Text("Filter Bulan: ${state.selectedPeriod}") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus filter bulan",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = WarmSand,
                                            selectedLabelColor = GreenPrimary
                                        ),
                                        modifier = Modifier.testTag("chip_active_period_filter")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { viewModel.onPeriodSelected(null) }) {
                                        Text("Lihat Semua Bulan", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // Filter Tabs (Semua, Pemasukan, Pengeluaran)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TransactionFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = state.selectedFilter == filter,
                                        onClick = { viewModel.onFilterSelected(filter) },
                                        label = { Text(filter.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GreenLight,
                                            selectedLabelColor = GreenPrimary
                                        ),
                                        modifier = Modifier.testTag("filter_chip_${filter.name}")
                                    )
                                }
                            }
                        }
                    }

                    // 3. Transactions Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Riwayat Catatan Transaksi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${state.filteredTransactions.size} Transaksi",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 4. Transaction Items or Empty View
                    if (state.filteredTransactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tidak Ada Transaksi",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Belum ada catatan mutasi kas untuk filter yang dipilih.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.filteredTransactions, key = { it.id }) { tx ->
                            TransactionListItem(transaction = tx)
                        }
                    }

                    // Bottom spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddTransactionDialog(
            type = state.newType,
            amount = state.newAmountInput,
            category = state.newCategoryInput,
            description = state.newDescriptionInput,
            errorMessage = state.errorMessage,
            onTypeChange = viewModel::onTypeChanged,
            onAmountChange = viewModel::onAmountChanged,
            onCategoryChange = viewModel::onCategoryChanged,
            onDescriptionChange = viewModel::onDescriptionChanged,
            onSubmit = viewModel::submitTransaction,
            onDismiss = viewModel::onDismissAddTransaction
        )
    }
}

@Composable
private fun TransactionListItem(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("in", "ID"))
    val dateString = dateFormat.format(Date(transaction.occurredAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) GreenLight else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) StatusSuccess else StatusError,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.categoryName} • ${transaction.createdByName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CurrencyDisplay(
                money = transaction.amount,
                isPositive = isIncome,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    type: TransactionType,
    amount: String,
    category: String,
    description: String,
    errorMessage: String?,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val incomeCategories = listOf("Iuran Kas RT", "Jimpitan Warga", "Donasi Warga", "Lainnya")
    val expenseCategories = listOf("Perlengkapan", "Konsumsi Acara", "Pemeliharaan Pos Ronda", "Kebersihan", "Lainnya")
    val currentCategories = if (type == TransactionType.INCOME) incomeCategories else expenseCategories
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Catat Transaksi Kas", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        label = { Text("Pemasukan (+)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenLight,
                            selectedLabelColor = GreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        label = { Text("Pengeluaran (-)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Nominal Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Nominal (Rp)") },
                    placeholder = { Text("Contoh: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        currentCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onCategoryChange(cat)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Keterangan") },
                    placeholder = { Text("Contoh: Beli sapu lidi untuk kerja bakti") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = amount.isNotBlank() && description.isNotBlank()
            ) {
                Text("Simpan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
