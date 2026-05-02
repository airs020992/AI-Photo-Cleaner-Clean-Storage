package com.air.cleaner.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.air.cleaner.core.permissions.MediaPermissionState
import com.air.cleaner.core.ui.theme.CleanerTheme
import com.air.cleaner.feature.dashboard.DashboardScreen
import com.air.cleaner.feature.dashboard.previewCleanupCategories
import com.air.cleaner.feature.onboarding.OnboardingScreen

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
            DashboardScreen(
                recoverableSpaceLabel = "11.9 GB",
                scannedItemsLabel = "12,480 items scanned",
                categories = previewCleanupCategories,
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
