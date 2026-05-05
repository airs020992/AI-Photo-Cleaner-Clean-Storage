package com.air.cleaner.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.air.cleaner.BuildConfig
import com.air.cleaner.core.permissions.MediaPermissionState
import com.air.cleaner.core.ui.theme.CleanerTheme
import com.air.cleaner.data.media.AndroidMediaStoreRepository
import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.data.media.SharedPreferencesPerceptualFingerprintCache
import com.air.cleaner.data.media.SharedPreferencesSimilarScreenshotResultCache
import com.air.cleaner.data.media.SimilarPhotoScanScope
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.feature.dashboard.CleanupCategory
import com.air.cleaner.feature.dashboard.CleanupPriority
import com.air.cleaner.feature.dashboard.localizedPreviewCleanupCategories
import com.air.cleaner.feature.onboarding.OnboardingScreen
import com.air.cleaner.feature.photos.PhotoDeleteConfirmationDialog
import com.air.cleaner.feature.photos.PhotoDeleteResultDialog
import com.air.cleaner.feature.photos.PhotoDeleteReconciliation
import com.air.cleaner.feature.photos.PhotoDeletionResult
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import com.air.cleaner.feature.photos.PhotoPostDeleteAction
import com.air.cleaner.feature.photos.PhotoPostDeleteStatus
import com.air.cleaner.feature.photos.PhotoReviewKeepStrategy
import com.air.cleaner.feature.photos.PhotoReviewScreen
import com.air.cleaner.feature.photos.SimilarScreenshotReviewFilter
import com.air.cleaner.feature.photos.similarPhotoMatchExplanation
import com.air.cleaner.feature.photos.similarScreenshotMatchExplanation
import com.air.cleaner.feature.photos.similarScreenshotTrustSummary
import com.air.cleaner.feature.photos.toSimilarScreenshotReviewWorkflow
import com.google.firebase.analytics.FirebaseAnalytics
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.roundToInt

