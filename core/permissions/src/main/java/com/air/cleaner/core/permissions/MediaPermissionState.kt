package com.air.cleaner.core.permissions

data class MediaPermissionState(
    val sdkInt: Int,
    val readImagesGranted: Boolean,
    val readVideoGranted: Boolean,
    val visualUserSelectedGranted: Boolean,
    val legacyReadGranted: Boolean,
) {
    val access: MediaPermissionAccess
        get() = when {
            sdkInt >= 33 && readImagesGranted && readVideoGranted -> MediaPermissionAccess.Full
            sdkInt >= 34 && visualUserSelectedGranted -> MediaPermissionAccess.SelectedOnly
            sdkInt <= 32 && legacyReadGranted -> MediaPermissionAccess.Full
            else -> MediaPermissionAccess.None
        }

    val canScanAnyMedia: Boolean
        get() = access != MediaPermissionAccess.None

    val canScanFullLibrary: Boolean
        get() = access == MediaPermissionAccess.Full
}
