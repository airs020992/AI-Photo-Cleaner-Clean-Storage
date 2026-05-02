package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaStoreRowMapperTest {
    @Test
    fun mapsValidImageRowToMediaItem() {
        val row = MediaStoreRow(
            id = 42L,
            displayName = "IMG_0420.jpg",
            sizeBytes = 2_500_000L,
            dateTakenMillis = 1_700_000_000_000L,
            dateModifiedSeconds = 1_600_000_000L,
            relativePath = "DCIM/Camera/",
        )

        val item = MediaStoreRowMapper.map(row, MediaType.Image)

        assertEquals("42", item?.id)
        assertEquals("IMG_0420.jpg", item?.displayName)
        assertEquals(2_500_000L, item?.sizeBytes)
        assertEquals(1_700_000_000_000L, item?.dateTakenMillis)
        assertEquals(MediaType.Image, item?.mediaType)
        assertNull(item?.contentHash)
    }

    @Test
    fun fallsBackToDateModifiedWhenDateTakenIsMissing() {
        val row = MediaStoreRow(
            id = 7L,
            displayName = "VID_0007.mp4",
            sizeBytes = 10_000_000L,
            dateTakenMillis = null,
            dateModifiedSeconds = 1_600_000_123L,
            relativePath = "Movies/",
        )

        val item = MediaStoreRowMapper.map(row, MediaType.Video)

        assertEquals(1_600_000_123_000L, item?.dateTakenMillis)
    }

    @Test
    fun ignoresRowsWithoutUsableIdentityOrSize() {
        val missingName = MediaStoreRow(
            id = 1L,
            displayName = "",
            sizeBytes = 100L,
            dateTakenMillis = null,
            dateModifiedSeconds = null,
            relativePath = null,
        )
        val emptyFile = missingName.copy(displayName = "empty.jpg", sizeBytes = 0L)

        assertNull(MediaStoreRowMapper.map(missingName, MediaType.Image))
        assertNull(MediaStoreRowMapper.map(emptyFile, MediaType.Image))
    }
}
