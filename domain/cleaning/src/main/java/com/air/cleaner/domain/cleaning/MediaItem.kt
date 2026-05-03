package com.air.cleaner.domain.cleaning

data class MediaItem(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val contentHash: String?,
    val mediaType: MediaType,
    val contentUri: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

enum class MediaType {
    Image,
    Video,
}
