package com.desa.kuniran.feature.announcements

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.desa.kuniran.core.designsystem.WarmSand
import com.desa.kuniran.core.model.Announcement
import com.desa.kuniran.core.model.AnnouncementTemplate
import com.desa.kuniran.core.ui.VillageTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            VillageTopAppBar(
                title = "Pengumuman Warga",
                subtitle = "Informasi Resmi Lingkungan",
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
                    Icon(Icons.Default.Campaign, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Pengumuman", fontWeight = FontWeight.Bold)
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
            items(state.announcements, key = { it.id }) { item ->
                AnnouncementCard(announcement = item)
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (state.showAddDialog) {
        CreateAnnouncementDialog(
            title = state.newTitle,
            content = state.newContent,
            isPinned = state.isPinned,
            errorMessage = state.errorMessage,
            onApplyTemplate = viewModel::onApplyTemplate,
            onTitleChange = viewModel::onTitleChange,
            onContentChange = viewModel::onContentChange,
            onPinnedToggle = viewModel::onPinnedToggle,
            onSubmit = viewModel::submitAnnouncement,
            onDismiss = viewModel::onDismissAddDialog
        )
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
    val dateString = dateFormat.format(Date(announcement.publishedAt))

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
                if (announcement.isPinned) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarmSand
                    ) {
                        Text(
                            text = "📌 Disematkan",
                            style = MaterialTheme.typography.labelSmall,
                            color = HarvestGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${announcement.readCount}/${announcement.totalRecipients} dibaca",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Oleh: ${announcement.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateAnnouncementDialog(
    title: String,
    content: String,
    isPinned: Boolean,
    errorMessage: String?,
    onApplyTemplate: (AnnouncementTemplate) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPinnedToggle: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Buat Pengumuman Baru", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Template selector (Blueprint Bagian 30 #5)
                Text(
                    text = "Template Siap Pakai:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onApplyTemplate(AnnouncementTemplate.KERJA_BAKTI) },
                        label = { Text("Kerja Bakti") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onApplyTemplate(AnnouncementTemplate.IURAN_WARGA) },
                        label = { Text("Iuran Kas") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onApplyTemplate(AnnouncementTemplate.PENGUMUMAN_UMUM) },
                        label = { Text("Umum") }
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Judul Pengumuman") },
                    placeholder = { Text("Contoh: Kerja Bakti Hari Minggu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("Isi Pengumuman") },
                    placeholder = { Text("Tulis pengumuman dengan jelas...") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = onPinnedToggle
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sematkan di bagian atas Beranda",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Kirimkan Pengumuman")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
