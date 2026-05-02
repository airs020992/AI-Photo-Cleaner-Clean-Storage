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
import androidx.compose.ui.res.stringResource
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
            text = stringResource(R.string.dashboard_section_biggest_wins),
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
            text = stringResource(R.string.dashboard_footer_trust),
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
                text = stringResource(R.string.dashboard_summary_label),
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
                    label = { Text(stringResource(R.string.dashboard_chip_safe_preview)) },
                )
            }
        }
    }
}

@Composable
fun localizedPreviewCleanupCategories(): List<CleanupCategory> {
    return listOf(
        CleanupCategory(
            id = "large_videos",
            title = stringResource(R.string.category_large_videos_title),
            subtitle = stringResource(R.string.category_large_videos_subtitle),
            recoverableLabel = "5.2 GB",
            actionLabel = stringResource(R.string.action_open),
            priority = CleanupPriority.High,
        ),
        CleanupCategory(
            id = "similar_photos",
            title = stringResource(R.string.category_similar_photos_title),
            subtitle = stringResource(R.string.category_similar_photos_subtitle),
            recoverableLabel = "3.4 GB",
            actionLabel = stringResource(R.string.action_review),
            priority = CleanupPriority.High,
        ),
        CleanupCategory(
            id = "duplicate_photos",
            title = stringResource(R.string.category_duplicate_photos_title),
            subtitle = stringResource(R.string.category_duplicate_photos_subtitle),
            recoverableLabel = "1.8 GB",
            actionLabel = stringResource(R.string.action_review),
            priority = CleanupPriority.Medium,
        ),
        CleanupCategory(
            id = "screenshots",
            title = stringResource(R.string.category_screenshots_title),
            subtitle = stringResource(R.string.category_screenshots_subtitle),
            recoverableLabel = "820 MB",
            actionLabel = stringResource(R.string.action_review),
            priority = CleanupPriority.Medium,
        ),
        CleanupCategory(
            id = "blurry_photos",
            title = stringResource(R.string.category_blurry_photos_title),
            subtitle = stringResource(R.string.category_blurry_photos_subtitle),
            recoverableLabel = "640 MB",
            actionLabel = stringResource(R.string.action_review),
            priority = CleanupPriority.Low,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    CleanerTheme {
        DashboardScreen(
            recoverableSpaceLabel = "11.9 GB",
            scannedItemsLabel = stringResource(R.string.dashboard_scanned_items_preview),
            categories = localizedPreviewCleanupCategories(),
            onCategoryClick = {},
        )
    }
}
