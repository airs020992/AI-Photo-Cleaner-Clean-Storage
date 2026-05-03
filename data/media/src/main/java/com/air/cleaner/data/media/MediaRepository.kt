package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup

interface MediaRepository {
    suspend fun scanSummary(): MediaScanSummary
    suspend fun scanDuplicatePhotoGroups(): List<DuplicateGroup>
    suspend fun scanSimilarScreenshotGroups(): List<DuplicateGroup>
    suspend fun contentUrisStillPresent(contentUris: List<String>): List<String>
}