@Composable
fun AIPhotoCleanerApp(
    debugSimilarPhotoRelativePathPrefix: String? = null,
) {
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
            var analyticsEnabled by remember {
                mutableStateOf(context.cleanerPreferences().analyticsEnabled())
            }
            val analyticsDiagnosticsEvents = remember { mutableStateListOf<CleanerTelemetryEvent>() }
            val analyticsDiagnosticsTelemetry = remember {
                AnalyticsDiagnosticsTelemetry { events ->
                    analyticsDiagnosticsEvents.clear()
                    analyticsDiagnosticsEvents.addAll(events)
                }
            }
            val analyticsPrivacyController = remember {
                FirebaseAnalyticsPrivacyController(
                    FirebaseAnalyticsCollectionAdapter(FirebaseAnalytics.getInstance(context)),
                )
            }
            val telemetry = remember {
                ProductAnalyticsWithDiagnosticsTelemetry(
                    productTelemetry = SafeCleanerTelemetry(
                        CompositeCleanerTelemetry(
                            FirebaseCleanerTelemetry(FirebaseAnalytics.getInstance(context)),
                            LogcatCleanerTelemetry(),
                        ),
                    ),
                    diagnosticsTelemetry = SafeCleanerTelemetry(
                        analyticsDiagnosticsTelemetry,
                    ),
                    analyticsEnabled = { analyticsEnabled },
                )
            }
            var scanSummary by remember { mutableStateOf<MediaScanSummary?>(null) }
            var largeVideos by remember { mutableStateOf<List<MediaItem>?>(null) }
            var duplicatePhotoGroups by remember { mutableStateOf<List<DuplicateGroup>?>(null) }
            var similarPhotoGroups by remember { mutableStateOf<List<DuplicateGroup>?>(null) }
            var similarScreenshotGroups by remember { mutableStateOf<List<DuplicateGroup>?>(null) }
            var similarScreenshotReviewStatus by remember { mutableStateOf(SimilarScreenshotReviewStatus.Loading) }
            var similarScreenshotScanStartedAtMillis by remember { mutableStateOf<Long?>(null) }
            var similarScreenshotScanSource by remember {
                mutableStateOf(SimilarScreenshotScanSource.ColdScan)
            }
            var scanStatus by remember { mutableStateOf(MediaScanStatus()) }
            var navigationState by remember { mutableStateOf(AppNavigationState()) }
            var pendingDeleteSummary by remember { mutableStateOf<PhotoDeletionSummary?>(null) }
            var pendingDeleteReviewContext by remember { mutableStateOf<PhotoDeleteReviewContext?>(null) }
            var deleteResult by remember { mutableStateOf<PhotoDeletionResult?>(null) }
            var deleteResultReviewContext by remember { mutableStateOf<PhotoDeleteReviewContext?>(null) }
            var lastDeletedResult by remember { mutableStateOf<PhotoDeletionResult?>(null) }
            var lastDeletedSummary by remember { mutableStateOf<PhotoDeletionSummary?>(null) }
            var lastDeletedReviewContext by remember { mutableStateOf<PhotoDeleteReviewContext?>(null) }
            var lastStillExistingDeletedUris by remember { mutableStateOf<List<String>?>(null) }
            var scanRefreshKey by remember { mutableStateOf(0) }
            val deleteLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                pendingDeleteSummary?.let { summary ->
                    when (pendingDeleteReviewContext) {
                        PhotoDeleteReviewContext.SimilarPhotos -> telemetry.track(
                            SimilarPhotoTelemetry.systemDeleteResult(
                                confirmed = result.resultCode == Activity.RESULT_OK,
                                summary = summary,
                            ),
                        )
                        PhotoDeleteReviewContext.SimilarScreenshots -> telemetry.track(
                            SimilarScreenshotTelemetry.systemDeleteResult(
                                confirmed = result.resultCode == Activity.RESULT_OK,
                                summary = summary,
                            ),
                        )
                        PhotoDeleteReviewContext.LargeVideos -> telemetry.track(
                            LargeVideoTelemetry.systemDeleteResult(
                                confirmed = result.resultCode == Activity.RESULT_OK,
                                summary = summary,
                            ),
                        )
                        PhotoDeleteReviewContext.DuplicatePhotos,
                        null -> Unit
                    }
                    val deletionResult = PhotoDeletionResult.fromSystemResult(
                        summary = summary,
                        systemConfirmed = result.resultCode == Activity.RESULT_OK,
                    )
                    deleteResultReviewContext = pendingDeleteReviewContext
                    deleteResult = deletionResult
                    if (deletionResult.shouldRefreshScan) {
                        lastDeletedResult = deletionResult
                        lastDeletedSummary = summary
                        lastDeletedReviewContext = pendingDeleteReviewContext
                        lastStillExistingDeletedUris = null
                        if (pendingDeleteReviewContext == PhotoDeleteReviewContext.SimilarScreenshots) {
                            similarScreenshotScanSource = SimilarScreenshotScanSource.PostDeleteRefresh
                        }
                        scanRefreshKey += 1
                    }
                }
                pendingDeleteSummary = null
                pendingDeleteReviewContext = null
            }

            LaunchedEffect(context, permissionState.access, scanRefreshKey) {
                val repository = AndroidMediaStoreRepository(
                    contentResolver = context.contentResolver,
                    similarScreenshotFingerprintCache = SharedPreferencesPerceptualFingerprintCache(context),
                    similarScreenshotResultCache = SharedPreferencesSimilarScreenshotResultCache(context),
                    similarPhotoScanScope = SimilarPhotoScanScope(
                        relativePathPrefix = debugSimilarPhotoRelativePathPrefix.takeIf { BuildConfig.DEBUG },
                    ),
                )
                val shouldUseCachedSimilarResults =
                    lastDeletedResult?.shouldRefreshScan != true &&
                        similarScreenshotScanSource == SimilarScreenshotScanSource.ColdScan
                if (shouldUseCachedSimilarResults) {
                    val cacheLoadStartedAtMillis = SystemClock.elapsedRealtime()
                    val cachedSimilarScreenshotResult = withContext(Dispatchers.IO) {
                        repository.cachedSimilarScreenshotGroupResult()
                    }
                    telemetry.track(
                        SimilarScreenshotTelemetry.cacheLoaded(
                            elapsedMillis = SystemClock.elapsedRealtime() - cacheLoadStartedAtMillis,
                            groups = cachedSimilarScreenshotResult.groups,
                            filteredToEmpty = cachedSimilarScreenshotResult.filteredToEmpty,
                        ),
                    )
                    if (cachedSimilarScreenshotResult.groups.isNotEmpty()) {
                        similarScreenshotGroups = cachedSimilarScreenshotResult.groups
                        similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.CachedRefreshing
                    } else if (cachedSimilarScreenshotResult.filteredToEmpty) {
                        similarScreenshotGroups = emptyList()
                        similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.FilteredCacheEmpty
                    } else {
                        similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.Loading
                    }
                } else {
                    similarScreenshotGroups = null
                    similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.Loading
                }
                scanStatus = MediaScanStatus(MediaScanPhase.CountingLibrary)
                scanSummary = withContext(Dispatchers.IO) {
                    repository.scanSummary()
                }
                val largeVideoScanStartedAtMillis = SystemClock.elapsedRealtime()
                val scannedLargeVideos = withContext(Dispatchers.IO) {
                    repository.scanLargeVideos()
                }
                largeVideos = scannedLargeVideos
                telemetry.track(
                    LargeVideoTelemetry.scanCompleted(
                        elapsedMillis = SystemClock.elapsedRealtime() - largeVideoScanStartedAtMillis,
                        videos = scannedLargeVideos,
                    ),
                )
                scanStatus = MediaScanStatus(
                    phase = MediaScanPhase.FindingSimilarScreenshots,
                    summary = scanSummary,
                )
                similarScreenshotScanStartedAtMillis = SystemClock.elapsedRealtime()
                val freshSimilarScreenshotResult = withContext(Dispatchers.IO) {
                    repository.scanSimilarScreenshotGroupResult()
                }
                val freshSimilarScreenshotGroups = freshSimilarScreenshotResult.groups
                similarScreenshotGroups = freshSimilarScreenshotGroups
                similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.Fresh
                telemetry.track(
                    SimilarScreenshotTelemetry.scanCompleted(
                        elapsedMillis = SystemClock.elapsedRealtime() -
                            (similarScreenshotScanStartedAtMillis ?: SystemClock.elapsedRealtime()),
                        scanSummary = scanSummary,
                        result = freshSimilarScreenshotResult,
                        status = SimilarScreenshotReviewStatus.Fresh,
                        source = similarScreenshotScanSource,
                    ),
                )
                similarScreenshotScanSource = SimilarScreenshotScanSource.ColdScan
                withContext(Dispatchers.IO) {
                    repository.saveSimilarScreenshotGroups(freshSimilarScreenshotGroups)
                }
                scanStatus = MediaScanStatus(
                    phase = MediaScanPhase.FindingDuplicatePhotos,
                    summary = scanSummary,
                )
                val similarPhotoScanStartedAtMillis = SystemClock.elapsedRealtime()
                val similarPhotoResult = withContext(Dispatchers.IO) {
                    repository.scanSimilarPhotoGroupResult()
                }
                similarPhotoGroups = similarPhotoResult.groups
                telemetry.track(
                    SimilarPhotoTelemetry.scanCompleted(
                        elapsedMillis = SystemClock.elapsedRealtime() - similarPhotoScanStartedAtMillis,
                        scanSummary = scanSummary,
                        result = similarPhotoResult,
                    ),
                )
                duplicatePhotoGroups = withContext(Dispatchers.IO) {
                    repository.scanDuplicatePhotoGroups()
                }
                scanStatus = MediaScanStatus(
                    phase = MediaScanPhase.ReconcilingDeletes,
                    summary = scanSummary,
                )
                lastStillExistingDeletedUris = withContext(Dispatchers.IO) {
                    lastDeletedSummary
                        ?.takeIf { lastDeletedResult?.shouldRefreshScan == true }
                        ?.let { deleteSummary ->
                            repository.contentUrisStillPresent(deleteSummary.contentUris)
                        }
                }
                scanStatus = MediaScanStatus(
                    phase = MediaScanPhase.Complete,
                    summary = scanSummary,
                )
            }

            val postDeleteStatus = lastDeletedReviewContext?.let { reviewContext ->
                PhotoPostDeleteStatus.from(
                    reconciliation = PhotoDeleteReconciliation.from(
                        summary = lastDeletedSummary,
                        result = lastDeletedResult,
                        currentGroups = reviewContext.groupsForReconciliation(
                            duplicatePhotoGroups = duplicatePhotoGroups.orEmpty(),
                            similarPhotoGroups = similarPhotoGroups.orEmpty(),
                            similarScreenshotGroups = similarScreenshotGroups.orEmpty(),
                        ),
                        stillExistingContentUris = lastStillExistingDeletedUris,
                        remainingHighPriorityGroupCount = reviewContext.remainingHighPriorityGroups(
                            similarScreenshotGroups = similarScreenshotGroups.orEmpty(),
                        ),
                        remainingMediumPriorityGroupCount = reviewContext.remainingMediumPriorityGroups(
                            similarScreenshotGroups = similarScreenshotGroups.orEmpty(),
                        ),
                        keepStrategy = if (reviewContext == PhotoDeleteReviewContext.SimilarScreenshots) {
                            PhotoReviewKeepStrategy.Newest
                        } else {
                            null
                        },
                    ),
                )
            }
            val largeVideoPostDeleteStatus = if (lastDeletedReviewContext == PhotoDeleteReviewContext.LargeVideos) {
                val summary = lastDeletedSummary
                val result = lastDeletedResult
                if (summary != null && result != null && (!result.shouldRefreshScan || lastStillExistingDeletedUris != null)) {
                    LargeVideoPostDeleteStatus.from(
                        summary = summary,
                        result = result,
                        stillExistingContentUris = lastStillExistingDeletedUris,
                        formatBytes = ::formatBytes,
                    )
                } else {
                    null
                }
            } else {
                null
            }

            MainAppShell(
                navigationState = navigationState,
                scanSummary = scanSummary,
                scanStatus = scanStatus,
                mediaPermissionState = permissionState,
                largeVideos = largeVideos,
                duplicatePhotoGroups = duplicatePhotoGroups,
                similarPhotoGroups = similarPhotoGroups,
                similarScreenshotGroups = similarScreenshotGroups,
                similarScreenshotReviewStatus = similarScreenshotReviewStatus,
                telemetry = telemetry,
                analyticsEnabled = analyticsEnabled,
                analyticsDiagnosticsEvents = analyticsDiagnosticsEvents,
                onTabSelected = { navigationState = navigationState.selectTab(it) },
                onOpenDuplicatePhotos = {
                    navigationState = navigationState.openDuplicatePhotos(
                        scanComplete = duplicatePhotoGroups != null,
                    )
                },
                onOpenSimilarPhotos = {
                    telemetry.track(
                        SimilarPhotoTelemetry.entryTapped(
                            groups = similarPhotoGroups,
                        ),
                    )
                    navigationState = navigationState.openSimilarPhotos(
                        scanComplete = similarPhotoGroups != null,
                    )
                },
                onOpenLargeVideos = {
                    telemetry.track(LargeVideoTelemetry.entryTapped(videos = largeVideos))
                    navigationState = navigationState.openLargeVideos(
                        scanComplete = largeVideos != null,
                    )
                },
                onOpenSimilarScreenshots = {
                    telemetry.track(
                        SimilarScreenshotTelemetry.entryTapped(
                            groups = similarScreenshotGroups,
                            status = similarScreenshotReviewStatus,
                        ),
                    )
                    navigationState = navigationState.openSimilarScreenshots(
                        scanComplete = similarScreenshotGroups != null,
                    )
                },
                onRequestPermission = {
                    permissionLauncher.launch(mediaPermissionsForCurrentDevice())
                },
                onRescanSimilarScreenshots = {
                    telemetry.track(
                        SimilarScreenshotTelemetry.rescanTapped(
                            status = similarScreenshotReviewStatus,
                            currentGroupCount = similarScreenshotGroups.orEmpty().size,
                        ),
                    )
                    similarScreenshotGroups = null
                    similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.Loading
                    similarScreenshotScanSource = SimilarScreenshotScanSource.ManualRescan
                    scanRefreshKey += 1
                },
                onBackToPhotos = { navigationState = navigationState.selectTab(AppTab.Photos) },
                onBackToVideos = { navigationState = navigationState.selectTab(AppTab.Videos) },
                onPostDeleteAction = { action ->
                    if (postDeleteStatus != null) {
                        when (lastDeletedReviewContext) {
                            PhotoDeleteReviewContext.SimilarPhotos -> telemetry.track(
                                SimilarPhotoTelemetry.postDeleteAction(
                                    action = action,
                                    status = postDeleteStatus,
                                ),
                            )
                            PhotoDeleteReviewContext.SimilarScreenshots -> telemetry.track(
                                SimilarScreenshotTelemetry.postDeleteAction(
                                    action = action,
                                    status = postDeleteStatus,
                                ),
                            )
                            PhotoDeleteReviewContext.DuplicatePhotos,
                            PhotoDeleteReviewContext.LargeVideos,
                            null -> Unit
                        }
                    }
                    lastDeletedSummary = null
                    lastDeletedResult = null
                    lastDeletedReviewContext = null
                    if (action == PhotoPostDeleteAction.ReturnToPhotos) {
                        navigationState = navigationState.selectTab(AppTab.Photos)
                    }
                },
                pendingDeleteSummary = pendingDeleteSummary,
                pendingDeleteReviewContext = pendingDeleteReviewContext,
                deleteResult = deleteResult,
                deleteResultReviewContext = deleteResultReviewContext,
                postDeleteStatus = postDeleteStatus,
                largeVideoPostDeleteStatus = largeVideoPostDeleteStatus,
                postDeleteReviewContext = lastDeletedReviewContext,
                onRequestDeleteConfirmation = { summary, reviewContext ->
                    pendingDeleteSummary = summary
                    pendingDeleteReviewContext = reviewContext
                },
                onDismissDeleteConfirmation = {
                    pendingDeleteSummary = null
                    pendingDeleteReviewContext = null
                },
                onConfirmDelete = { summary ->
                    val sender = context.createSystemDeleteRequest(summary.contentUris)
                    when (pendingDeleteReviewContext) {
                        PhotoDeleteReviewContext.SimilarPhotos -> telemetry.track(
                            SimilarPhotoTelemetry.deleteRequested(
                                summary = summary,
                                systemDialogAvailable = sender != null,
                            ),
                        )
                        PhotoDeleteReviewContext.SimilarScreenshots -> telemetry.track(
                            SimilarScreenshotTelemetry.deleteRequested(
                                summary = summary,
                                systemDialogAvailable = sender != null,
                            ),
                        )
                        PhotoDeleteReviewContext.LargeVideos -> telemetry.track(
                            LargeVideoTelemetry.deleteRequested(
                                summary = summary,
                                systemDialogAvailable = sender != null,
                            ),
                        )
                        PhotoDeleteReviewContext.DuplicatePhotos,
                        null -> Unit
                    }
                    if (sender == null) {
                        deleteResultReviewContext = pendingDeleteReviewContext
                        deleteResult = PhotoDeletionResult.blocked(summary)
                        pendingDeleteSummary = null
                        pendingDeleteReviewContext = null
                    } else {
                        deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                },
                onDismissDeleteResult = {
                    deleteResult = null
                    deleteResultReviewContext = null
                },
                onAnalyticsEnabledChange = { enabled ->
                    analyticsEnabled = enabled
                    analyticsPrivacyController.setProductAnalyticsEnabled(enabled)
                    context.cleanerPreferences()
                        .edit()
                        .putBoolean(PREF_ANALYTICS_ENABLED, enabled)
                        .apply()
                },
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
    scanStatus: MediaScanStatus,
    mediaPermissionState: MediaPermissionState,
    largeVideos: List<MediaItem>?,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    similarPhotoGroups: List<DuplicateGroup>?,
    similarScreenshotGroups: List<DuplicateGroup>?,
    similarScreenshotReviewStatus: SimilarScreenshotReviewStatus,
    telemetry: CleanerTelemetry,
    analyticsEnabled: Boolean,
    analyticsDiagnosticsEvents: List<CleanerTelemetryEvent>,
    onTabSelected: (AppTab) -> Unit,
    onOpenDuplicatePhotos: () -> Unit,
    onOpenSimilarPhotos: () -> Unit,
    onOpenLargeVideos: () -> Unit,
    onOpenSimilarScreenshots: () -> Unit,
    onRequestPermission: () -> Unit,
    onRescanSimilarScreenshots: () -> Unit,
    onBackToPhotos: () -> Unit,
    onBackToVideos: () -> Unit,
    onPostDeleteAction: (PhotoPostDeleteAction) -> Unit,
    pendingDeleteSummary: PhotoDeletionSummary?,
    pendingDeleteReviewContext: PhotoDeleteReviewContext?,
    deleteResult: PhotoDeletionResult?,
    deleteResultReviewContext: PhotoDeleteReviewContext?,
    postDeleteStatus: PhotoPostDeleteStatus?,
    largeVideoPostDeleteStatus: LargeVideoPostDeleteStatus?,
    postDeleteReviewContext: PhotoDeleteReviewContext?,
    onRequestDeleteConfirmation: (PhotoDeletionSummary, PhotoDeleteReviewContext) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDelete: (PhotoDeletionSummary) -> Unit,
    onDismissDeleteResult: () -> Unit,
    onAnalyticsEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val largeVideoSampleLedgerStore = remember {
        SharedPreferencesLargeVideoCompressionSampleLedgerStore(context)
    }
    var largeVideoSampleLedger by remember {
        mutableStateOf(largeVideoSampleLedgerStore.load())
    }
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
                mediaPermissionState = mediaPermissionState,
                largeVideos = largeVideos,
                duplicatePhotoGroups = duplicatePhotoGroups,
                similarPhotoGroups = similarPhotoGroups,
                similarScreenshotGroups = similarScreenshotGroups,
                similarScreenshotReviewStatus = similarScreenshotReviewStatus,
                analyticsEnabled = analyticsEnabled,
                analyticsDiagnosticsEvents = analyticsDiagnosticsEvents,
                onOpenDuplicatePhotos = onOpenDuplicatePhotos,
                onOpenSimilarPhotos = onOpenSimilarPhotos,
                onOpenLargeVideos = onOpenLargeVideos,
                onOpenSimilarScreenshots = onOpenSimilarScreenshots,
                onRequestPermission = onRequestPermission,
                onAnalyticsEnabledChange = onAnalyticsEnabledChange,
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
                            PhotoDeleteReviewContext.DuplicatePhotos,
                        )
                    },
                    postDeleteStatus = postDeleteStatus.takeIf {
                        postDeleteReviewContext == PhotoDeleteReviewContext.DuplicatePhotos
                    },
                    onPostDeleteAction = onPostDeleteAction,
                )
            }
            AppScreen.SimilarPhotoReview -> Box(modifier = Modifier.padding(padding)) {
                PhotoReviewScreen(
                    title = "Similar photos",
                    groups = similarPhotoGroups.orEmpty(),
                    onBack = onBackToPhotos,
                    onContinue = { selectionState ->
                        val summary = PhotoDeletionSummary.fromItems(selectionState.selectedItems)
                        telemetry.track(
                            SimilarPhotoTelemetry.continueTapped(
                                summary = summary,
                                totalGroups = similarPhotoGroups.orEmpty().size,
                            ),
                        )
                        onRequestDeleteConfirmation(
                            summary,
                            PhotoDeleteReviewContext.SimilarPhotos,
                        )
                    },
                    onReviewReady = { selectionState ->
                        telemetry.track(
                            SimilarPhotoTelemetry.reviewShown(
                                groups = similarPhotoGroups.orEmpty(),
                                selectedCount = selectionState.selectedCount,
                                selectedBytes = selectionState.selectedBytes,
                            ),
                        )
                    },
                    onSelectionChanged = { selectionState, action ->
                        telemetry.track(
                            SimilarPhotoTelemetry.selectionChanged(
                                action = action.analyticsValue,
                                selectedCount = selectionState.selectedCount,
                                selectedBytes = selectionState.selectedBytes,
                                totalGroups = similarPhotoGroups.orEmpty().size,
                            ),
                        )
                    },
                    onPreviewAction = { previewEvent ->
                        telemetry.track(
                            SimilarPhotoTelemetry.previewAction(
                                action = previewEvent.action.analyticsValue,
                                selectedCount = previewEvent.selectedCount,
                                selectedBytes = previewEvent.selectedBytes,
                                totalGroups = similarPhotoGroups.orEmpty().size,
                                photoIndex = previewEvent.photoIndex,
                                photoCount = previewEvent.photoCount,
                                selectedForDeletion = previewEvent.selectedForDeletion,
                                recommendedKeep = previewEvent.recommendedKeep,
                            ),
                        )
                    },
                    postDeleteStatus = postDeleteStatus.takeIf {
                        postDeleteReviewContext == PhotoDeleteReviewContext.SimilarPhotos
                    },
                    onPostDeleteAction = onPostDeleteAction,
                    emptyTitle = "No similar photos found",
                    emptyMessage = "Same-scene bursts and near-identical camera shots will appear here once detected.",
                    itemMatchLabel = "Similar photo",
                    groupMatchExplanation = { group -> group.similarPhotoMatchExplanation() },
                    keepStrategy = PhotoReviewKeepStrategy.Recommended,
                )
            }
            AppScreen.LargeVideoReview -> Box(modifier = Modifier.padding(padding)) {
                LargeVideoReviewScreen(
                    videos = largeVideos.orEmpty(),
                    postDeleteStatus = largeVideoPostDeleteStatus,
                    onBack = onBackToVideos,
                    onReviewReady = { selectionState ->
                        telemetry.track(LargeVideoTelemetry.reviewShown(selectionState))
                    },
                    onSelectionChanged = { action, selectionState ->
                        telemetry.track(
                            LargeVideoTelemetry.selectionChanged(
                                action = action,
                                state = selectionState,
                            ),
                        )
                    },
                    onContinue = { selectionState ->
                        telemetry.track(LargeVideoTelemetry.continueTapped(selectionState))
                        onRequestDeleteConfirmation(
                            selectionState.deleteSummary(),
                            PhotoDeleteReviewContext.LargeVideos,
                        )
                    },
                    onCompressionCompleted = { profile, elapsedMillis, results ->
                        telemetry.track(
                            LargeVideoTelemetry.compressionCompleted(
                                profile = profile,
                                elapsedMillis = elapsedMillis,
                                results = results,
                            ),
                        )
                        val updatedLedger = largeVideoSampleLedger.appendCompressionResults(
                            results = results,
                            profile = profile,
                            sourceVideos = largeVideos.orEmpty(),
                        )
                        largeVideoSampleLedgerStore.save(updatedLedger)
                        largeVideoSampleLedger = updatedLedger
                        telemetry.track(
                            LargeVideoTelemetry.compressionSampleMatrixUpdated(
                                updatedLedger.toSampleMatrix(),
                            ),
                        )
                    },
                    onCompressedCopyOpenRequested = { result, opened ->
                        telemetry.track(
                            LargeVideoTelemetry.compressedCopyOpenRequested(
                                result = result,
                                opened = opened,
                            ),
                        )
                    },
                )
            }
            AppScreen.SimilarScreenshotReview -> Box(modifier = Modifier.padding(padding)) {
                if (similarScreenshotGroups == null) {
                    SimilarScreenshotsLoadingScreen(
                        scanStatus = scanStatus,
                        onBack = onBackToPhotos,
                    )
                } else {
                    val reviewActionState = similarScreenshotReviewStatus.reviewActionState(
                        hasGroups = similarScreenshotGroups.isNotEmpty(),
                    )
                    PhotoReviewScreen(
                        title = "Similar screenshots",
                        groups = similarScreenshotGroups,
                        onBack = onBackToPhotos,
                        onContinue = { selectionState ->
                            val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
                                groups = similarScreenshotGroups,
                                selectionState = selectionState,
                                keepStrategy = PhotoReviewKeepStrategy.Newest,
                            )
                            telemetry.track(
                                SimilarScreenshotTelemetry.continueTapped(
                                    summary = summary,
                                    totalGroups = similarScreenshotGroups.size,
                                    priorityGroups = summary.priorityGroupCount,
                                ),
                            )
                            onRequestDeleteConfirmation(
                                summary,
                                PhotoDeleteReviewContext.SimilarScreenshots,
                            )
                        },
                        onReviewReady = { selectionState ->
                            telemetry.track(
                                SimilarScreenshotTelemetry.reviewShown(
                                    groups = similarScreenshotGroups,
                                    selectedCount = selectionState.selectedCount,
                                    selectedBytes = selectionState.selectedBytes,
                                    priorityGroups = similarScreenshotGroups.priorityGroupCountForSimilarScreenshots(),
                                    status = similarScreenshotReviewStatus,
                                ),
                            )
                        },
                        onSelectionChanged = { selectionState, action ->
                            telemetry.track(
                                SimilarScreenshotTelemetry.selectionChanged(
                                    action = action.analyticsValue,
                                    selectedCount = selectionState.selectedCount,
                                    selectedBytes = selectionState.selectedBytes,
                                    totalGroups = similarScreenshotGroups.size,
                                    priorityGroups = similarScreenshotGroups.priorityGroupCountForSimilarScreenshots(),
                                ),
                            )
                        },
                        onPreviewAction = { previewEvent ->
                            telemetry.track(
                                SimilarScreenshotTelemetry.previewAction(
                                    action = previewEvent.action.analyticsValue,
                                    selectedCount = previewEvent.selectedCount,
                                    selectedBytes = previewEvent.selectedBytes,
                                    totalGroups = similarScreenshotGroups.size,
                                    priorityGroups = similarScreenshotGroups.priorityGroupCountForSimilarScreenshots(),
                                    photoIndex = previewEvent.photoIndex,
                                    photoCount = previewEvent.photoCount,
                                    selectedForDeletion = previewEvent.selectedForDeletion,
                                    recommendedKeep = previewEvent.recommendedKeep,
                                ),
                            )
                        },
                        postDeleteStatus = postDeleteStatus.takeIf {
                            postDeleteReviewContext == PhotoDeleteReviewContext.SimilarScreenshots
                        },
                        onPostDeleteAction = onPostDeleteAction,
                        emptyTitle = similarScreenshotReviewStatus.emptyTitle(scanStatus),
                        emptyMessage = similarScreenshotReviewStatus.emptyMessage(scanStatus),
                        emptyActionLabel = similarScreenshotReviewStatus.emptyActionLabel(),
                        onEmptyAction = onRescanSimilarScreenshots,
                        itemMatchLabel = "Similar screenshot",
                        groupMatchExplanation = { group -> group.similarScreenshotMatchExplanation() },
                        groupTrustSummary = { group ->
                            group.similarScreenshotTrustSummary(PhotoReviewKeepStrategy.Newest)
                        },
                        noticeTitle = similarScreenshotReviewStatus.noticeTitle(),
                        noticeMessage = similarScreenshotReviewStatus.noticeMessage(),
                        reviewActionTitle = reviewActionState?.title,
                        reviewActionMessage = reviewActionState?.message,
                        reviewActionLabel = reviewActionState?.actionLabel,
                        reviewActionEnabled = reviewActionState?.actionEnabled ?: true,
                        onReviewAction = onRescanSimilarScreenshots,
                        keepStrategy = PhotoReviewKeepStrategy.Newest,
                    )
                }
            }
        }
        pendingDeleteSummary?.let { summary ->
            PhotoDeleteConfirmationDialog(
                summary = summary,
                onDismiss = onDismissDeleteConfirmation,
                onConfirmDelete = { onConfirmDelete(summary) },
                title = pendingDeleteReviewContext?.deleteDialogTitle ?: "Delete selected photos?",
                itemLabel = pendingDeleteReviewContext?.deleteDialogItemLabel ?: "Photos",
            )
        }
        deleteResult?.let { result ->
            PhotoDeleteResultDialog(
                result = result,
                onDone = onDismissDeleteResult,
                deletedMessage = deleteResultReviewContext?.deleteResultDeletedMessage
                    ?: postDeleteReviewContext?.deleteResultDeletedMessage
                    ?: "Your library was refreshed. Continue reviewing any remaining duplicate groups.",
                canceledMessage = deleteResultReviewContext?.deleteResultCanceledMessage
                    ?: postDeleteReviewContext?.deleteResultCanceledMessage
                    ?: "No photos were removed. Your previous selection is still available for review.",
            )
        }
    }
}

