package com.desa.kuniran.feature.finance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.StatusError
import com.desa.kuniran.core.designsystem.StatusSuccess
import com.desa.kuniran.core.designsystem.WarmSand
import com.desa.kuniran.core.model.MonthlyBalance
import com.desa.kuniran.core.ui.CurrencyDisplay

enum class VisualizationType(val label: String) {
    BAR("Grafik Batang"),
    PIE("Diagram Lingkaran")
}

/**
 * Komponen visualisasi data interaktif Kas Warga.
 * Menyediakan grafik batang perbandingan bulanan dan diagram donat proporsi kas
 * agar warga desa dapat memantau kas secara visual dan transparan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashVisualizationCard(
    monthlyBalances: List<MonthlyBalance>,
    modifier: Modifier = Modifier
) {
    if (monthlyBalances.isEmpty()) return

    var selectedChartType by remember { mutableStateOf(VisualizationType.BAR) }
    var selectedMonthIndex by remember { mutableStateOf(0) }
    val currentMonth = monthlyBalances.getOrNull(selectedMonthIndex) ?: monthlyBalances.first()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_visualization_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header & Chart Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Visualisasi Kas Warga",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ringkasan grafis arus kas & saldo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedChartType == VisualizationType.BAR) GreenPrimary else GreenLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedChartType = VisualizationType.BAR }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Grafik Batang",
                                tint = if (selectedChartType == VisualizationType.BAR) Color.White else GreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Batang",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedChartType == VisualizationType.BAR) Color.White else GreenPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedChartType == VisualizationType.PIE) GreenPrimary else GreenLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedChartType = VisualizationType.PIE }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Diagram Lingkaran",
                                tint = if (selectedChartType == VisualizationType.PIE) Color.White else GreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lingkaran",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedChartType == VisualizationType.PIE) Color.White else GreenPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Content based on Type
            when (selectedChartType) {
                VisualizationType.BAR -> {
                    CashBarChartSection(monthlyBalances = monthlyBalances.take(4).reversed())
                }
                VisualizationType.PIE -> {
                    // Month selector chips for pie chart
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        monthlyBalances.take(3).forEachIndexed { idx, balance ->
                            FilterChip(
                                selected = selectedMonthIndex == idx,
                                onClick = { selectedMonthIndex = idx },
                                label = {
                                    Text(
                                        text = "${balance.monthName} ${balance.year}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    CashPieChartSection(balance = currentMonth)
                }
            }
        }
    }
}

/**
 * Komponen Grafik Batang Kustom menggunakan Jetpack Compose Canvas.
 * Membandingkan Pemasukan Kas (Hijau) vs Pengeluaran Kas (Merah) per bulan.
 */
@Composable
private fun CashBarChartSection(
    monthlyBalances: List<MonthlyBalance>
) {
    val incomeColor = StatusSuccess
    val expenseColor = StatusError
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val maxAmount = remember(monthlyBalances) {
        val maxVal = monthlyBalances.flatMap { listOf(it.totalIncome.rupiah, it.totalExpense.rupiah) }.maxOrNull() ?: 1L
        if (maxVal <= 0L) 1000000L else maxVal
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 28.dp.toPx()
                val bottomY = canvasHeight

                // Draw background horizontal guide lines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = bottomY - (canvasHeight * (i.toFloat() / gridLines))
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Calculate group width
                val count = monthlyBalances.size
                if (count > 0) {
                    val groupWidth = canvasWidth / count
                    val barWidth = (groupWidth * 0.28f).coerceAtMost(28.dp.toPx())
                    val barGap = 6.dp.toPx()

                    monthlyBalances.forEachIndexed { index, balance ->
                        val groupCenterX = (index * groupWidth) + (groupWidth / 2f)

                        // Income bar
                        val incomeRatio = (balance.totalIncome.rupiah.toFloat() / maxAmount).coerceIn(0.04f, 1f)
                        val incomeBarHeight = canvasHeight * incomeRatio
                        val incomeLeft = groupCenterX - barWidth - (barGap / 2f)
                        val incomeTop = bottomY - incomeBarHeight

                        drawRoundRect(
                            color = incomeColor,
                            topLeft = Offset(incomeLeft, incomeTop),
                            size = Size(barWidth, incomeBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Expense bar
                        val expenseRatio = (balance.totalExpense.rupiah.toFloat() / maxAmount).coerceIn(0.04f, 1f)
                        val expenseBarHeight = canvasHeight * expenseRatio
                        val expenseLeft = groupCenterX + (barGap / 2f)
                        val expenseTop = bottomY - expenseBarHeight

                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(expenseLeft, expenseTop),
                            size = Size(barWidth, expenseBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            // Month Labels on X Axis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                monthlyBalances.forEach { balance ->
                    val shortMonth = balance.monthName.take(3)
                    Text(
                        text = "$shortMonth '${balance.year.toString().takeLast(2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(incomeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pemasukan Kas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(expenseColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pengeluaran Kas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Komponen Donut / Pie Chart menggunakan Jetpack Compose Canvas.
 * Menampilkan proporsi pengeluaran vs sisa kas bulan berjalan secara transparan.
 */
@Composable
private fun CashPieChartSection(
    balance: MonthlyBalance
) {
    val totalIncome = balance.totalIncome.rupiah
    val totalExpense = balance.totalExpense.rupiah
    val endingBalance = balance.endingBalance.rupiah

    val expenseColor = StatusError
    val remainingColor = StatusSuccess
    val emptyColor = MaterialTheme.colorScheme.outlineVariant

    // Hitung proporsi
    val totalFlow = (totalExpense + endingBalance).coerceAtLeast(1L)
    val expenseAngle = ((totalExpense.toDouble() / totalFlow) * 360.0).toFloat().coerceIn(0f, 360f)
    val remainingAngle = 360f - expenseAngle

    val expensePercent = if (totalFlow > 0) ((totalExpense.toDouble() / totalFlow) * 100).toInt() else 0
    val remainingPercent = 100 - expensePercent

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Donut Canvas
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 22.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val arcSize = Size(diameter, diameter)
                    val offset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    if (totalFlow <= 1L) {
                        drawArc(
                            color = emptyColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    } else {
                        // Sisa Saldo / Surplus Arc
                        drawArc(
                            color = remainingColor,
                            startAngle = -90f,
                            sweepAngle = remainingAngle,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        // Pengeluaran Arc
                        drawArc(
                            color = expenseColor,
                            startAngle = -90f + remainingAngle,
                            sweepAngle = expenseAngle,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }

                // Center Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Saldo Akhir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = balance.endingBalance.toFormattedRupiah(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }

            // Legend Breakdown
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sisa Saldo Item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(remainingColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Saldo Tersisa",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$remainingPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = balance.endingBalance.toFormattedRupiah(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = remainingColor
                    )
                }

                // Pengeluaran Item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(expenseColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$expensePercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = balance.totalExpense.toFormattedRupiah(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor
                    )
                }

                // Total Pemasukan
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Masuk:",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenPrimary
                        )
                        Text(
                            text = balance.totalIncome.toFormattedRupiah(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }
            }
        }
    }
}
