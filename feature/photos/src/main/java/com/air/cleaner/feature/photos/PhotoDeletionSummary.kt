package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem

data class PhotoDeletionSummary(
    val itemCount: Int,
    val bytesToDelete: Long,
    val contentUris: List<String>,
    val highPriorityGroupCount: Int = 0,
    val mediumPriorityGroupCount: Int = 0,
) {
    val canRequestSystemDelete: Boolean = itemCount > 0 && contentUris.size == itemCount
    val missingDeleteAccessCount: Int = itemCount - contentUris.size
    val itemCountLabel: String = "$itemCount selected"
    val systemConfirmationLabel: String = "Android confirmation required"
    val cancelSafetyLabel: String = "Cancel keeps your current selection"
    val priorityGroupCount: Int = highPriorityGroupCount + mediumPriorityGroupCount
    val priorityGroupCountLabel: String? = if (priorityGroupCount > 0) {
        "$priorityGroupCount priority ${if (priorityGroupCount == 1) "group" else "groups"} selected"
    } else {
        null
    }
    val priorityWarningLine: String? = if (priorityGroupCount > 0) {
        "Review again: ${priorityCountParts().joinToString(" and ")} ${if (priorityGroupCount == 1) "is" else "are"} selected."
    } else {
        null
    }
    val blockedReason: String? = if (missingDeleteAccessCount > 0) {
        "$missingDeleteAccessCount selected photo${if (missingDeleteAccessCount == 1) "" else "s"} is missing Android delete access."
    } else {
        null
    }

    companion object {
        fun fromItems(items: List<MediaItem>): PhotoDeletionSummary {
            return PhotoDeletionSummary(
                itemCount = items.size,
                bytesToDelete = items.sumOf { it.sizeBytes },
                contentUris = items.mapNotNull { it.contentUri },
            )
        }

        fun fromSimilarScreenshotSelection(
            groups: List<DuplicateGroup>,
            selectionState: PhotoReviewSelectionState,
            keepStrategy: PhotoReviewKeepStrategy,
        ): PhotoDeletionSummary {
            val selectedIds = selectionState.selectedItems.map { it.id }.toSet()
            val selectedGroups = groups.filter { group ->
                group.items.any { item -> item.id in selectedIds }
            }
            val highPriorityGroupCount = selectedGroups.count { group ->
                group.similarScreenshotKeepGuidance(keepStrategy).reviewPriority == SimilarScreenshotReviewPriority.High
            }
            val mediumPriorityGroupCount = selectedGroups.count { group ->
                group.similarScreenshotKeepGuidance(keepStrategy).reviewPriority == SimilarScreenshotReviewPriority.Medium
            }
            return fromItems(selectionState.selectedItems).copy(
                highPriorityGroupCount = highPriorityGroupCount,
                mediumPriorityGroupCount = mediumPriorityGroupCount,
            )
        }
    }
}

private fun PhotoDeletionSummary.priorityCountParts(): List<String> {
    return listOfNotNull(
        priorityCountPart(highPriorityGroupCount, "high risk"),
        priorityCountPart(mediumPriorityGroupCount, "medium risk"),
    )
}

private fun priorityCountPart(count: Int, label: String): String? {
    if (count == 0) return null
    return "$count $label ${if (count == 1) "group" else "groups"}"
}
