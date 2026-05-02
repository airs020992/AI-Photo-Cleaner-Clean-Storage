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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
) {
    var selectionState by remember(groups) {
        mutableStateOf(PhotoReviewSelectionState.fromGroups(groups))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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

        if (groups.isEmpty()) {
            EmptyDuplicateReviewCard()
        } else {
            groups.forEachIndexed { index, group ->
                DuplicateGroupCard(
                    groupIndex = index + 1,
                    group = group,
                    selectionState = selectionState,
                    onToggle = { itemId ->
                        selectionState = selectionState.toggle(itemId)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                enabled = selectionState.selectedCount > 0,
                onClick = { onContinue(selectionState) },
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun EmptyDuplicateReviewCard(modifier: Modifier = Modifier) {
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
                text = "No duplicate photos found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "We only show likely matches for review. Nothing is selected or deleted automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    groupIndex: Int,
    group: DuplicateGroup,
    selectionState: PhotoReviewSelectionState,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
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
            Text(
                text = "Group $groupIndex | ${group.items.size} photos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            group.items.sortedBy { it.dateTakenMillis }.forEachIndexed { itemIndex, item ->
                PhotoReviewRow(
                    item = item,
                    isRecommendedKeep = itemIndex == 0,
                    selectedForDeletion = selectionState.isSelectedForDeletion(item.id),
                    onToggle = { onToggle(item.id) },
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
                    "Duplicate | ${formatBytes(item.sizeBytes)}"
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
