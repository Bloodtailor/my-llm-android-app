package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.util.AppConstants

/**
 * Bottom navigation bar component
 */
@Composable
fun AppBottomNavigation(
    currentPage: Int,
    isParametersEnabled: Boolean,
    onNavigateToChat: () -> Unit,
    onNavigateToParameters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Chat tab
            NavigationTab(
                icon = Icons.Default.Chat,
                label = "Chat",
                isSelected = currentPage == 0,
                enabled = true,
                onClick = onNavigateToChat
            )

            // Parameters tab
            NavigationTab(
                icon = Icons.Default.Tune,
                label = "Parameters",
                isSelected = currentPage == 1,
                enabled = isParametersEnabled,
                onClick = onNavigateToParameters
            )
        }
    }
}

/**
 * Individual navigation tab component
 */
@Composable
private fun NavigationTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                if (enabled) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}