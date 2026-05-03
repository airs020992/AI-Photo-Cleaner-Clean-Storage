package com.air.cleaner.feature.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.air.cleaner.core.ui.theme.CleanerTheme
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import kotlin.math.roundToInt

@Composable
fun PhotoReviewScreen(
    title: String,
    groups: List<DuplicateGroup>,
    onBack: () -> Unit,
    onContinue: (PhotoReviewSelectionState) -> Unit,
    modifier: Modifier = Modifier,
    postDeleteStatus: PhotoPostDeleteStatus? = null,
    noticeTitle: String? = null,
    noticeMessage: String? = null,
    emptyTitle: String = "No duplicate photos found",
    emptyMessage: String = "We only show likely matches for review. Nothing is selected or deleted automatically.",
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    itemMatchLabel: String = "Duplicate",
    groupMatchExplanation: ((DuplicateGroup) -> String?)? = null,
    groupTrustSummary: ((DuplicateGroup) -> SimilarScreenshotTrustSummary?)? = null,
    keepStrategy: PhotoReviewKeepStrategy = PhotoReviewKeepStrategy.Recommended,
) {
    var selectionState by remember(groups, keepStrategy) {
        mutableStateOf(PhotoReviewSelectionState.fromGroups(groups, keepStrategy))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${selectionState.selectedCount} selected | ${formatBytes(selectionState.selectedBytes)} recoverable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            postDeleteStatus?.let { status ->
                PostDeleteStatusCard(status = status)
            }
            if (noticeTitle != null && noticeMessage != null) {
                ReviewNoticeCard(
                    title = noticeTitle,
                    message = noticeMessage,
                )
            }

            if (groups.isEmpty()) {
                EmptyDuplicateReviewCard(
                    title = emptyTitle,
                    message = emptyMessage,
                    actionLabel = emptyActionLabel,
                    onAction = onEmptyAction,
                )
            } else {
                groups.forEachIndexed { index, group ->
                    DuplicateGroupCard(
                        groupIndex = index + 1,
                        group = group,
                        selectionState = selectionState,
                        onToggle = { itemId ->
                            selectionState = selectionState.toggle(itemId)
                        },
                        onDeselectGroup = {
                            selectionState = selectionState.deselectGroup(group.key)
                        },
                        onResetGroup = {
                            selectionState = selectionState.resetGroup(group.key, keepStrategy)
                        },
                        itemMatchLabel = itemMatchLabel,
                        matchExplanation = groupMatchExplanation?.invoke(group),
                        trustSummary = groupTrustSummary?.invoke(group),
                        keepStrategy = keepStrategy,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        PhotoReviewBottomActionBar(
            selectionState = selectionState,
            onBack = onBack,
            onContinue = { onContinue(selectionState) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PhotoReviewBottomActionBar(
    selectionState: PhotoReviewSelectionState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${selectionState.selectedCount} selected | ${formatBytes(selectionState.selectedBytes)} recoverable",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                ) {
                    Text("Back")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = selectionState.canContinue,
                    onClick = onContinue,
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun ReviewNoticeCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun PostDeleteStatusCard(
    status: PhotoPostDeleteStatus,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "${status.message} | ${formatBytes(status.remainingRecoverableBytes)} remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (status.metrics.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    status.metrics.forEach { metric ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = metric.value,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = metric.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoDeleteConfirmationDialog(
    summary: PhotoDeletionSummary,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text("Delete selected photos?")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeleteConfirmationMetric(
                        value = summary.itemCountLabel,
                        label = "Photos",
                        modifier = Modifier.weight(1f),
                    )
                    DeleteConfirmationMetric(
                        value = formatBytes(summary.bytesToDelete),
                        label = "Recoverable",
                        modifier = Modifier.weight(1f),
                    )
                }
                DeleteConfirmationTrustLine(summary.systemConfirmationLabel)
                DeleteConfirmationTrustLine(summary.cancelSafetyLabel)
                summary.blockedReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "Nothing is removed until you approve Android's system delete request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = summary.canRequestSystemDelete,
                onClick = onConfirmDelete,
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteConfirmationMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeleteConfirmationTrustLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun PhotoDeleteResultDialog(
    result: PhotoDeletionResult,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDone,
        title = {
            Text(result.title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeleteConfirmationMetric(
                        value = result.itemCount.toString(),
                        label = if (result.status == PhotoDeletionStatus.Deleted) "Removed" else "Selected",
                        modifier = Modifier.weight(1f),
                    )
                    DeleteConfirmationMetric(
                        value = formatBytes(result.bytes),
                        label = if (result.status == PhotoDeletionStatus.Deleted) "Freed" else "Protected",
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = when (result.status) {
                        PhotoDeletionStatus.Deleted -> "Your library was refreshed. Continue reviewing any remaining duplicate groups."
                        PhotoDeletionStatus.Canceled -> "No photos were removed. Your previous selection is still available for review."
                        PhotoDeletionStatus.Blocked -> "No photos were removed because Android could not open a system delete request for this selection."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDone) {
                Text(result.primaryActionLabel)
            }
        },
    )
}

@Composable
private fun EmptyDuplicateReviewCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    groupIndex: Int,
    group: DuplicateGroup,
    selectionState: PhotoReviewSelectionState,
    onToggle: (String) -> Unit,
    onDeselectGroup: () -> Unit,
    onResetGroup: () -> Unit,
    modifier: Modifier = Modifier,
    itemMatchLabel: String = "Duplicate",
    matchExplanation: String? = null,
    trustSummary: SimilarScreenshotTrustSummary? = null,
    keepStrategy: PhotoReviewKeepStrategy = PhotoReviewKeepStrategy.Recommended,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val selectedInGroup = selectionState.selectedCountInGroup(group.key)
            val selectedBytesInGroup = selectionState.selectedBytesInGroup(group.key)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Group $groupIndex | ${group.items.size} photos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "$selectedInGroup selected | ${formatBytes(selectedBytesInGroup)} in this group",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            enabled = selectedInGroup > 0,
                            onClick = onDeselectGroup,
                        ) {
                            Text("Clear")
                        }
                        TextButton(
                            enabled = selectedInGroup < group.items.size - 1,
                            onClick = onResetGroup,
                        ) {
                            Text("Suggested")
                        }
                    }
                }
            }
            matchExplanation?.let { explanation ->
                Text(
                    text = "Why matched: $explanation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trustSummary?.let { summary ->
                SimilarScreenshotTrustSummaryBlock(summary = summary)
            }
            PhotoReviewPreviewStrip(
                items = selectionState.previewItemsInGroup(group.key),
                extraCount = (group.items.size - 4).coerceAtLeast(0),
            )
            val keepItem = group.keepItem(keepStrategy)
            group.items.sortedByDescending { it.dateTakenMillis }.forEach { item ->
                PhotoReviewRow(
                    item = item,
                    isRecommendedKeep = item.id == keepItem.id,
                    selectedForDeletion = selectionState.isSelectedForDeletion(item.id),
                    onToggle = { onToggle(item.id) },
                    itemMatchLabel = itemMatchLabel,
                )
            }
        }
    }
}

@Composable
private fun SimilarScreenshotTrustSummaryBlock(
    summary: SimilarScreenshotTrustSummary,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            summary.lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhotoReviewPreviewStrip(
    items: List<MediaItem>,
    extraCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (item.contentUri != null) {
                    AsyncImage(
                        model = item.contentUri,
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = item.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$extraCount more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhotoReviewRow(
    item: MediaItem,
    isRecommendedKeep: Boolean,
    selectedForDeletion: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    itemMatchLabel: String = "Duplicate",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (item.contentUri != null) {
                AsyncImage(
                    model = item.contentUri,
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = item.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (isRecommendedKeep) {
                    "Recommended keep | ${formatBytes(item.sizeBytes)}"
                } else {
                    "$itemMatchLabel | ${formatBytes(item.sizeBytes)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(
            checked = selectedForDeletion,
            onCheckedChange = { onToggle() },
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    if (megabytes < 1024.0) return "${megabytes.roundToInt()} MB"
    return String.format("%.1f GB", megabytes / 1024.0)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PhotoReviewScreenPreview() {
    CleanerTheme {
        PhotoReviewScreen(
            title = "Duplicate photos",
            groups = previewDuplicateGroups,
            onBack = {},
            onContinue = {},
        )
    }
}

val previewDuplicateGroups: List<DuplicateGroup> = listOf(
    DuplicateGroup(
        key = "beach",
        items = listOf(
            previewMediaItem("1", "Beach trip original.jpg", 3_400_000L, 100L),
            previewMediaItem("2", "Beach trip copy.jpg", 3_300_000L, 200L),
            previewMediaItem("3", "Beach trip edited.jpg", 3_100_000L, 300L),
        ),
    ),
    DuplicateGroup(
        key = "receipt",
        items = listOf(
            previewMediaItem("4", "Receipt keep.png", 900_000L, 400L),
            previewMediaItem("5", "Receipt duplicate.png", 880_000L, 500L),
        ),
    ),
)

private fun previewMediaItem(
    id: String,
    displayName: String,
    sizeBytes: Long,
    dateTakenMillis: Long,
): MediaItem {
    return MediaItem(
        id = id,
        displayName = displayName,
        sizeBytes = sizeBytes,
        dateTakenMillis = dateTakenMillis,
        contentHash = null,
        mediaType = MediaType.Image,
    )
}