@Composable
private fun SimilarScreenshotsLoadingScreen(
    scanStatus: MediaScanStatus,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Similar screenshots",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = scanStatus.similarLoadingStepLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Text(
                    text = "Looking for near-identical screenshots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { scanStatus.similarLoadingProgress() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = scanStatus.similarLoadingScopeLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = scanStatus.similarLoadingMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = scanStatus.similarLoadingExpectationLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "High-confidence groups appear automatically. We never delete anything from scan results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack,
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun TabScreen(
    tab: AppTab,
    scanSummary: MediaScanSummary?,
    mediaPermissionState: MediaPermissionState,
    largeVideos: List<MediaItem>?,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    similarPhotoGroups: List<DuplicateGroup>?,
    similarScreenshotGroups: List<DuplicateGroup>?,
    similarScreenshotReviewStatus: SimilarScreenshotReviewStatus,
    analyticsEnabled: Boolean,
    analyticsDiagnosticsEvents: List<CleanerTelemetryEvent>,
    onOpenDuplicatePhotos: () -> Unit,
    onOpenSimilarPhotos: () -> Unit,
    onOpenLargeVideos: () -> Unit,
    onOpenSimilarScreenshots: () -> Unit,
    onRequestPermission: () -> Unit,
    onAnalyticsEnabledChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    when (tab) {
        AppTab.Clean -> CleanTabScreen(
            scanSummary = scanSummary,
            mediaPermissionState = mediaPermissionState,
            duplicatePhotoGroups = duplicatePhotoGroups,
            similarPhotoGroups = similarPhotoGroups,
            similarScreenshotGroups = similarScreenshotGroups,
            onOpenDuplicatePhotos = onOpenDuplicatePhotos,
            onOpenSimilarPhotos = onOpenSimilarPhotos,
            onOpenSimilarScreenshots = onOpenSimilarScreenshots,
            onRequestPermission = onRequestPermission,
            contentPadding = contentPadding,
        )
        AppTab.Photos -> PhotosTabScreen(
            scanSummary = scanSummary,
            mediaPermissionState = mediaPermissionState,
            duplicatePhotoGroups = duplicatePhotoGroups,
            similarPhotoGroups = similarPhotoGroups,
            similarScreenshotGroups = similarScreenshotGroups,
            similarScreenshotReviewStatus = similarScreenshotReviewStatus,
            onOpenDuplicatePhotos = onOpenDuplicatePhotos,
            onOpenSimilarPhotos = onOpenSimilarPhotos,
            onOpenSimilarScreenshots = onOpenSimilarScreenshots,
            onRequestPermission = onRequestPermission,
            contentPadding = contentPadding,
        )
        AppTab.Videos -> VideosTabScreen(
            videoBytesLabel = scanSummary?.videoBytes.toStorageLabel(),
            largeVideos = largeVideos,
            onOpenLargeVideos = onOpenLargeVideos,
            contentPadding = contentPadding,
        )
        AppTab.Settings -> SettingsTabScreen(
            analyticsEnabled = analyticsEnabled,
            mediaPermissionState = mediaPermissionState,
            analyticsDiagnosticsEvents = analyticsDiagnosticsEvents,
            onAnalyticsEnabledChange = onAnalyticsEnabledChange,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun CleanTabScreen(
    scanSummary: MediaScanSummary?,
    mediaPermissionState: MediaPermissionState,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    similarPhotoGroups: List<DuplicateGroup>?,
    similarScreenshotGroups: List<DuplicateGroup>?,
    onOpenDuplicatePhotos: () -> Unit,
    onOpenSimilarPhotos: () -> Unit,
    onOpenSimilarScreenshots: () -> Unit,
    onRequestPermission: () -> Unit,
    contentPadding: PaddingValues,
) {
    val categories = scanSummary
        ?.toCleanupCategories(
            duplicatePhotoGroups = duplicatePhotoGroups,
            similarPhotoGroups = similarPhotoGroups,
            similarScreenshotGroups = similarScreenshotGroups,
        )
        ?: localizedPreviewCleanupCategories()
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "AI Cleaner", action = "Premium")
        StorageHero(
            recoverableSpaceLabel = scanSummary?.totalBytes.toStorageLabel(),
            scannedItemsLabel = scanSummary?.totalCount.toScannedItemsLabel(),
        )
        LimitedLibraryAccessNotice(
            mediaPermissionState = mediaPermissionState,
            onRequestPermission = onRequestPermission,
        )
        SectionTitle("Biggest wins")
        categories.take(3).forEach { category ->
            MetricRow(
                icon = category.icon(),
                title = category.title,
                subtitle = category.subtitle,
                metric = category.recoverableLabel,
                onClick = when (category.id) {
                    "duplicate_photos" -> onOpenDuplicatePhotos.takeIf { duplicatePhotoGroups != null }
                    "similar_photos" -> onOpenSimilarPhotos.takeIf { similarPhotoGroups != null }
                    "screenshots" -> onOpenSimilarScreenshots
                    else -> null
                },
            )
        }
        TrustStrip()
    }
}

@Composable
private fun PhotosTabScreen(
    scanSummary: MediaScanSummary?,
    mediaPermissionState: MediaPermissionState,
    duplicatePhotoGroups: List<DuplicateGroup>?,
    similarPhotoGroups: List<DuplicateGroup>?,
    similarScreenshotGroups: List<DuplicateGroup>?,
    similarScreenshotReviewStatus: SimilarScreenshotReviewStatus,
    onOpenDuplicatePhotos: () -> Unit,
    onOpenSimilarPhotos: () -> Unit,
    onOpenSimilarScreenshots: () -> Unit,
    onRequestPermission: () -> Unit,
    contentPadding: PaddingValues,
) {
    val similarPhotosEntry = similarPhotosEntryState(
        groups = similarPhotoGroups,
        formatBytes = ::formatBytes,
    )
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "Photos", action = "Sort")
        LimitedLibraryAccessNotice(
            mediaPermissionState = mediaPermissionState,
            onRequestPermission = onRequestPermission,
        )
        FilterChips(labels = listOf("Duplicates", "Similar", "Blurry", "Screenshots"))
        MetricRow(
            icon = Icons.Rounded.Image,
            title = "Duplicate photos",
            subtitle = "Review likely matches and keep the best original",
            metric = duplicatePhotoGroups.toDuplicateMetricLabel(),
            onClick = onOpenDuplicatePhotos.takeIf { duplicatePhotoGroups != null },
        )
        MetricRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "Similar photos",
            subtitle = similarPhotosEntry.subtitle,
            metric = similarPhotosEntry.metric,
            onClick = onOpenSimilarPhotos.takeIf { similarPhotoGroups != null },
        )
        MetricRow(
            icon = Icons.Rounded.Shield,
            title = "Screenshots",
            subtitle = "Old captures grouped for quick review",
            metric = scanSummary?.screenshotBytes.toStorageLabel(),
            onClick = onOpenSimilarScreenshots,
        )
        BottomSummaryBar(text = "Safe selection protects at least one photo per group")
    }
}

