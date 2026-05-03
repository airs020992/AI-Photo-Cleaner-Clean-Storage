package com.air.cleaner.ui

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.air.cleaner.core.permissions.MediaPermissionState
import com.air.cleaner.core.ui.theme.CleanerTheme
import com.air.cleaner.data.media.AndroidMediaStoreRepository
import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.feature.dashboard.CleanupCategory
import com.air.cleaner.feature.dashboard.CleanupPriority
import com.air.cleaner.feature.dashboard.localizedPreviewCleanupCategories
import com.air.cleaner.feature.onboarding.OnboardingScreen
import com.air.cleaner.feature.photos.PhotoDeleteConfirmationDialog
import com.air.cleaner.feature.photos.PhotoDeleteResultDialog
import com.air.cleaner.feature.photos.PhotoDeletionResult
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import com.air.cleaner.feature.photos.PhotoPostDeleteStatus
import com.air.cleaner.feature.photos.PhotoReviewScreen
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
            var duplicatePhotoGroups by remember { mutableStateOf<List<DuplicateGroup>?>(null) }
            var navigationState by remember { mutableStateOf(AppNavigationState()) }
            var pendingDeleteSummary by remember { mutableStateOf<PhotoDeletionSummary?>(null) }
            var deleteResult by remember { mutableStateOf<PhotoDeletionResult?>(null) }
            var lastDeletedResult by remember { mutableStateOf<PhotoDeletionResult?>(null) }
            var scanRefreshKey by remember { mutableStateOf(0) }
            val deleteLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                pendingDeleteSummary?.let { summary ->
                    val deletionResult = PhotoDeletionResult.fromSystemResult(
                        summary = summary,
                        systemConfirmed = result.resultCode == Activity.RESULT_OK,
                    )
                    deleteResult = deletionResult
                    if (deletionResult.shouldRefreshScan) {
                        lastDeletedResult = deletionResult
                        scanRefreshKey += 1
                    }
                }
                pendingDeleteSummary = null
            }

            LaunchedEffect(context, permissionState.access, scanRefreshKey) {
                val scanResult = withContext(Dispatchers.IO) {
                    val repository = AndroidMediaStoreRepository(context.contentResolver)
                    repository.scanSummary() to repository.scanDuplicatePhotoGroups()
                }
                scanSummary = scanResult.first
                duplicatePhotoGroups = scanResult.second
            }

            MainAppShell(
                navigationState = navigationState,
                scanSummary = scanSummary,
                duplicatePhotoGroups = duplicatePhotoGroups,
                onTabSelected = { navigationState = navigationState.selectTab(it) },
                onOpenDuplicatePhotos = { navigationState = navigationState.openDuplicatePhotos() },
                onBackToPhotos = { navigationState = navigationState.selectTab(AppTab.Photos) },
                pendingDeleteSummary = pendingDeleteSummary,
                deleteResult = deleteResult,
                postDeleteStatus = PhotoPostDeleteStatus.from(
                    result = lastDeletedResult,
                    currentGroups = duplicatePhotoGroups.orEmpty(),
                ),
                onRequestDeleteConfirmation = { pendingDeleteSummary = it },
                onDismissDeleteConfirmation = { pendingDeleteSummary = null },
                onConfirmDelete = { summary ->
                    val sender = context.createSystemDeleteRequest(summary.contentUris)
                    if (sender == null) {
                        deleteResult = PhotoDeletionResult.blocked(summary)
                        pendingDeleteSummary = null
                    } else {
                        deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                },
                onDismissDeleteResult = { deleteResult = null },
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
private fun MainAppShell(
    navigationState: AppNavigationState,
    scanSummary: MediaScanSummary?,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    onTabSelected: (AppTab) -> Unit,
    onOpenDuplicatePhotos: () -> Unit,
    onBackToPhotos: () -> Unit,
    pendingDeleteSummary: PhotoDeletionSummary?,
    deleteResult: PhotoDeletionResult?,
    postDeleteStatus: PhotoPostDeleteStatus?,
    onRequestDeleteConfirmation: (PhotoDeletionSummary) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDelete: (PhotoDeletionSummary) -> Unit,
    onDismissDeleteResult: () -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFFF6F8FA),
        bottomBar = {
            if (navigationState.shouldShowBottomTabs) {
                CleanerNavigationBar(
                    selectedTab = navigationState.selectedTab,
                    onTabSelected = onTabSelected,
                )
            }
        },
    ) { padding ->
        when (val screen = navigationState.currentScreen) {
            is AppScreen.Tab -> TabScreen(
                tab = screen.tab,
                scanSummary = scanSummary,
                duplicatePhotoGroups = duplicatePhotoGroups,
                onOpenDuplicatePhotos = onOpenDuplicatePhotos,
                contentPadding = padding,
            )
            AppScreen.DuplicatePhotoReview -> Box(modifier = Modifier.padding(padding)) {
                PhotoReviewScreen(
                    title = "Duplicate photos",
                    groups = duplicatePhotoGroups.orEmpty(),
                    onBack = onBackToPhotos,
                    onContinue = { selectionState ->
                        onRequestDeleteConfirmation(
                            PhotoDeletionSummary.fromItems(selectionState.selectedItems),
                        )
                    },
                    postDeleteStatus = postDeleteStatus,
                )
            }
        }
        pendingDeleteSummary?.let { summary ->
            PhotoDeleteConfirmationDialog(
                summary = summary,
                onDismiss = onDismissDeleteConfirmation,
                onConfirmDelete = { onConfirmDelete(summary) },
            )
        }
        deleteResult?.let { result ->
            PhotoDeleteResultDialog(
                result = result,
                onDone = onDismissDeleteResult,
            )
        }
    }
}

