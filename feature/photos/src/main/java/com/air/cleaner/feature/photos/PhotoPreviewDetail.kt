package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem
import kotlin.math.roundToInt

data class PhotoPreviewDetail(
    val title: String,
    val fileLine: String,
    val roleLine: String,
    val statusLine: String,
    val metadataLine: String,
    val keepReasonLine: String?,
    val riskLine: String?,
    val selectedForDeletion: Boolean,
)

fun MediaItem.toPhotoPreviewDetail(
    isRecommendedKeep: Boolean,
    selectedForDeletion: Boolean,
    itemMatchLabel: String,
    keepReasonLine: String? = null,
    riskLine: String? = null,
): PhotoPreviewDetail {
    val sizeLabel = formatPreviewBytes(sizeBytes)
    val roleLabel = if (isRecommendedKeep) "Recommended keep" else itemMatchLabel
    val statusLabel = if (selectedForDeletion) "selected for deletion" else "kept"
    val dimensionsLabel = if (width != null && height != null) {
        "$width x $height"
    } else {
        "dimensions unavailable"
    }

    return PhotoPreviewDetail(
        title = "Preview photo",
        fileLine = displayName,
        roleLine = "$roleLabel | $sizeLabel",
        statusLine = "Status: $statusLabel",
        metadataLine = "Size: $sizeLabel | $dimensionsLabel",
        keepReasonLine = keepReasonLine,
        riskLine = riskLine,
        selectedForDeletion = selectedForDeletion,
    )
}

private fun formatPreviewBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    if (megabytes < 1024.0) return "${megabytes.roundToInt()} MB"
    return String.format("%.1f GB", megabytes / 1024.0)
}
