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
