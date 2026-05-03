package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup

class ContentUriPresenceVerifier(
    private val exists: (String) -> Boolean,
) {
    fun stillPresent(contentUris: List<String>): List<String> {
        return contentUris
            .distinct()
            .filter { uri -> exists(uri) }
    }

    fun stillPresentGroups(groups: List<DuplicateGroup>): List<DuplicateGroup> {
        return stillPresentGroupResult(groups).groups
    }

    fun stillPresentGroupResult(groups: List<DuplicateGroup>): CachedDuplicateGroupsResult {
        val filteredGroups = groups.mapNotNull { group ->
            val existingItems = group.items.filter { item ->
                item.contentUri?.let { uri -> exists(uri) } == true
            }
            if (existingItems.size >= 2) {
                group.copy(items = existingItems)
            } else {
                null
            }
        }
        return CachedDuplicateGroupsResult(
            groups = filteredGroups,
            sourceGroupCount = groups.size,
            sourceItemCount = groups.sumOf { group -> group.items.size },
        )
    }
}

data class CachedDuplicateGroupsResult(
    val groups: List<DuplicateGroup>,
    val sourceGroupCount: Int,
    val sourceItemCount: Int,
) {
    val filteredToEmpty: Boolean = sourceItemCount > 0 && groups.isEmpty()
}
