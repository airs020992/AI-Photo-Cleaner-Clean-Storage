package com.air.cleaner.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.air.cleaner.core.ui.components.ActionCard
import com.air.cleaner.core.ui.theme.CleanerTheme

@Composable
fun DashboardScreen(
    recoverableSpaceLabel: String,
    scannedItemsLabel: String,
    categories: List<CleanupCategory>,
    onCategoryClick: (CleanupCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StorageSummary(
            recoverableSpaceLabel = recoverableSpaceLabel,
            scannedItemsLabel = scannedItemsLabel,
        )

        Text(
            text = "Start with the biggest wins",
            style = MaterialTheme.typography.titleMedium,
        )

        categories.forEach { category ->
            ActionCard(
                title = category.title,
                subtitle = category.subtitle,
                metric = category.recoverableLabel,
                actionLabel = category.actionLabel,
                onAction = { onCategoryClick(category) },
            )
        }

        Text(
            text = "You review everything before deleting. Premium and ads appear only after useful scan results.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StorageSummary(
    recoverableSpaceLabel: String,
    scannedItemsLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "You can free up",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = recoverableSpaceLabel,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(scannedItemsLabel) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Safe preview") },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    CleanerTheme {
        DashboardScreen(
            recoverableSpaceLabel = "11.9 GB",
            scannedItemsLabel = "12,480 items scanned",
            categories = previewCleanupCategories,
            onCategoryClick = {},
        )
    }
}
