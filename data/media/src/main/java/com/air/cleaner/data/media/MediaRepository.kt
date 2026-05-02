package com.air.cleaner.data.media

interface MediaRepository {
    suspend fun scanSummary(): MediaScanSummary
}
