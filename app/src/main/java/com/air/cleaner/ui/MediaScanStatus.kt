package com.air.cleaner.ui

import com.air.cleaner.data.media.MediaScanSummary

enum class MediaScanPhase {
    CountingLibrary,
    FindingSimilarScreenshots,
    FindingDuplicatePhotos,
    ReconcilingDeletes,
    Complete,
}

data class MediaScanStatus(
    val phase: MediaScanPhase = MediaScanPhase.CountingLibrary,
    val summary: MediaScanSummary? = null,
)

internal fun MediaScanStatus.similarLoadingStepLabel(): String {
    return when (phase) {
        MediaScanPhase.CountingLibrary -> "Step 1 of 3 | Counting media"
        MediaScanPhase.FindingSimilarScreenshots -> {
            val screenshotCount = summary?.screenshotCount
            if (screenshotCount != null) {
                "Step 2 of 3 | Checking $screenshotCount screenshots"
            } else {
                "Step 2 of 3 | Checking screenshots"
            }
        }
        MediaScanPhase.FindingDuplicatePhotos -> "Step 3 of 3 | Finalizing photo scan"
        MediaScanPhase.ReconcilingDeletes -> "Step 3 of 3 | Refreshing cleanup results"
        MediaScanPhase.Complete -> "Scan complete"
    }
}

internal fun MediaScanStatus.similarLoadingMessage(): String {
    return when (phase) {
        MediaScanPhase.CountingLibrary -> "Counting photos and screenshots so we can narrow the scan."
        MediaScanPhase.FindingSimilarScreenshots -> {
            "Comparing screenshots by visual fingerprint. Results appear here automatically."
        }
        MediaScanPhase.FindingDuplicatePhotos -> {
            "Similar screenshot results are nearly ready. We are finishing the broader photo scan."
        }
        MediaScanPhase.ReconcilingDeletes -> "Refreshing the library after your last cleanup."
        MediaScanPhase.Complete -> "Results are ready."
    }
}
