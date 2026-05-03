package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import kotlin.math.roundToInt

data class SimilarScreenshotKeepGuidance(
    val keepReasonLine: String,
    val riskLine: String,
    val reviewPriorityLine: String,
)

fun DuplicateGroup.similarScreenshotKeepGuidance(
    keepStrategy: PhotoReviewKeepStrategy,
): SimilarScreenshotKeepGuidance {
    val risk = riskAssessment()
    return SimilarScreenshotKeepGuidance(
        keepReasonLine = when (keepStrategy) {
            PhotoReviewKeepStrategy.Newest -> "Why keep: newest capture in this group"
            PhotoReviewKeepStrategy.Recommended -> "Why keep: highest-quality candidate"
        },
        riskLine = risk.line,
        reviewPriorityLine = risk.priorityLine,
    )
}

private data class SimilarScreenshotRiskAssessment(
    val line: String,
    val priorityLine: String,
)

private fun DuplicateGroup.riskAssessment(): SimilarScreenshotRiskAssessment {
    if (containsSensitiveScreenshotName()) {
        return SimilarScreenshotRiskAssessment(
            line = "Risk: may contain private or transaction details; review before deleting",
            priorityLine = "Review priority: high",
        )
    }
    if (captureRangeSeconds() > 60 * 60) {
        return SimilarScreenshotRiskAssessment(
            line = "Risk: captured in different sessions; review before deleting",
            priorityLine = "Review priority: high",
        )
    }
    if (hasMixedDimensions()) {
        return SimilarScreenshotRiskAssessment(
            line = "Risk: mixed dimensions; compare before deleting",
            priorityLine = "Review priority: medium",
        )
    }
    return SimilarScreenshotRiskAssessment(
        line = "Risk: low; still review before deleting",
        priorityLine = "Review priority: normal",
    )
}

private fun DuplicateGroup.containsSensitiveScreenshotName(): Boolean {
    val sensitiveTerms = listOf(
        "receipt",
        "order",
        "checkout",
        "confirmation",
        "invoice",
        "chat",
        "message",
        "code",
        "otp",
        "2fa",
    )
    return items.any { item ->
        val name = item.displayName.lowercase()
        sensitiveTerms.any { term -> term in name }
    }
}

private fun DuplicateGroup.captureRangeSeconds(): Int {
    val rangeMillis = items.maxOf { it.dateTakenMillis } - items.minOf { it.dateTakenMillis }
    return (rangeMillis / 1_000.0).roundToInt().coerceAtLeast(0)
}

private fun DuplicateGroup.hasMixedDimensions(): Boolean {
    val dimensions = items
        .mapNotNull { item ->
            val width = item.width
            val height = item.height
            if (width != null && height != null) width to height else null
        }
        .toSet()
    return dimensions.size > 1
}
