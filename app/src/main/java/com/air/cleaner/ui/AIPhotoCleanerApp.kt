package com.air.cleaner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.air.cleaner.core.ui.theme.CleanerTheme

@Composable
fun AIPhotoCleanerApp() {
    CleanerTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Free up space safely",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Find duplicate photos, blurry shots, screenshots, and large videos. Nothing is deleted without your confirmation.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Button(onClick = {}) {
                Text("Scan my photos")
            }
        }
    }
}
