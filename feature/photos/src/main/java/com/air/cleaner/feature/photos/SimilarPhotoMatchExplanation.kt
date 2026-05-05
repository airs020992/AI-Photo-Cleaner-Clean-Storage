package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import kotlin.math.roundToInt

fun DuplicateGroup.similarPhotoMatchExplanation(): String {
    val countLabel = "${items.size} photos"
    val captureDistanceLabel = captureDistanceLabel()
    val sizeClosenessLabel = sizeClosenessLabel()
    return "$countLabel captured within $captureDistanceLabel | $sizeClosenessLabel | keep one best shot"
}

private fun DuplicateGroup.captureDistanceLabel(): String {
    val rangeMillis = items.maxOf { it.dateTakenMillis } - items.minOf { it.dateTakenMillis }
    val seconds = (rangeMillis / 1_000.0).roundToInt().coerceAtLeast(0)
    if (seconds < 60) return "$seconds sec"
    return "${(seconds / 60.0).roundToInt()} min"
}

private fun DuplicateGroup.sizeClosenessLabel(): String {
    val smallest = items.minOf { it.sizeBytes }
    val largest = items.maxOf { it.sizeBytes }
    val deltaPercent = if (largest <= 0L) {
        0
    } else {
        (((largest - smallest).toDouble() / largest.toDouble()) * 100).roundToInt()
    }
    return if (deltaPercent <= 10) {
        "very close file sizes"
    } else {
        "close file sizes"
    }
}
