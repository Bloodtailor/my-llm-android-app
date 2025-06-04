package com.bloodtailor.myllmapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodtailor.myllmapp.network.ContextUsage

/**
 * Status message display component
 */
@Composable
fun StatusMessage(message: String) {
    if (message.isNotEmpty()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

/**
 * Context usage display component
 */
@Composable
fun ContextUsageDisplay(
    contextUsage: ContextUsage?,
    modifier: Modifier = Modifier
) {
    if (contextUsage == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Context Usage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${contextUsage.tokenCount} / ${contextUsage.maxContext} tokens",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${contextUsage.usagePercentage}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = when {
                        contextUsage.usagePercentage < 75 -> MaterialTheme.colorScheme.primary
                        contextUsage.usagePercentage < 90 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )

                // Progress indicator
                LinearProgressIndicator(
                    progress = (contextUsage.usagePercentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.width(80.dp),
                    color = when {
                        contextUsage.usagePercentage < 75 -> MaterialTheme.colorScheme.primary
                        contextUsage.usagePercentage < 90 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}