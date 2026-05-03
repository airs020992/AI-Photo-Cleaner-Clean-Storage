package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem

enum class PhotoReviewKeepStrategy {
    Recommended,
    Newest,
}

data class PhotoReviewSelectionState(
    private val groups: List<DuplicateGroup>,
    private val selectedIds: Set<String>,
) {
    val selectedItems: List<MediaItem> = groups
        .flatMap { it.items }
        .filter { it.id in selectedIds }

    val selectedContentUris: List<String> = selectedItems.mapNotNull { it.contentUri }

    val selectedCount: Int = selectedIds.size
    val selectedBytes: Long = selectedItems.sumOf { it.sizeBytes }

    fun isSelectedForDeletion(itemId: String): Boolean {
        return itemId in selectedIds
    }

    fun toggle(itemId: String): PhotoReviewSelectionState {
        val nextSelectedIds = if (itemId in selectedIds) {
            selectedIds - itemId
        } else {
            val group = groups.firstOrNull { group -> group.items.any { it.id == itemId } }
                ?: return this
            val groupIds = group.items.map { it.id }.toSet()
            val selectedInGroup = selectedIds.intersect(groupIds)
            if (selectedInGroup.size >= groupIds.size - 1) {
                selectedIds
            } else {
                selectedIds + itemId
            }
        }
        return copy(selectedIds = nextSelectedIds)
    }

    companion object {
        fun fromGroups(
            groups: List<DuplicateGroup>,
            keepStrategy: PhotoReviewKeepStrategy = PhotoReviewKeepStrategy.Recommended,
        ): PhotoReviewSelectionState {
            val selectedIds = groups
                .flatMap { group ->
                    group.items.filterNot { item -> item.id == group.keepItem(keepStrategy).id }
                }
                .map { it.id }
                .toSet()
            return PhotoReviewSelectionState(groups = groups, selectedIds = selectedIds)
        }
    }
}

fun DuplicateGroup.keepItem(strategy: PhotoReviewKeepStrategy): MediaItem {
    return when (strategy) {
        PhotoReviewKeepStrategy.Recommended -> recommendedKeep
        PhotoReviewKeepStrategy.Newest -> items.maxBy { it.dateTakenMillis }
    }
}
