package com.air.cleaner.data.media

data class SimilarPhotoScanScope(
    val relativePathPrefix: String? = null,
) {
    fun accepts(relativePath: String?): Boolean {
        val prefix = relativePathPrefix?.trim().orEmpty()
        if (prefix.isEmpty()) return true
        return relativePath?.startsWith(prefix, ignoreCase = true) == true
    }
}
