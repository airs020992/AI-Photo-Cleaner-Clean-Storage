package com.air.cleaner.data.media

class ContentUriPresenceVerifier(
    private val exists: (String) -> Boolean,
) {
    fun stillPresent(contentUris: List<String>): List<String> {
        return contentUris
            .distinct()
            .filter { uri -> exists(uri) }
    }
}