@Composable
private fun TabScreen(
    tab: AppTab,
    scanSummary: MediaScanSummary?,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    onOpenDuplicatePhotos: () -> Unit,
    contentPadding: PaddingValues,
) {
    when (tab) {
        AppTab.Clean -> CleanTabScreen(
            scanSummary = scanSummary,
            onOpenDuplicatePhotos = onOpenDuplicatePhotos,
            contentPadding = contentPadding,
        )
        AppTab.Photos -> PhotosTabScreen(
            scanSummary = scanSummary,
            duplicatePhotoGroups = duplicatePhotoGroups,
            onOpenDuplicatePhotos = onOpenDuplicatePhotos,
            contentPadding = contentPadding,
        )
        AppTab.Videos -> VideosTabScreen(
            videoBytesLabel = scanSummary?.videoBytes.toStorageLabel(),
            contentPadding = contentPadding,
        )
        AppTab.Settings -> SettingsTabScreen(contentPadding = contentPadding)
    }
}

@Composable
private fun CleanTabScreen(
    scanSummary: MediaScanSummary?,
    onOpenDuplicatePhotos: () -> Unit,
    contentPadding: PaddingValues,
) {
    val categories = scanSummary?.toCleanupCategories() ?: localizedPreviewCleanupCategories()
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "AI Cleaner", action = "Premium")
        StorageHero(
            recoverableSpaceLabel = scanSummary?.totalBytes.toStorageLabel(),
            scannedItemsLabel = scanSummary?.totalCount.toScannedItemsLabel(),
        )
        SectionTitle("Biggest wins")
        categories.take(3).forEach { category ->
            MetricRow(
                icon = category.icon(),
                title = category.title,
                subtitle = category.subtitle,
                metric = category.recoverableLabel,
                onClick = if (category.id == "duplicate_photos") onOpenDuplicatePhotos else null,
            )
        }
        TrustStrip()
    }
}

@Composable
private fun PhotosTabScreen(
    scanSummary: MediaScanSummary?,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    onOpenDuplicatePhotos: () -> Unit,
    contentPadding: PaddingValues,
) {
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "Photos", action = "Sort")
        FilterChips(labels = listOf("Duplicates", "Similar", "Blurry", "Screenshots"))
        MetricRow(
            icon = Icons.Rounded.Image,
            title = "Duplicate photos",
            subtitle = "Review likely matches and keep the best original",
            metric = duplicatePhotoGroups.toDuplicateMetricLabel(),
            onClick = onOpenDuplicatePhotos,
        )
        MetricRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "Similar photos",
            subtitle = "Repeated moments, bursts, and near-identical shots",
            metric = scanSummary?.imageBytes.toStorageLabel(),
        )
        MetricRow(
            icon = Icons.Rounded.Shield,
            title = "Screenshots",
            subtitle = "Old captures grouped for quick review",
            metric = scanSummary?.screenshotBytes.toStorageLabel(),
        )
        BottomSummaryBar(text = "Safe selection protects at least one photo per group")
    }
}

@Composable
private fun VideosTabScreen(
    videoBytesLabel: String,
    contentPadding: PaddingValues,
) {
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "Videos", action = "Filter")
        PremiumSurface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Large videos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(videoBytesLabel, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    "Compress preview creates a copy first. Originals stay untouched until you confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        MetricRow(Icons.Rounded.PlayCircle, "Travel clip", "03:42 | 1080p", "920 MB")
        MetricRow(Icons.Rounded.PlayCircle, "Screen recording", "01:18 | 1440p", "460 MB")
        BottomSummaryBar(text = "Compression estimate is shown before any destructive action")
    }
}

