package com.desa.kuniran.feature.members

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary
import com.desa.kuniran.core.designsystem.HarvestGold
import com.desa.kuniran.core.designsystem.WarmSand
import com.desa.kuniran.core.model.GroupMember
import com.desa.kuniran.core.model.MemberRole
import com.desa.kuniran.core.ui.VillageTopAppBar

@Composable
fun MemberListScreen(
    viewModel: MembersViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            VillageTopAppBar(
                title = "Direktori Warga Desa Kuniran",
                subtitle = "${state.filteredMembers.size} dari ${state.members.size} Warga • Sinkronisasi Cloud Firestore",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onOpenAddMember,
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_member")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Warga", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("citizen_directory_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Status Cloud Firestore Badge
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Direktori terhubung realtime ke Cloud Firestore Desa Kuniran",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Search Input Field
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Cari warga, RT/Blok, atau nomor HP...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_directory"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Filter Cepat RT / Blok
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Filter Wilayah RT / Blok:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.availableBlocks.forEach { block ->
                            val isSelected = state.selectedBlockFilter == block
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onBlockFilterSelected(block) },
                                label = { Text(block) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_block_${block.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // Filter Cepat Peran
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Peran:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("Semua", "Pengurus", "Warga").forEach { roleFilter ->
                        val isSelected = state.selectedRoleFilter == roleFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onRoleFilterSelected(roleFilter) },
                            label = { Text(roleFilter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HarvestGold,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Empty state
            if (state.filteredMembers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tidak ada warga ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba ubah kata kunci pencarian atau sesuaikan pilihan filter Blok/RT.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    viewModel.onBlockFilterSelected("Semua")
                                    viewModel.onRoleFilterSelected("Semua")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Text("Reset Semua Filter")
                            }
                        }
                    }
                }
            }

            // Member list items
            items(state.filteredMembers, key = { it.id }) { member ->
                MemberCard(member = member)
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (state.showAddDialog) {
        AddMemberDialog(
            name = state.newName,
            phone = state.newPhone,
            blockRt = state.newBlockRt,
            availableBlocks = state.availableBlocks.filter { it != "Semua" },
            normalized = state.normalizedPreview,
            role = state.newRole,
            needsManualNotice = state.needsManualNotice,
            errorMessage = state.errorMessage,
            onNameChange = viewModel::onNameChanged,
            onPhoneChange = viewModel::onPhoneChanged,
            onBlockRtChange = viewModel::onBlockRtChanged,
            onRoleChange = viewModel::onRoleChanged,
            onManualNoticeToggle = viewModel::onManualNoticeToggled,
            onSubmit = viewModel::submitAddMember,
            onDismiss = viewModel::onDismissAddMember
        )
    }
}

@Composable
private fun MemberCard(member: GroupMember) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (member.role.isPrivileged) WarmSand else GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (member.role.isPrivileged) Icons.Default.Shield else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (member.role.isPrivileged) HarvestGold else GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (member.role.isPrivileged) WarmSand else GreenLight
                    ) {
                        Text(
                            text = member.role.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (member.role.isPrivileged) HarvestGold else GreenPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge RT / Blok
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = member.blockRt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Masked phone
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = member.maskedPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (member.needsManualNotice) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "📢 Perlu info lisan/kertas (Non-HP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMemberDialog(
    name: String,
    phone: String,
    blockRt: String,
    availableBlocks: List<String>,
    normalized: String,
    role: MemberRole,
    needsManualNotice: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBlockRtChange: (String) -> Unit,
    onRoleChange: (MemberRole) -> Unit,
    onManualNoticeToggle: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tambah Warga ke Direktori", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nama Warga Lengkap") },
                    placeholder = { Text("Contoh: Pak Suwarno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Nomor Handphone") },
                    placeholder = { Text("Contoh: 081234567890") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_phone"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (normalized.isNotBlank()) {
                    Text(
                        text = "Format E.164: $normalized",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Input Wilayah RT / Blok
                Text(
                    text = "Wilayah Domisili (RT / Blok):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = if (availableBlocks.isNotEmpty()) availableBlocks else listOf("RT 01", "RT 02", "RT 03", "Blok A", "Blok B", "Blok C")
                    presets.forEach { b ->
                        FilterChip(
                            selected = blockRt == b,
                            onClick = { onBlockRtChange(b) },
                            label = { Text(b) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenLight,
                                selectedLabelColor = GreenPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = blockRt,
                    onValueChange = onBlockRtChange,
                    label = { Text("Atau Tulis RT/Blok Spesifik") },
                    placeholder = { Text("Misal: RT 02 / Dusun Jogorejo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_block"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Peran dalam Lingkungan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = role == MemberRole.MEMBER,
                        onClick = { onRoleChange(MemberRole.MEMBER) },
                        label = { Text("Warga") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenLight,
                            selectedLabelColor = GreenPrimary
                        )
                    )
                    FilterChip(
                        selected = role == MemberRole.TREASURER,
                        onClick = { onRoleChange(MemberRole.TREASURER) },
                        label = { Text("Bendahara") }
                    )
                    FilterChip(
                        selected = role == MemberRole.ADMIN,
                        onClick = { onRoleChange(MemberRole.ADMIN) },
                        label = { Text("Admin RT") }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = needsManualNotice,
                        onCheckedChange = onManualNoticeToggle
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Warga lansia/tidak punya smartphone (butuh info lisan/surat kertas)",
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
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier.testTag("btn_save_member")
            ) {
                Text("Simpan ke Cloud Direktori")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

