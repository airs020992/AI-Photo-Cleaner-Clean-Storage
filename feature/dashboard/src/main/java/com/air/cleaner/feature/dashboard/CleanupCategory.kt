package com.air.cleaner.feature.dashboard

data class CleanupCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val recoverableLabel: String,
    val actionLabel: String,
    val priority: CleanupPriority,
)

enum class CleanupPriority {
    High,
    Medium,
    Low,
}

val previewCleanupCategories = listOf(
    CleanupCategory(
        id = "large_videos",
        title = "Large videos",
        subtitle = "Compress or remove the biggest files first",
        recoverableLabel = "5.2 GB",
        actionLabel = "Open",
        priority = CleanupPriority.High,
    ),
    CleanupCategory(
        id = "similar_photos",
        title = "Similar photos",
        subtitle = "Pick the best shot from repeated moments",
        recoverableLabel = "3.4 GB",
        actionLabel = "Review",
        priority = CleanupPriority.High,
    ),
    CleanupCategory(
        id = "duplicate_photos",
        title = "Duplicate photos",
        subtitle = "Review exact copies before deleting",
        recoverableLabel = "1.8 GB",
        actionLabel = "Review",
        priority = CleanupPriority.Medium,
    ),
    CleanupCategory(
        id = "screenshots",
        title = "Screenshots",
        subtitle = "Clean old captures you no longer need",
        recoverableLabel = "820 MB",
        actionLabel = "Review",
        priority = CleanupPriority.Medium,
    ),
    CleanupCategory(
        id = "blurry_photos",
        title = "Blurry photos",
        subtitle = "Check low-quality images manually",
        recoverableLabel = "640 MB",
        actionLabel = "Review",
        priority = CleanupPriority.Low,
    ),
)
