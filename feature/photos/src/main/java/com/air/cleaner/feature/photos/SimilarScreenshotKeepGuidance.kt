package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup

data class SimilarScreenshotKeepGuidance(
    val keepReasonLine: String,
    val riskLine: String,
)

fun DuplicateGroup.similarScreenshotKeepGuidance(
    keepStrategy: PhotoReviewKeepStrategy,
): SimilarScreenshotKeepGuidance {
    return SimilarScreenshotKeepGuidance(
        keepReasonLine = when (keepStrategy) {
            PhotoReviewKeepStrategy.Newest -> "Why keep: newest capture in this group"
            PhotoReviewKeepStrategy.Recommended -> "Why keep: highest-quality candidate"
        },
        riskLine = if (containsSensitiveScreenshotName()) {
            "Risk: may contain private or transaction details; review before deleting"
        } else {
            "Risk: low; still review before deleting"
        },
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
