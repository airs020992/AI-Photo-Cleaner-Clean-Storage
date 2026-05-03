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

internal fun MediaScanStatus.similarLoadingProgress(): Float {
    return when (phase) {
        MediaScanPhase.CountingLibrary -> 0.18f
        MediaScanPhase.FindingSimilarScreenshots -> 0.64f
        MediaScanPhase.FindingDuplicatePhotos -> 0.88f
        MediaScanPhase.ReconcilingDeletes -> 0.92f
        MediaScanPhase.Complete -> 1f
    }
}

internal fun MediaScanStatus.similarLoadingScopeLabel(): String {
    val screenshotCount = summary?.screenshotCount
    return if (screenshotCount == null) {
        "Scan scope appears after media count finishes"
    } else {
        "$screenshotCount screenshots in scope"
    }
}

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

internal fun MediaScanStatus.similarLoadingExpectationLabel(): String {
    val screenshotCount = summary?.screenshotCount
    return when {
        phase != MediaScanPhase.FindingSimilarScreenshots ->
            "Scan progress updates automatically."
        screenshotCount == null ->
            "Counting scan scope before visual comparison starts."
        screenshotCount >= 100 ->
            "Large screenshot library. This can take a few seconds; keep this screen open and groups will appear automatically."
        else ->
            "This usually finishes quickly; groups will appear automatically."
    }
}
