package com.air.cleaner.data.media

import android.content.Context
import android.content.SharedPreferences
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import java.nio.charset.StandardCharsets
import java.util.Base64

interface SimilarScreenshotResultCache {
    fun load(): List<DuplicateGroup>
    fun save(groups: List<DuplicateGroup>)
}

class InMemorySimilarScreenshotResultCache : SimilarScreenshotResultCache {
    private var payload: String? = null

    override fun load(): List<DuplicateGroup> {
        return SimilarScreenshotResultSerializer.decode(payload.orEmpty())
    }

    override fun save(groups: List<DuplicateGroup>) {
        payload = SimilarScreenshotResultSerializer.encode(groups)
    }

    fun saveRaw(rawPayload: String) {
        payload = rawPayload
    }
}

class SharedPreferencesSimilarScreenshotResultCache(
    context: Context,
) : SimilarScreenshotResultCache {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): List<DuplicateGroup> {
        return SimilarScreenshotResultSerializer.decode(
            preferences.getString(KEY_GROUPS, null).orEmpty(),
        )
    }

    override fun save(groups: List<DuplicateGroup>) {
        preferences.edit()
            .putString(KEY_GROUPS, SimilarScreenshotResultSerializer.encode(groups))
            .apply()
    }

    private companion object {
        private const val PREFERENCES_NAME = "similar_screenshot_result_cache"
        private const val KEY_GROUPS = "groups"
    }
}

private object SimilarScreenshotResultSerializer {
    private const val VERSION = "v1"
    private const val GROUP_SEPARATOR = "\n"
    private const val FIELD_SEPARATOR = "|"

    fun encode(groups: List<DuplicateGroup>): String {
        return buildString {
            append(VERSION)
            groups.forEach { group ->
                append(GROUP_SEPARATOR)
                append(encodeValue(group.key))
                append(FIELD_SEPARATOR)
                append(group.items.size)
                group.items.forEach { item ->
                    append(FIELD_SEPARATOR)
                    append(item.encode())
                }
            }
        }
    }

    fun decode(payload: String): List<DuplicateGroup> {
        return runCatching {
            val lines = payload.lines().filter { it.isNotBlank() }
            if (lines.firstOrNull() != VERSION) return@runCatching emptyList()
            lines.drop(1).mapNotNull { line -> line.decodeGroupOrNull() }
        }.getOrDefault(emptyList())
    }

    private fun String.decodeGroupOrNull(): DuplicateGroup? {
        val fields = split(FIELD_SEPARATOR)
        if (fields.size < 4) return null
        val groupKey = decodeValue(fields[0])
        val itemCount = fields[1].toIntOrNull() ?: return null
        if (itemCount < 2 || fields.size != itemCount + 2) return null
        val items = fields.drop(2).mapNotNull { it.decodeItemOrNull() }
        if (items.size != itemCount) return null
        return runCatching {
            DuplicateGroup(
                key = groupKey,
                items = items,
            )
        }.getOrNull()
    }

    private fun MediaItem.encode(): String {
        return listOf(
            id,
            displayName,
            sizeBytes.toString(),
            dateTakenMillis.toString(),
            contentHash.orEmpty(),
            mediaType.name,
            contentUri.orEmpty(),
            width?.toString().orEmpty(),
            height?.toString().orEmpty(),
        ).joinToString(separator = ",") { value -> encodeValue(value) }
    }

    private fun String.decodeItemOrNull(): MediaItem? {
        val fields = split(",")
        if (fields.size != 9) return null
        val mediaType = runCatching { MediaType.valueOf(decodeValue(fields[5])) }.getOrNull() ?: return null
        return MediaItem(
            id = decodeValue(fields[0]),
            displayName = decodeValue(fields[1]),
            sizeBytes = decodeValue(fields[2]).toLongOrNull() ?: return null,
            dateTakenMillis = decodeValue(fields[3]).toLongOrNull() ?: return null,
            contentHash = decodeValue(fields[4]).takeIf { it.isNotEmpty() },
            mediaType = mediaType,
            contentUri = decodeValue(fields[6]).takeIf { it.isNotEmpty() },
            width = decodeValue(fields[7]).toIntOrNull(),
            height = decodeValue(fields[8]).toIntOrNull(),
        )
    }

    private fun encodeValue(value: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeValue(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }
}
