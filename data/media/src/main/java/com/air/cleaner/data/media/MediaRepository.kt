package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup

interface MediaRepository {
    suspend fun scanSummary(): MediaScanSummary
    suspend fun scanDuplicatePhotoGroups(): List<DuplicateGroup>
    suspend fun scanSimilarScreenshotGroupResult(): SimilarScreenshotScanResult
    suspend fun scanSimilarScreenshotGroups(): List<DuplicateGroup> = scanSimilarScreenshotGroupResult().groups
    suspend fun cachedSimilarScreenshotGroupResult(): CachedDuplicateGroupsResult
    suspend fun cachedSimilarScreenshotGroups(): List<DuplicateGroup>
    suspend fun saveSimilarScreenshotGroups(groups: List<DuplicateGroup>)
    suspend fun contentUrisStillPresent(contentUris: List<String>): List<String>
}
