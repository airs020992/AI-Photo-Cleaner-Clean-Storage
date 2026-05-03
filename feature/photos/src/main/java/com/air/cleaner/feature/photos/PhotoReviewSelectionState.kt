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
    val canContinue: Boolean = selectedCount > 0

    fun isSelectedForDeletion(itemId: String): Boolean {
        return itemId in selectedIds
    }

    fun selectedCountInGroup(groupKey: String): Int {
        return selectedItemsInGroup(groupKey).size
    }

    fun selectedBytesInGroup(groupKey: String): Long {
        return selectedItemsInGroup(groupKey).sumOf { it.sizeBytes }
    }

    fun previewItemsInGroup(groupKey: String, maxItems: Int = 4): List<MediaItem> {
        val group = groups.firstOrNull { it.key == groupKey } ?: return emptyList()
        return group.items
            .sortedByDescending { it.dateTakenMillis }
            .take(maxItems.coerceAtLeast(0))
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

    fun deselectGroup(groupKey: String): PhotoReviewSelectionState {
        val group = groups.firstOrNull { it.key == groupKey } ?: return this
        val groupIds = group.items.map { it.id }.toSet()
        return copy(selectedIds = selectedIds - groupIds)
    }

    fun resetGroup(
        groupKey: String,
        keepStrategy: PhotoReviewKeepStrategy = PhotoReviewKeepStrategy.Recommended,
    ): PhotoReviewSelectionState {
        val group = groups.firstOrNull { it.key == groupKey } ?: return this
        val keepId = group.keepItem(keepStrategy).id
        val groupIds = group.items.map { it.id }.toSet()
        val suggestedIds = groupIds - keepId
        return copy(selectedIds = (selectedIds - groupIds) + suggestedIds)
    }

    companion object {
        fun fromGroups(
            groups: List<DuplicateGroup>,
            keepStrategy: PhotoReviewKeepStrategy = PhotoReviewKeepStrategy.Recommended,
            protectedGroupKeys: Set<String> = emptySet(),
        ): PhotoReviewSelectionState {
            val selectedIds = groups
                .filterNot { group -> group.key in protectedGroupKeys }
                .flatMap { group ->
                    group.items.filterNot { item -> item.id == group.keepItem(keepStrategy).id }
                }
                .map { it.id }
                .toSet()
            return PhotoReviewSelectionState(groups = groups, selectedIds = selectedIds)
        }
    }

    private fun selectedItemsInGroup(groupKey: String): List<MediaItem> {
        val group = groups.firstOrNull { it.key == groupKey } ?: return emptyList()
        return group.items.filter { it.id in selectedIds }
    }
}

fun DuplicateGroup.keepItem(strategy: PhotoReviewKeepStrategy): MediaItem {
    return when (strategy) {
        PhotoReviewKeepStrategy.Recommended -> recommendedKeep
        PhotoReviewKeepStrategy.Newest -> items.maxBy { it.dateTakenMillis }
    }
}
