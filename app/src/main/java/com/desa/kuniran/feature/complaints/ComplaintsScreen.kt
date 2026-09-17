package com.desa.kuniran.feature.complaints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.HarvestGold
import com.desa.kuniran.core.designsystem.StatusSuccess
import com.desa.kuniran.core.designsystem.WarmSand
import com.desa.kuniran.core.model.Complaint
import com.desa.kuniran.core.model.ComplaintCategory
import com.desa.kuniran.core.model.ComplaintStatus
import com.desa.kuniran.core.ui.VillageTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ComplaintsScreen(
    viewModel: ComplaintsViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            VillageTopAppBar(
                title = "Pengaduan & Fasilitas",
                subtitle = "Sampaikan Masalah Lingkungan Anda",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onOpenAddDialog,
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Pengaduan", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Kategori Laporan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedCategoryFilter == null,
                                onClick = { viewModel.onSelectCategoryFilter(null) },
                                label = { Text("Semua") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenLight,
                                    selectedLabelColor = GreenPrimary
                                )
                            )
                        }
                        items(ComplaintCategory.entries) { cat ->
                            FilterChip(
                                selected = state.selectedCategoryFilter == cat,
                                onClick = { viewModel.onSelectCategoryFilter(cat) },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenLight,
                                    selectedLabelColor = GreenPrimary
                                )
                            )
                        }
                    }
                }
            }

            if (state.filteredComplaints.isEmpty() && !state.isLoading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum ada laporan untuk kategori ini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lingkungan terpantau aman dan tertib.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.filteredComplaints, key = { it.id }) { complaint ->
                    ComplaintCard(complaint = complaint)
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (state.showAddDialog) {
        CreateComplaintDialog(
            category = state.newCategory,
            authorName = state.newAuthorName,
            desc = state.newDescription,
            location = state.newLocation,
            errorMessage = state.errorMessage,
            onCategoryChange = viewModel::onCategoryChange,
            onAuthorNameChange = viewModel::onAuthorNameChange,
            onDescChange = viewModel::onDescriptionChange,
            onLocationChange = viewModel::onLocationChange,
            onSubmit = viewModel::submitComplaint,
            onDismiss = viewModel::onDismissAddDialog
        )
    }
}

@Composable
private fun ComplaintCard(complaint: Complaint) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
    val dateString = dateFormat.format(Date(complaint.createdAt))

    val (statusColor, statusBg, statusText) = when (complaint.status) {
        ComplaintStatus.SUBMITTED -> Triple(Color(0xFF0284C7), Color(0xFFE0F2FE), "Terkirim")
        ComplaintStatus.VERIFIED -> Triple(HarvestGold, WarmSand, "Diverifikasi")
        ComplaintStatus.IN_PROGRESS -> Triple(HarvestGold, WarmSand, "Sedang Dikerjakan")
        ComplaintStatus.RESOLVED -> Triple(StatusSuccess, GreenLight, "Selesai Ditangani ✓")
        ComplaintStatus.REJECTED -> Triple(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.12f), "Ditolak")
        ComplaintStatus.CLOSED -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, "Ditutup")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenLight
                ) {
                    Text(
                        text = complaint.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = complaint.description,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pelapor: ${complaint.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${complaint.location} • $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!complaint.officerNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Tanggapan Pengurus / Petugas:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = complaint.officerNotes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateComplaintDialog(
    category: ComplaintCategory,
    authorName: String,
    desc: String,
    location: String,
    errorMessage: String?,
    onCategoryChange: (ComplaintCategory) -> Unit,
    onAuthorNameChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        ComplaintCategory.LAMPU,
        ComplaintCategory.SAMPAH,
        ComplaintCategory.JALAN,
        ComplaintCategory.AIR,
        ComplaintCategory.KEAMANAN,
        ComplaintCategory.FASILITAS
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sampaikan Laporan / Keluhan", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pilih Kategori:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat.label.take(8)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenLight,
                                selectedLabelColor = GreenPrimary
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat.label.take(8)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenLight,
                                selectedLabelColor = GreenPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = authorName,
                    onValueChange = onAuthorNameChange,
                    label = { Text("Nama Pelapor") },
                    placeholder = { Text("Nama Anda / Warga RT 02") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Lokasi Masalah") },
                    placeholder = { Text("Contoh: Pertigaan Gang 2 RT 02") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text("Uraian Masalah") },
                    placeholder = { Text("Jelaskan kerusakan/masalah yang terjadi...") },
                    minLines = 3,
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
                enabled = location.isNotBlank() && desc.isNotBlank()
            ) {
                Text("Kirim Laporan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
