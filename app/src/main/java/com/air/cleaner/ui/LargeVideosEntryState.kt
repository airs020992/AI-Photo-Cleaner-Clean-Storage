package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.MediaItem

internal data class LargeVideosEntryState(
    val subtitle: String,
    val totalBytesLabel: String,
    val rows: List<LargeVideoRowState>,
    val actionEnabled: Boolean,
    val statusLabel: String,
)

internal data class LargeVideoRowState(
    val title: String,
    val subtitle: String,
    val metric: String,
)

internal fun largeVideosEntryState(
    videos: List<MediaItem>,
    formatBytes: (Long) -> String,
): LargeVideosEntryState {
    return largeVideosEntryState(
        videos = videos,
        fallbackTotalBytesLabel = formatBytes(videos.sumOf { video -> video.sizeBytes }),
        formatBytes = formatBytes,
    )
}

internal fun largeVideosEntryState(
    videos: List<MediaItem>?,
    fallbackTotalBytesLabel: String,
    formatBytes: (Long) -> String,
): LargeVideosEntryState {
    if (videos == null) {
        return LargeVideosEntryState(
            subtitle = "Scanning videos by size",
            totalBytesLabel = fallbackTotalBytesLabel,
            rows = emptyList(),
            actionEnabled = false,
            statusLabel = "Originals stay untouched until you confirm.",
        )
    }
    val rows = videos.map { video ->
        LargeVideoRowState(
            title = video.displayName,
            subtitle = "${video.durationLabel()} | ${formatBytes(video.sizeBytes)}",
            metric = formatBytes(video.sizeBytes),
        )
    }
    return LargeVideosEntryState(
        subtitle = videos.largeVideoEntrySubtitle(),
        totalBytesLabel = formatBytes(videos.sumOf { video -> video.sizeBytes }),
        rows = rows,
        actionEnabled = videos.isNotEmpty(),
        statusLabel = if (videos.isEmpty()) {
            "No action needed. New videos will appear after the next scan."
        } else {
            "Compression estimate first. Android asks again before deletion."
        },
    )
}

private fun List<MediaItem>.largeVideoEntrySubtitle(): String {
    return when {
        isEmpty() -> "No large videos found"
        size >= 20 -> "Top 20 largest videos ready to review"
        else -> "$size videos ready to review"
    }
}
