package com.air.cleaner.data.media

data class MediaStoreRow(
    val id: Long,
    val displayName: String?,
    val sizeBytes: Long?,
    val dateTakenMillis: Long?,
    val dateModifiedSeconds: Long?,
    val relativePath: String?,
    val width: Int?,
    val height: Int?,
    val durationMillis: Long? = null,
)
