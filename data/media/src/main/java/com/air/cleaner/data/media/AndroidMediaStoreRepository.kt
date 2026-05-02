package com.air.cleaner.data.media

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.DuplicatePhotoDetector
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMediaStoreRepository(
    private val contentResolver: ContentResolver,
) : MediaRepository {
    override suspend fun scanSummary(): MediaScanSummary = withContext(Dispatchers.IO) {
        val accumulator = MediaScanSummaryAccumulator()
        scanItems(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.Image,
        ).forEach { itemWithPath ->
            accumulator.add(itemWithPath.item, itemWithPath.relativePath)
        }
        scanItems(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.Video,
        ).forEach { itemWithPath ->
            accumulator.add(itemWithPath.item, itemWithPath.relativePath)
        }
        accumulator.summary()
    }

    override suspend fun scanDuplicatePhotoGroups(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        DuplicatePhotoDetector().findDuplicates(
            scanItems(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                mediaType = MediaType.Image,
            ).map { it.item },
        )
    }

    private fun scanItems(
        uri: Uri,
        mediaType: MediaType,
    ): List<MediaItemWithPath> {
        val items = mutableListOf<MediaItemWithPath>()
        contentResolver.query(
            uri,
            projection(),
            "${MediaStore.MediaColumns.SIZE} > 0",
            null,
            "${MediaStore.MediaColumns.SIZE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val row = cursor.toMediaStoreRow()
                val item = MediaStoreRowMapper.map(row, mediaType)
                if (item != null) {
                    items += MediaItemWithPath(item = item, relativePath = row.relativePath)
                }
            }
        }
        return items
    }

    private fun projection(): Array<String> {
        return buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            if (Build.VERSION.SDK_INT >= 29) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()
    }

    private fun Cursor.toMediaStoreRow(): MediaStoreRow {
        return MediaStoreRow(
            id = getRequiredLong(MediaStore.MediaColumns._ID),
            displayName = getOptionalString(MediaStore.MediaColumns.DISPLAY_NAME),
            sizeBytes = getOptionalLong(MediaStore.MediaColumns.SIZE),
            dateTakenMillis = getOptionalLong(MediaStore.MediaColumns.DATE_TAKEN),
            dateModifiedSeconds = getOptionalLong(MediaStore.MediaColumns.DATE_MODIFIED),
            relativePath = if (Build.VERSION.SDK_INT >= 29) {
                getOptionalString(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                null
            },
        )
    }

    private fun Cursor.getRequiredLong(columnName: String): Long {
        return getLong(getColumnIndexOrThrow(columnName))
    }

    private fun Cursor.getOptionalLong(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun Cursor.getOptionalString(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private data class MediaItemWithPath(
        val item: MediaItem,
        val relativePath: String?,
    )
}
