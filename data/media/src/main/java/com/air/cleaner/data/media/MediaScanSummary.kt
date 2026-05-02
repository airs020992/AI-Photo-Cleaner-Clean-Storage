package com.air.cleaner.data.media

data class MediaScanSummary(
    val imageCount: Int,
    val videoCount: Int,
    val imageBytes: Long,
    val videoBytes: Long,
    val screenshotCount: Int,
    val screenshotBytes: Long,
) {
    val totalCount: Int = imageCount + videoCount
    val totalBytes: Long = imageBytes + videoBytes

    companion object {
        val Empty = MediaScanSummary(
            imageCount = 0,
            videoCount = 0,
            imageBytes = 0L,
            videoBytes = 0L,
            screenshotCount = 0,
            screenshotBytes = 0L,
        )
    }
}