@Composable
private fun SettingsTabScreen(contentPadding: PaddingValues) {
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "Settings", action = "v0.1")
        PremiumSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Trust-first cleanup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("No fake booster claims. No deletion without confirmation.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        listOf("Language", "Subscription", "Privacy", "Restore purchases", "Scan rules").forEach {
            MetricRow(
                icon = Icons.Rounded.Settings,
                title = it,
                subtitle = "Manage $it",
                metric = "Open",
            )
        }
    }
}

@Composable
private fun ScreenColumn(
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun TopHeader(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AssistChip(onClick = {}, label = { Text(action) })
    }
}

@Composable
private fun StorageHero(
    recoverableSpaceLabel: String,
    scannedItemsLabel: String,
) {
    PremiumSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Free up", style = MaterialTheme.typography.titleMedium)
                Text(
                    recoverableSpaceLabel,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(scannedItemsLabel, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Safe preview before deleting",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StorageRing(progress = 0.68f)
        }
    }
}

@Composable
private fun StorageRing(progress: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = Color(0xFF2F80ED)
    val track = Color(0xFFE2E8F0)
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx())
            val topLeft = Offset(6.dp.toPx(), 6.dp.toPx())
            drawArc(track, -90f, 360f, false, topLeft, size, style = stroke)
            drawArc(primary, -90f, 360f * progress, false, topLeft, size, style = stroke)
            drawArc(secondary, -90f + 360f * progress + 8f, 42f, false, topLeft, size, style = stroke)
        }
        Text("${(progress * 100).roundToInt()}%", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumSurface(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    metric: String,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.White,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF5F3)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF0F766E))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(metric, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun FilterChips(labels: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            AssistChip(
                onClick = {},
                label = { Text(label) },
                enabled = index != 0,
            )
        }
    }
}

@Composable
private fun TrustStrip() {
    BottomSummaryBar(text = "Review everything first. Premium and ads appear only after useful scan results.")
}

@Composable
private fun BottomSummaryBar(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEAF5F3),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF115E59),
        )
    }
}

@Composable
private fun CleanerNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
) {
    NavigationBar(containerColor = Color.White) {
        AppTab.primaryTabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon(), contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

private fun AppTab.icon(): ImageVector {
    return when (this) {
        AppTab.Clean -> Icons.Rounded.CleaningServices
        AppTab.Photos -> Icons.Rounded.Image
        AppTab.Videos -> Icons.Rounded.PlayCircle
        AppTab.Settings -> Icons.Rounded.Settings
    }
}

private fun CleanupCategory.icon(): ImageVector {
    return when (id) {
        "large_videos" -> Icons.Rounded.PlayCircle
        "similar_photos", "duplicate_photos", "screenshots", "blurry_photos" -> Icons.Rounded.Image
        else -> Icons.Rounded.CleaningServices
    }
}

@Composable
private fun List<DuplicateGroup>?.toDuplicateMetricLabel(): String {
    return when {
        this == null -> stringResource(com.air.cleaner.feature.dashboard.R.string.dashboard_summary_scanning)
        isEmpty() -> "0 found"
        else -> formatBytes(sumOf { it.recoverableBytes })
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
            id = "similar_photos",
            title = stringResource(com.air.cleaner.feature.dashboard.R.string.category_similar_photos_title),
            subtitle = stringResource(com.air.cleaner.feature.dashboard.R.string.category_similar_photos_subtitle),
            recoverableLabel = formatBytes(imageBytes),
            actionLabel = stringResource(com.air.cleaner.feature.dashboard.R.string.action_review),
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

private fun android.content.Context.createSystemDeleteRequest(contentUris: List<String>): IntentSender? {
    if (Build.VERSION.SDK_INT < 30 || contentUris.isEmpty()) {
        return null
    }
    return android.provider.MediaStore.createDeleteRequest(
        contentResolver,
        contentUris.map { Uri.parse(it) },
    ).intentSender
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainAppShellPreview() {
    CleanerTheme {
        MainAppShell(
            navigationState = AppNavigationState(),
            scanSummary = MediaScanSummary(
                imageCount = 8_240,
                videoCount = 420,
                imageBytes = 3_400_000_000L,
                videoBytes = 5_200_000_000L,
                screenshotCount = 820,
                screenshotBytes = 820_000_000L,
            ),
            duplicatePhotoGroups = emptyList(),
            onTabSelected = {},
            onOpenDuplicatePhotos = {},
            onBackToPhotos = {},
            pendingDeleteSummary = null,
            deleteResult = null,
            postDeleteStatus = null,
            onRequestDeleteConfirmation = {},
            onDismissDeleteConfirmation = {},
            onConfirmDelete = {},
            onDismissDeleteResult = {},
        )
    }
}
