package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup

enum class SimilarScreenshotReviewFilter(
    val label: String,
) {
    All(label = "All"),
    NeedsReview(label = "Needs review"),
}

data class SimilarScreenshotReviewWorkflow(
    val groups: List<DuplicateGroup>,
    val totalGroups: Int,
    val needsReviewGroups: Int,
    val activeFilterLabel: String,
    val needsReviewGroupKeys: Set<String>,
    val normalGroupKeys: Set<String>,
) {
    val totalGroupsLabel: String = "$totalGroups ${if (totalGroups == 1) "group" else "groups"}"
    val needsReviewGroupsLabel: String =
        "$needsReviewGroups ${if (needsReviewGroups == 1) "needs" else "need"} review"
    val normalGroupsLabel: String =
        "${normalGroupKeys.size} quick-clean ${if (normalGroupKeys.size == 1) "group" else "groups"}"
}

fun List<DuplicateGroup>.toSimilarScreenshotReviewWorkflow(
    keepStrategy: PhotoReviewKeepStrategy,
    filter: SimilarScreenshotReviewFilter,
): SimilarScreenshotReviewWorkflow {
    val rankedGroups = map { group ->
        RankedSimilarScreenshotGroup(
            group = group,
            priority = group.similarScreenshotKeepGuidance(keepStrategy).reviewPriority,
        )
    }.sortedWith(
        compareBy<RankedSimilarScreenshotGroup> { it.priority.sortOrder }
            .thenByDescending { rankedGroup ->
                rankedGroup.group.items.maxOfOrNull { item -> item.dateTakenMillis } ?: 0L
            },
    )
    val needsReviewGroups = rankedGroups.count { it.priority != SimilarScreenshotReviewPriority.Normal }
    val needsReviewGroupKeys = rankedGroups
        .filter { it.priority != SimilarScreenshotReviewPriority.Normal }
        .map { it.group.key }
        .toSet()
    val normalGroupKeys = rankedGroups
        .filter { it.priority == SimilarScreenshotReviewPriority.Normal }
        .map { it.group.key }
        .toSet()
    val visibleGroups = rankedGroups
        .filter { rankedGroup ->
            filter == SimilarScreenshotReviewFilter.All ||
                rankedGroup.priority != SimilarScreenshotReviewPriority.Normal
        }
        .map { it.group }

    return SimilarScreenshotReviewWorkflow(
        groups = visibleGroups,
        totalGroups = size,
        needsReviewGroups = needsReviewGroups,
        activeFilterLabel = filter.label,
        needsReviewGroupKeys = needsReviewGroupKeys,
        normalGroupKeys = normalGroupKeys,
    )
}

private data class RankedSimilarScreenshotGroup(
    val group: DuplicateGroup,
    val priority: SimilarScreenshotReviewPriority,
)
