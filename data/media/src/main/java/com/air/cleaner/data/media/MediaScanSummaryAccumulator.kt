package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType

class MediaScanSummaryAccumulator {
    private var imageCount = 0
    private var videoCount = 0
    private var imageBytes = 0L
    private var videoBytes = 0L
    private var screenshotCount = 0
    private var screenshotBytes = 0L

    fun add(item: MediaItem, relativePath: String?) {
        when (item.mediaType) {
            MediaType.Image -> {
                imageCount += 1
                imageBytes += item.sizeBytes
                if (item.isScreenshot(relativePath)) {
                    screenshotCount += 1
                    screenshotBytes += item.sizeBytes
                }
            }
            MediaType.Video -> {
                videoCount += 1
                videoBytes += item.sizeBytes
            }
        }
    }

    fun summary(): MediaScanSummary {
        return MediaScanSummary(
            imageCount = imageCount,
            videoCount = videoCount,
            imageBytes = imageBytes,
            videoBytes = videoBytes,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
        )
    }

    private fun MediaItem.isScreenshot(relativePath: String?): Boolean {
        val path = relativePath.orEmpty()
        return displayName.contains("screenshot", ignoreCase = true) ||
            path.contains("screenshot", ignoreCase = true)
    }
}
