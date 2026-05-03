package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import kotlin.math.roundToInt

fun DuplicateGroup.similarScreenshotMatchExplanation(): String {
    val captureDistanceLabel = captureDistanceLabel()
    return if (captureDistanceLabel == null) {
        "Same screen size | tiny visual differences"
    } else {
        "Same screen size | captured $captureDistanceLabel apart | tiny visual differences"
    }
}

private fun DuplicateGroup.captureDistanceLabel(): String? {
    val rangeMillis = items.maxOf { it.dateTakenMillis } - items.minOf { it.dateTakenMillis }
    val seconds = (rangeMillis / 1_000.0).roundToInt().coerceAtLeast(0)
    if (seconds > 60 * 60) return null
    if (seconds < 60) return "$seconds sec"
    return "${(seconds / 60.0).roundToInt()} min"
}
