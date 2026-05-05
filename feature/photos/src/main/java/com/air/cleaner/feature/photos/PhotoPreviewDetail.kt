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
    val reviewPriorityLine: String?,
    val decisionTitle: String,
    val decisionBody: String,
    val decisionActionLabel: String,
    val selectedForDeletion: Boolean,
)

fun MediaItem.toPhotoPreviewDetail(
    isRecommendedKeep: Boolean,
    selectedForDeletion: Boolean,
    itemMatchLabel: String,
    keepReasonLine: String? = null,
    riskLine: String? = null,
    reviewPriorityLine: String? = null,
): PhotoPreviewDetail {
    val sizeLabel = formatPreviewBytes(sizeBytes)
    val roleLabel = if (isRecommendedKeep) "Recommended keep" else itemMatchLabel
    val statusLabel = if (selectedForDeletion) "selected for deletion" else "kept"
    val decisionTitle = when {
        selectedForDeletion -> "Ready to delete"
        isRecommendedKeep -> "Keep this photo"
        else -> "Not selected"
    }
    val decisionBody = when {
        selectedForDeletion -> "This photo is selected for cleanup. Android will ask for final confirmation before anything is removed."
        isRecommendedKeep -> "This is the copy AI Photo Cleaner recommends keeping. It will not be deleted unless you change the selection."
        else -> "This similar photo is currently protected. Select it only if the larger preview confirms it is safe to remove."
    }
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
        reviewPriorityLine = reviewPriorityLine,
        decisionTitle = decisionTitle,
        decisionBody = decisionBody,
        decisionActionLabel = if (selectedForDeletion) "Keep this" else "Select for deletion",
        selectedForDeletion = selectedForDeletion,
    )
}

private fun formatPreviewBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    if (megabytes < 1024.0) return "${megabytes.roundToInt()} MB"
    return String.format("%.1f GB", megabytes / 1024.0)
}
