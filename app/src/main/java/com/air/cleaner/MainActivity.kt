package com.air.cleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.air.cleaner.ui.AIPhotoCleanerApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val debugSimilarPhotoRelativePathPrefix = if (BuildConfig.DEBUG) {
            intent.getStringExtra(DEBUG_SIMILAR_PHOTO_RELATIVE_PATH_PREFIX_EXTRA)
        } else {
            null
        }
        setContent {
            AIPhotoCleanerApp(
                debugSimilarPhotoRelativePathPrefix = debugSimilarPhotoRelativePathPrefix,
            )
        }
    }

    companion object {
        const val DEBUG_SIMILAR_PHOTO_RELATIVE_PATH_PREFIX_EXTRA =
            "debug_similar_photo_relative_path_prefix"
    }
}
