package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup

internal data class SimilarPhotosEntryState(
    val subtitle: String,
    val metric: String,
)

internal fun similarPhotosEntryState(
    groups: List<DuplicateGroup>?,
    formatBytes: (Long) -> String,
): SimilarPhotosEntryState {
    val metric = when {
        groups == null -> "Scanning"
        groups.isEmpty() -> "0 found"
        else -> formatBytes(groups.sumOf { it.recoverableBytes })
    }
    val subtitle = when {
        groups == null -> "Checking camera and album photos for near-duplicates"
        groups.isNotEmpty() -> "Near-identical photos ready for review"
        else -> "No safe photo matches yet. Burst or same-scene shots will appear here."
    }
    return SimilarPhotosEntryState(subtitle = subtitle, metric = metric)
}
