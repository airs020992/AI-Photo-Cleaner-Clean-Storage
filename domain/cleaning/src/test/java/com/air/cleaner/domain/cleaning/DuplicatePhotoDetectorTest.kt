package com.air.cleaner.domain.cleaning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatePhotoDetectorTest {
    @Test
    fun groupsItemsWithSameContentHash() {
        val detector = DuplicatePhotoDetector()
        val items = listOf(
            mediaItem(id = "1", displayName = "a.jpg", sizeBytes = 100, dateTakenMillis = 1_000, contentHash = "hash-a"),
            mediaItem(id = "2", displayName = "b.jpg", sizeBytes = 120, dateTakenMillis = 1_100, contentHash = "hash-a"),
            mediaItem(id = "3", displayName = "c.jpg", sizeBytes = 130, dateTakenMillis = 1_200, contentHash = "hash-b"),
        )

        val groups = detector.findDuplicates(items)

        assertEquals(1, groups.size)
        assertEquals(listOf("2", "1"), groups.single().items.map { it.id })
    }

    @Test
    fun ignoresItemsWithoutContentHash() {
        val detector = DuplicatePhotoDetector()
        val items = listOf(
            mediaItem(id = "1", contentHash = null),
            mediaItem(id = "2", contentHash = null),
        )

        val groups = detector.findDuplicates(items)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun calculatesRecoverableBytesByKeepingOneItemPerGroup() {
        val detector = DuplicatePhotoDetector()
        val items = listOf(
            mediaItem(id = "small", sizeBytes = 100, dateTakenMillis = 1_000, contentHash = "hash-a"),
            mediaItem(id = "largest", sizeBytes = 300, dateTakenMillis = 1_100, contentHash = "hash-a"),
            mediaItem(id = "middle", sizeBytes = 200, dateTakenMillis = 1_200, contentHash = "hash-a"),
        )

        val group = detector.findDuplicates(items).single()

        assertEquals("largest", group.recommendedKeep.id)
        assertEquals(300, group.recoverableBytes)
    }

    @Test
    fun sortsGroupsByMostRecoverableStorageFirst() {
        val detector = DuplicatePhotoDetector()
        val items = listOf(
            mediaItem(id = "a1", sizeBytes = 100, contentHash = "hash-a"),
            mediaItem(id = "a2", sizeBytes = 100, contentHash = "hash-a"),
            mediaItem(id = "b1", sizeBytes = 900, contentHash = "hash-b"),
            mediaItem(id = "b2", sizeBytes = 800, contentHash = "hash-b"),
        )

        val groups = detector.findDuplicates(items)

        assertEquals("hash-b", groups.first().key)
        assertEquals(800, groups.first().recoverableBytes)
    }

    private fun mediaItem(
        id: String,
        displayName: String = "$id.jpg",
        sizeBytes: Long = 100,
        dateTakenMillis: Long = 1_000,
        contentHash: String?,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = contentHash,
            mediaType = MediaType.Image,
        )
    }
}
