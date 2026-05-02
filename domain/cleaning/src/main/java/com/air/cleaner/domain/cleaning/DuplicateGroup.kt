package com.air.cleaner.domain.cleaning

data class DuplicateGroup(
    val key: String,
    val items: List<MediaItem>,
) {
    init {
        require(items.size > 1) { "Duplicate groups must contain at least two items." }
    }

    val recommendedKeep: MediaItem = items.maxWith(
        compareBy<MediaItem> { it.sizeBytes }
            .thenByDescending { it.dateTakenMillis },
    )

    val recoverableBytes: Long = items
        .filterNot { it.id == recommendedKeep.id }
        .sumOf { it.sizeBytes }
}
