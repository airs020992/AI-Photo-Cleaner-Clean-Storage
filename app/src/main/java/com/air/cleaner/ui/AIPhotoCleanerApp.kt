package com.air.cleaner.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.air.cleaner.core.permissions.MediaPermissionState
import com.air.cleaner.core.ui.theme.CleanerTheme
import com.air.cleaner.data.media.AndroidMediaStoreRepository
import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.feature.dashboard.DashboardScreen
import com.air.cleaner.feature.dashboard.CleanupCategory
import com.air.cleaner.feature.dashboard.CleanupPriority
import com.air.cleaner.feature.dashboard.localizedPreviewCleanupCategories
import com.air.cleaner.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.roundToInt

@Composable
fun AIPhotoCleanerApp() {
    CleanerTheme {
        val context = LocalContext.current
        var permissionState by remember {
            mutableStateOf(context.currentMediaPermissionState())
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            permissionState = context.currentMediaPermissionState()
        }

        if (permissionState.canScanAnyMedia) {
            var scanSummary by remember { mutableStateOf<MediaScanSummary?>(null) }

            LaunchedEffect(context, permissionState.access) {
                scanSummary = withContext(Dispatchers.IO) {
                    AndroidMediaStoreRepository(context.contentResolver).scanSummary()
                }
            }

            DashboardScreen(
                recoverableSpaceLabel = scanSummary?.totalBytes.toStorageLabel(),
                scannedItemsLabel = scanSummary?.totalCount.toScannedItemsLabel(),
                categories = scanSummary?.toCleanupCategories() ?: localizedPreviewCleanupCategories(),
                onCategoryClick = {},
            )
        } else {
            OnboardingScreen(
                mediaAccess = permissionState.access,
                onRequestPermission = {
                    permissionLauncher.launch(mediaPermissionsForCurrentDevice())
                },
            )
        }
    }
}

@Composable
private fun Int?.toScannedItemsLabel(): String {
    return if (this == null) {
        stringResource(com.air.cleaner.feature.dashboard.R.string.dashboard_summary_scanning)
    } else {
        stringResource(
            com.air.cleaner.feature.dashboard.R.string.dashboard_scanned_items_dynamic,
            NumberFormat.getIntegerInstance().format(this),
        )
    }
}

@Composable
private fun Long?.toStorageLabel(): String {
    if (this == null) {
        return stringResource(com.air.cleaner.feature.dashboard.R.string.scan_size_zero)
    }
    return formatBytes(this)
}

@Composable
private fun MediaScanSummary.toCleanupCategories(): List<CleanupCategory> {
    return listOf(
        CleanupCategory(
            id = "large_videos",
            title = stringResource(com.air.cleaner.feature.dashboard.R.string.category_large_videos_title),
            subtitle = stringResource(com.air.cleaner.feature.dashboard.R.string.category_large_videos_subtitle),
            recoverableLabel = formatBytes(videoBytes),
            actionLabel = stringResource(com.air.cleaner.feature.dashboard.R.string.action_open),
            priority = CleanupPriority.High,
        ),
        CleanupCategory(
            id = "screenshots",
            title = stringResource(com.air.cleaner.feature.dashboard.R.string.category_screenshots_title),
            subtitle = stringResource(com.air.cleaner.feature.dashboard.R.string.category_screenshots_subtitle),
            recoverableLabel = formatBytes(screenshotBytes),
            actionLabel = stringResource(com.air.cleaner.feature.dashboard.R.string.action_review),
            priority = CleanupPriority.Medium,
        ),
        CleanupCategory(
            id = "similar_photos",
            title = stringResource(com.air.cleaner.feature.dashboard.R.string.category_similar_photos_title),
            subtitle = stringResource(com.air.cleaner.feature.dashboard.R.string.category_similar_photos_subtitle),
            recoverableLabel = formatBytes(imageBytes),
            actionLabel = stringResource(com.air.cleaner.feature.dashboard.R.string.action_review),
            priority = CleanupPriority.High,
        ),
        CleanupCategory(
            id = "duplicate_photos",
            title = stringResource(com.air.cleaner.feature.dashboard.R.string.category_duplicate_photos_title),
            subtitle = stringResource(com.air.cleaner.feature.dashboard.R.string.category_duplicate_photos_subtitle),
            recoverableLabel = formatBytes(0L),
            actionLabel = stringResource(com.air.cleaner.feature.dashboard.R.string.action_review),
            priority = CleanupPriority.Medium,
        ),
        CleanupCategory(
            id = "blurry_photos",
            title = stringResource(com.air.cleaner.feature.dashboard.R.string.category_blurry_photos_title),
            subtitle = stringResource(com.air.cleaner.feature.dashboard.R.string.category_blurry_photos_subtitle),
            recoverableLabel = formatBytes(0L),
            actionLabel = stringResource(com.air.cleaner.feature.dashboard.R.string.action_review),
            priority = CleanupPriority.Low,
        ),
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    if (megabytes < 1024.0) {
        return "${megabytes.roundToInt()} MB"
    }
    val gigabytes = megabytes / 1024.0
    return String.format("%.1f GB", gigabytes)
}

private fun android.content.Context.currentMediaPermissionState(): MediaPermissionState {
    return MediaPermissionState(
        sdkInt = Build.VERSION.SDK_INT,
        readImagesGranted = hasPermission(Manifest.permission.READ_MEDIA_IMAGES),
        readVideoGranted = hasPermission(Manifest.permission.READ_MEDIA_VIDEO),
        visualUserSelectedGranted = if (Build.VERSION.SDK_INT >= 34) {
            hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else {
            false
        },
        legacyReadGranted = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
    )
}

private fun android.content.Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
}

private fun mediaPermissionsForCurrentDevice(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
