package com.air.cleaner.domain.cleaning

data class MediaItem(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val contentHash: String?,
    val mediaType: MediaType,
)

enum class MediaType {
    Image,
    Video,
}
