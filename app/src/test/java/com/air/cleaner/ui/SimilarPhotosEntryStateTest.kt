package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarPhotosEntryStateTest {
    @Test
    fun loadingBeforeGroupsShowsScanningMetric() {
        val state = similarPhotosEntryState(
            groups = null,
            formatBytes = { "$it bytes" },
        )

        assertEquals("Scanning", state.metric)
        assertEquals("Checking camera and album photos for near-duplicates", state.subtitle)
    }

    @Test
    fun freshGroupsShowRecoverableSpaceAndReadyCopy() {
        val state = similarPhotosEntryState(
            groups = listOf(group("ready")),
            formatBytes = { "$it bytes" },
        )

        assertEquals("1000 bytes", state.metric)
        assertEquals("Near-identical photos ready for review", state.subtitle)
    }

    @Test
    fun freshEmptyGroupsExplainSafeThreshold() {
        val state = similarPhotosEntryState(
            groups = emptyList(),
            formatBytes = { "$it bytes" },
        )

        assertEquals("0 found", state.metric)
        assertEquals("No safe photo matches yet. Burst or same-scene shots will appear here.", state.subtitle)
    }

    private fun group(key: String): DuplicateGroup {
        return DuplicateGroup(
            key = key,
            items = listOf(
                MediaItem(
                    id = "$key-a",
                    displayName = "$key-a.jpg",
                    sizeBytes = 2_000L,
                    dateTakenMillis = 1_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-a",
                ),
                MediaItem(
                    id = "$key-b",
                    displayName = "$key-b.jpg",
                    sizeBytes = 1_000L,
                    dateTakenMillis = 2_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-b",
                ),
            ),
        )
    }
}
