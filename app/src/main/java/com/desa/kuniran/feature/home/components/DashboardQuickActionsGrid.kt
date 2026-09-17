package com.desa.kuniran.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.desa.kuniran.core.designsystem.GreenLight
import com.desa.kuniran.core.designsystem.GreenPrimary

/**
 * Grid Navigasi Aksi Cepat Dashboard untuk warga desa.
 */
@Composable
fun DashboardQuickActionsGrid(
    onAnnouncementsClick: () -> Unit,
    onActivitiesClick: () -> Unit,
    onFinanceClick: () -> Unit,
    onComplaintsClick: () -> Unit,
    onMembersClick: () -> Unit,
    onEnvironmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardQuickActionButton(
                label = "Pengumuman",
                icon = Icons.Default.Campaign,
                onClick = onAnnouncementsClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_announcements")
            )
            DashboardQuickActionButton(
                label = "Kegiatan",
                icon = Icons.Default.Event,
                onClick = onActivitiesClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_activities")
            )
            DashboardQuickActionButton(
                label = "Kas Warga",
                icon = Icons.Default.Payment,
                onClick = onFinanceClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_finance")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardQuickActionButton(
                label = "Pengaduan",
                icon = Icons.Default.ReportProblem,
                onClick = onComplaintsClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_complaints")
            )
            DashboardQuickActionButton(
                label = "Warga & RT",
                icon = Icons.Default.Groups,
                onClick = onMembersClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_members")
            )
            DashboardQuickActionButton(
                label = "Lingkungan",
                icon = Icons.Default.Delete,
                onClick = onEnvironmentClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_environment")
            )
        }
    }
}

@Composable
private fun DashboardQuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.heightIn(min = 76.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