@Composable
private fun VideosTabScreen(
    videoBytesLabel: String,
    largeVideos: List<MediaItem>?,
    onOpenLargeVideos: () -> Unit,
    contentPadding: PaddingValues,
) {
    val largeVideosEntry = largeVideosEntryState(
        videos = largeVideos,
        fallbackTotalBytesLabel = videoBytesLabel,
        formatBytes = ::formatBytes,
    )
    ScreenColumn(contentPadding = contentPadding) {
        TopHeader(title = "Videos", action = "Filter")
        PremiumSurface {
            Column(
                modifier = Modifier.clickable(
                    enabled = largeVideosEntry.actionEnabled,
                    onClick = onOpenLargeVideos,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Large videos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    largeVideosEntry.totalBytesLabel,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    largeVideosEntry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    largeVideosEntry.statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        largeVideosEntry.rows.take(5).forEach { row ->
            MetricRow(
                icon = Icons.Rounded.PlayCircle,
                title = row.title,
                subtitle = row.subtitle,
                metric = row.metric,
                onClick = onOpenLargeVideos.takeIf { largeVideosEntry.actionEnabled },
            )
        }
        BottomSummaryBar(text = "Compression estimate is shown before any destructive action")
    }
}

@Composable
private fun LargeVideoReviewScreen(
    videos: List<MediaItem>,
    postDeleteStatus: LargeVideoPostDeleteStatus?,
    isPremium: Boolean = false,
    hasRewardedCompressionUnlock: Boolean = false,
    onBack: () -> Unit,
    onReviewReady: (LargeVideoReviewSelectionState) -> Unit,
    onSelectionChanged: (LargeVideoSelectionAction, LargeVideoReviewSelectionState) -> Unit,
    onContinue: (LargeVideoReviewSelectionState) -> Unit,
    onCompressionCompleted: (LargeVideoCompressionProfile, Long, List<LargeVideoCompressionResult>) -> Unit,
    onCompressedCopyOpenRequested: (LargeVideoCompressionResult, Boolean) -> Unit,
) {
    var selectionState by remember(videos) {
        mutableStateOf(LargeVideoReviewSelectionState.fromVideos(videos))
    }
    var compressionProfile by remember(videos) {
        mutableStateOf(LargeVideoCompressionProfile.Balanced)
    }
    var compressionJobState by remember(videos) {
        mutableStateOf(LargeVideoCompressionJobState())
    }
    var completedCompressionResults by remember(videos) {
        mutableStateOf(emptyList<LargeVideoCompressionResult>())
    }
    var compressionRuntimeJob by remember(videos) {
        mutableStateOf<Job?>(null)
    }
    val compressionScope = rememberCoroutineScope()
    val context = LocalContext.current
    val largeVideoCompressor = remember(context) {
        Media3LargeVideoCompressor(context.applicationContext)
    }
    val compressionRunning = compressionJobState.phase == LargeVideoCompressionJobPhase.Running
    DisposableEffect(videos) {
        onDispose {
            compressionRuntimeJob?.cancel()
        }
    }
    LaunchedEffect(videos) {
        onReviewReady(selectionState)
    }
    val rows = selectionState.rows(formatBytes = ::formatBytes)
    val compressionEstimate = selectionState.compressionEstimate(
        profile = compressionProfile,
        formatBytes = ::formatBytes,
    )
    val compressionProfilePreviews = selectionState.compressionProfilePreviews(
        selectedProfile = compressionProfile,
        formatBytes = ::formatBytes,
    )
    val compressionAccessState = LargeVideoCompressionAccessState.fromSelection(
        selectedCount = selectionState.selectedCount,
        isPremium = isPremium,
        hasRewardedUnlock = hasRewardedCompressionUnlock,
    )
    val sourceCleanupState = LargeVideoSourceCleanupState.fromCompressionResults(
        selection = selectionState,
        results = completedCompressionResults,
        formatBytes = ::formatBytes,
    )
    val outputAuditState = LargeVideoCompressionOutputAuditState.fromResults(
        results = completedCompressionResults,
        formatBytes = ::formatBytes,
    )
    val openCompressedCopyActionState = LargeVideoCompressedCopyOpenActionState.fromResults(
        results = completedCompressionResults,
    )
    val bottomActionState = LargeVideoReviewBottomActionState.fromCleanupState(
        cleanup = sourceCleanupState,
        compressionRunning = compressionRunning,
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TopHeader(title = "Large videos", action = "Review")
            PremiumSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${selectionState.selectedCount} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${formatBytes(selectionState.selectedBytes)} selected for cleanup",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Review big videos before deleting. Nothing is removed until Android asks you to confirm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    postDeleteStatus?.let { status ->
                        LargeVideoPostDeleteStatusPanel(status)
                    }
                    Text(
                        text = "${compressionEstimate.profileLabel}: ${compressionEstimate.estimatedCompressedLabel} after compression",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Potential savings: ${compressionEstimate.estimatedSavingsLabel} (${compressionEstimate.estimatedSavingsRatioLabel})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = compressionEstimate.guidance,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        compressionProfilePreviews.forEach { preview ->
                            LargeVideoCompressionProfileRow(
                                preview = preview,
                                enabled = !compressionRunning,
                                onClick = {
                                    compressionProfile = preview.profile
                                    compressionJobState = LargeVideoCompressionJobState(profile = preview.profile)
                                    completedCompressionResults = emptyList()
                                },
                            )
                        }
                    }
                    LargeVideoCompressionAccessPanel(compressionAccessState)
                    LargeVideoCompressionJobPanel(
                        state = compressionJobState,
                        onStart = {
                            val selectedSnapshot = selectionState
                            val profileSnapshot = compressionProfile
                            compressionRuntimeJob?.cancel()
                            completedCompressionResults = emptyList()
                            compressionJobState = LargeVideoCompressionJobState.start(
                                selection = selectedSnapshot,
                                profile = profileSnapshot,
                                formatBytes = ::formatBytes,
                            )
                            val startedAtMillis = SystemClock.elapsedRealtime()
                            compressionRuntimeJob = compressionScope.launch {
                                try {
                                    val results = largeVideoCompressor.compress(
                                        request = LargeVideoCompressionRequest(
                                            videos = selectedSnapshot.selectedItems,
                                            profile = profileSnapshot,
                                        ),
                                        onProgress = { progress ->
                                            compressionJobState = compressionJobState.updateProgress(
                                                completedItems = progress.completedCount,
                                                activeItemProgress = progress.activeItemProgress,
                                            )
                                        },
                                    )
                                    compressionJobState = compressionJobState.completeWithResults(
                                        results = results,
                                        formatBytes = ::formatBytes,
                                    )
                                    completedCompressionResults = results
                                    onCompressionCompleted(
                                        profileSnapshot,
                                        SystemClock.elapsedRealtime() - startedAtMillis,
                                        results,
                                    )
                                } catch (exception: CancellationException) {
                                    // The cancel button updates the visible state immediately.
                                } catch (exception: Exception) {
                                    compressionJobState = compressionJobState.fail(
                                        exception.message ?: "The video encoder could not create a smaller copy.",
                                    )
                                }
                            }
                        },
                        onCancel = {
                            compressionRuntimeJob?.cancel()
                            completedCompressionResults = emptyList()
                            compressionJobState = compressionJobState.cancel()
                        },
                        enabled = selectionState.selectedCount > 0,
                        access = compressionAccessState,
                    )
                    if (completedCompressionResults.isNotEmpty()) {
                        LargeVideoCompressionOutputAuditPanel(
                            state = outputAuditState,
                            openAction = openCompressedCopyActionState,
                            onOpenCompressedCopy = { outputUri ->
                                val opened = context.openLargeVideoCompressedCopy(outputUri)
                                completedCompressionResults.firstOrNull { result ->
                                    result.outputPath == outputUri
                                }?.let { result ->
                                    onCompressedCopyOpenRequested(result, opened)
                                }
                                if (!opened) {
                                    Toast.makeText(
                                        context,
                                        "No video app could open this compressed copy.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    }
                    LargeVideoSourceCleanupPanel(sourceCleanupState)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            enabled = !compressionRunning && selectionState.selectedCount < videos.size,
                            onClick = {
                                selectionState = selectionState.selectAll()
                                compressionJobState = LargeVideoCompressionJobState(profile = compressionProfile)
                                completedCompressionResults = emptyList()
                                onSelectionChanged(LargeVideoSelectionAction.SelectAll, selectionState)
                            },
                        ) {
                            Text("Select all")
                        }
                        TextButton(
                            enabled = !compressionRunning && selectionState.selectedCount > 0,
                            onClick = {
                                selectionState = selectionState.clearSelection()
                                compressionJobState = LargeVideoCompressionJobState(profile = compressionProfile)
                                completedCompressionResults = emptyList()
                                onSelectionChanged(LargeVideoSelectionAction.Clear, selectionState)
                            },
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                PremiumSurface {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "No large videos found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Videos that use the most storage will appear here after the media scan finishes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                rows.forEach { row ->
                    LargeVideoReviewRow(
                        row = row,
                        enabled = !compressionRunning,
                        onToggle = {
                            selectionState = selectionState.toggle(row.id)
                            compressionJobState = LargeVideoCompressionJobState(profile = compressionProfile)
                            completedCompressionResults = emptyList()
                            onSelectionChanged(LargeVideoSelectionAction.Toggle, selectionState)
                        },
                    )
                }
            }
        }
        LargeVideoReviewBottomActionBar(
            state = bottomActionState,
            onBack = onBack,
            onPrimaryAction = { onContinue(selectionState) },
        )
    }
}

@Composable
private fun LargeVideoReviewBottomActionBar(
    state: LargeVideoReviewBottomActionState,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                ) {
                    Text("Back")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = state.primaryEnabled,
                    onClick = onPrimaryAction,
                ) {
                    Text(state.primaryLabel)
                }
            }
            BottomSummaryBar(text = state.trustMessage)
        }
    }
}

@Composable
private fun LargeVideoPostDeleteStatusPanel(status: LargeVideoPostDeleteStatus) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (status.confirmedFreed) {
            Color(0xFFECFDF5)
        } else {
            Color(0xFFFFFBEB)
        },
        border = BorderStroke(
            1.dp,
            if (status.confirmedFreed) Color(0xFF10B981) else Color(0xFFF59E0B),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LargeVideoPostDeleteMetric(
                    label = "Requested",
                    value = status.requestedLabel,
                    modifier = Modifier.weight(1f),
                )
                LargeVideoPostDeleteMetric(
                    label = "Freed",
                    value = status.freedLabel,
                    modifier = Modifier.weight(1f),
                )
                LargeVideoPostDeleteMetric(
                    label = "Check",
                    value = status.remainingLabel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LargeVideoPostDeleteMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LargeVideoCompressionOutputAuditPanel(
    state: LargeVideoCompressionOutputAuditState,
    openAction: LargeVideoCompressedCopyOpenActionState,
    onOpenCompressedCopy: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (state.canTrustSourceDelete) {
            Color(0xFFECFDF5)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f)
        },
        border = BorderStroke(
            1.dp,
            if (state.canTrustSourceDelete) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state.guidance,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = openAction.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = openAction.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        enabled = openAction.enabled && openAction.outputUri != null,
                        onClick = {
                            openAction.outputUri?.let(onOpenCompressedCopy)
                        },
                    ) {
                        Text(openAction.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeVideoSourceCleanupPanel(state: LargeVideoSourceCleanupState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (state.canRequestSourceDelete) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (state.canRequestSourceDelete) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LargeVideoCompressionProfileRow(
    preview: LargeVideoCompressionProfilePreviewState,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (preview.selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(14.dp),
        border = if (preview.selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = preview.selected,
                enabled = enabled,
                onClick = onClick,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preview.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = preview.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = preview.estimatedCompressedLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = preview.estimatedSavingsLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LargeVideoCompressionAccessPanel(state: LargeVideoCompressionAccessState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (state.requiresUnlock) {
            Color(0xFFFFFBEB)
        } else {
            Color(0xFFECFDF5)
        },
        border = BorderStroke(
            1.dp,
            if (state.requiresUnlock) Color(0xFFF59E0B) else Color(0xFF10B981),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (state.requiresUnlock) Color(0xFFF59E0B) else Color(0xFF10B981),
            ) {
                Text(
                    text = state.badgeLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = state.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LargeVideoCompressionJobPanel(
    state: LargeVideoCompressionJobState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean,
    access: LargeVideoCompressionAccessState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.phase == LargeVideoCompressionJobPhase.Running || state.phase == LargeVideoCompressionJobPhase.Completed) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = enabled && state.canStart && access.canStartCompression,
                    onClick = onStart,
                ) {
                    Text("Start compression")
                }
                TextButton(
                    enabled = state.canCancel,
                    onClick = onCancel,
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun LargeVideoReviewRow(
    row: LargeVideoReviewRowState,
    enabled: Boolean = true,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onToggle),
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
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEAF5F3)),
                contentAlignment = Alignment.Center,
            ) {
                if (row.contentUri != null) {
                    AsyncImage(
                        model = row.contentUri,
                        contentDescription = row.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color(0xFF0F766E))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = row.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.metric, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Checkbox(
                    checked = row.selected,
                    enabled = enabled,
                    onCheckedChange = { onToggle() },
                )
            }
        }
    }
}

@Composable
private fun SettingsTabScreen(
    analyticsEnabled: Boolean,
    mediaPermissionState: MediaPermissionState,
    analyticsDiagnosticsEvents: List<CleanerTelemetryEvent>,
    onAnalyticsEnabledChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    ScreenColumn(contentPadding = contentPadding) {
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        TopHeader(title = "Settings", action = "v0.1")
        PremiumSurface {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Trust-first cleanup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("No fake booster claims. No deletion without confirmation.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        PremiumSurface {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Product analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Share anonymous usage signals to improve scan speed and cleanup clarity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = analyticsEnabled,
                    onCheckedChange = onAnalyticsEnabledChange,
                )
            }
        }
        if (BuildConfig.DEBUG) {
            val diagnosticsSummary = analyticsDiagnosticsEvents.toAnalyticsDiagnosticsSummary()
            val diagnosticsReport = analyticsDiagnosticsEvents.toAnalyticsDiagnosticsReport(
                analyticsEnabled = analyticsEnabled,
                packageName = context.packageName,
                mediaAccessDiagnosticsLabels = mediaPermissionState.toMediaAccessDiagnosticsLabels(),
            )
            val deliveryDiagnosticsLabels = analyticsDiagnosticsEvents.toAnalyticsDeliveryDiagnosticsLabels(
                analyticsEnabled = analyticsEnabled,
                packageName = context.packageName,
            )
            val operationalDiagnosticsLabels = analyticsDiagnosticsEvents.toAnalyticsOperationalDiagnosticsLabels()
            val diagnosticsShareContent = analyticsDiagnosticsEvents.toAnalyticsDiagnosticsShareContent(
                analyticsEnabled = analyticsEnabled,
                packageName = context.packageName,
                mediaAccessDiagnosticsLabels = mediaPermissionState.toMediaAccessDiagnosticsLabels(),
            )
            PremiumSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Analytics diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = analyticsDiagnosticsStatusLabel(
                            analyticsEnabled = analyticsEnabled,
                            localEventCount = analyticsDiagnosticsEvents.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = diagnosticsSummary.similarFunnelProgressLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = diagnosticsSummary.similarFunnelNextStepLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = diagnosticsSummary.latestEventLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    mediaPermissionState.toMediaAccessDiagnosticsLabels().forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    operationalDiagnosticsLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    deliveryDiagnosticsLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(diagnosticsReport))
                        },
                    ) {
                        Text("Copy diagnostics")
                    }
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, diagnosticsShareContent.title)
                                .putExtra(Intent.EXTRA_TEXT, diagnosticsShareContent.text)
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, diagnosticsShareContent.title))
                            } catch (_: ActivityNotFoundException) {
                                clipboardManager.setText(AnnotatedString(diagnosticsShareContent.text))
                                Toast.makeText(
                                    context,
                                    diagnosticsShareContent.unavailableMessage,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    ) {
                        Text("Share diagnostics")
                    }
                    if (analyticsDiagnosticsEvents.isEmpty()) {
                        Text(
                            text = "No local analytics events recorded in this session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        analyticsDiagnosticsEvents.forEach { event ->
                            Text(
                                text = "${event.name} | ${event.properties.toDiagnosticsLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
private fun LimitedLibraryAccessNotice(
    mediaPermissionState: MediaPermissionState,
    onRequestPermission: () -> Unit,
) {
    val title = mediaPermissionState.accessNoticeTitle ?: return
    val message = mediaPermissionState.accessNoticeMessage ?: return
    PremiumSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestPermission,
            ) {
                Text("Allow full access")
            }
        }
    }
}

@Composable
private fun List<DuplicateGroup>?.toSimilarScreenshotMetricLabel(): String {
    return when {
        this == null -> stringResource(com.air.cleaner.feature.dashboard.R.string.dashboard_summary_scanning)
        isEmpty() -> "0 found"
        else -> formatBytes(similarScreenshotRecoverableBytes())
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
private fun MediaScanSummary.toCleanupCategories(
    duplicatePhotoGroups: List<DuplicateGroup>?,
    similarPhotoGroups: List<DuplicateGroup>?,
    similarScreenshotGroups: List<DuplicateGroup>?,
): List<CleanupCategory> {
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
            recoverableLabel = similarPhotoGroups.toDuplicateMetricLabel(),
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
            recoverableLabel = duplicatePhotoGroups.toDuplicateMetricLabel(),
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

private fun List<DuplicateGroup>.priorityGroupCountForSimilarScreenshots(): Int {
    return toSimilarScreenshotReviewWorkflow(
        keepStrategy = PhotoReviewKeepStrategy.Newest,
        filter = SimilarScreenshotReviewFilter.All,
    ).needsReviewGroups
}

private fun Map<String, Any>.toDiagnosticsLabel(): String {
    return entries.joinToString(separator = ", ") { (key, value) -> "$key=$value" }
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

private fun android.content.Context.openLargeVideoCompressedCopy(outputUri: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(outputUri), "video/mp4")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
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
            scanStatus = MediaScanStatus(
                phase = MediaScanPhase.Complete,
                summary = MediaScanSummary(
                    imageCount = 8_240,
                    videoCount = 420,
                    imageBytes = 3_400_000_000L,
                    videoBytes = 5_200_000_000L,
                    screenshotCount = 820,
                    screenshotBytes = 820_000_000L,
                ),
            ),
            mediaPermissionState = MediaPermissionState(
                sdkInt = 35,
                readImagesGranted = true,
                readVideoGranted = true,
                visualUserSelectedGranted = false,
                legacyReadGranted = false,
            ),
            largeVideos = emptyList(),
            duplicatePhotoGroups = emptyList(),
            similarPhotoGroups = emptyList(),
            similarScreenshotGroups = emptyList(),
            similarScreenshotReviewStatus = SimilarScreenshotReviewStatus.Fresh,
            telemetry = NoOpCleanerTelemetry,
            analyticsEnabled = false,
            analyticsDiagnosticsEvents = emptyList(),
            onTabSelected = {},
            onOpenDuplicatePhotos = {},
            onOpenSimilarPhotos = {},
            onOpenLargeVideos = {},
            onOpenSimilarScreenshots = {},
            onRequestPermission = {},
            onRescanSimilarScreenshots = {},
            onBackToPhotos = {},
            onBackToVideos = {},
            onPostDeleteAction = {},
            pendingDeleteSummary = null,
            pendingDeleteReviewContext = null,
            deleteResult = null,
            deleteResultReviewContext = null,
            postDeleteStatus = null,
            largeVideoPostDeleteStatus = null,
            postDeleteReviewContext = null,
            onRequestDeleteConfirmation = { _, _ -> },
            onDismissDeleteConfirmation = {},
            onConfirmDelete = {},
            onDismissDeleteResult = {},
            onAnalyticsEnabledChange = {},
        )
    }
}
