# AI Photo Cleaner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first Android MVP of `AI Photo Cleaner: Clean Storage`, with localized UI foundations, safe media scanning, photo/video cleanup flows, monetization hooks, analytics, and Figma-ready product design.

**Architecture:** Create a native Android Kotlin project with a modular Compose architecture. Keep UI, domain logic, Android data access, monetization, ads, analytics, and permissions separated so the app can become a matrix product later without copying tangled code.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, Jetpack Compose, Material 3, Coroutines, Flow, Room, DataStore, Hilt, Coil, Google Play Billing, Google Mobile Ads SDK, Firebase Analytics, Firebase Crashlytics, Firebase Remote Config, Android MediaStore, Android Contacts Provider.

---

## Source Specs

- Product spec: `docs/superpowers/specs/2026-05-03-ai-photo-cleaner-design.md`
- Chinese spec: `docs/superpowers/specs/2026-05-03-ai-photo-cleaner-design-zh.md`

## File Structure

Create a new Android project under:

- `ai-photo-cleaner/`

Planned modules:

- `ai-photo-cleaner/settings.gradle.kts`: Gradle module registry.
- `ai-photo-cleaner/build.gradle.kts`: root plugin and dependency version setup.
- `ai-photo-cleaner/app/`: Android app entry point, navigation shell, app theme wiring.
- `ai-photo-cleaner/core/ui/`: shared Compose components, theme, spacing, icons, preview helpers.
- `ai-photo-cleaner/core/i18n/`: locale metadata, supported locale list, region design profile helpers.
- `ai-photo-cleaner/core/permissions/`: media and contacts permission state logic.
- `ai-photo-cleaner/core/analytics/`: typed analytics event interface and Firebase implementation.
- `ai-photo-cleaner/core/billing/`: Google Play Billing wrapper and premium state.
- `ai-photo-cleaner/core/ads/`: rewarded ad and interstitial policy wrapper.
- `ai-photo-cleaner/data/media/`: MediaStore repository, scan cache entities, image/video metadata.
- `ai-photo-cleaner/data/contacts/`: Contacts provider repository and duplicate contact models.
- `ai-photo-cleaner/domain/cleaning/`: duplicate detection, perceptual hash, blur scoring, video size sorting, recommendation rules.
- `ai-photo-cleaner/feature/onboarding/`: first launch, trust, permission, and region-aware copy.
- `ai-photo-cleaner/feature/scan/`: scan progress state and scan orchestration UI.
- `ai-photo-cleaner/feature/dashboard/`: cleanup category dashboard.
- `ai-photo-cleaner/feature/photos/`: duplicate, similar, blurry, and screenshot review flows.
- `ai-photo-cleaner/feature/videos/`: large video and compression estimate flows.
- `ai-photo-cleaner/feature/paywall/`: subscription paywall and rewarded unlock prompts.
- `ai-photo-cleaner/feature/settings/`: settings, subscription management, privacy and language info.
- `ai-photo-cleaner/docs/figma/`: Figma brief, screen checklist, and regional design adaptation notes.

## Task 1: Create Android Project Skeleton

**Files:**
- Create: `ai-photo-cleaner/settings.gradle.kts`
- Create: `ai-photo-cleaner/build.gradle.kts`
- Create: `ai-photo-cleaner/gradle/libs.versions.toml`
- Create: `ai-photo-cleaner/app/build.gradle.kts`
- Create: `ai-photo-cleaner/app/src/main/AndroidManifest.xml`
- Create: `ai-photo-cleaner/app/src/main/java/com/air/cleaner/App.kt`
- Create: `ai-photo-cleaner/app/src/main/java/com/air/cleaner/MainActivity.kt`

- [ ] **Step 1: Generate the Android project shell**

Create `ai-photo-cleaner/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AIPhotoCleaner"

include(":app")
include(":core:ui")
include(":core:i18n")
include(":core:permissions")
include(":core:analytics")
include(":core:billing")
include(":core:ads")
include(":data:media")
include(":data:contacts")
include(":domain:cleaning")
include(":feature:onboarding")
include(":feature:scan")
include(":feature:dashboard")
include(":feature:photos")
include(":feature:videos")
include(":feature:paywall")
include(":feature:settings")
```

Create `ai-photo-cleaner/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
```

- [ ] **Step 2: Add dependency versions**

Create `ai-photo-cleaner/gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.8.2"
kotlin = "2.1.10"
ksp = "2.1.10-1.0.31"
composeBom = "2025.02.00"
activityCompose = "1.10.1"
androidxCore = "1.15.0"
lifecycle = "2.8.7"
navigationCompose = "2.8.7"
hilt = "2.55"
hiltNavigationCompose = "1.2.0"
coroutines = "1.10.1"
room = "2.6.1"
datastore = "1.1.2"
coil = "2.7.0"
billing = "7.1.1"
ads = "23.6.0"
firebaseBom = "33.9.0"
junit = "4.13.2"
androidxJunit = "1.2.1"
espresso = "3.6.1"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version = "3.0.2" }

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidxCore" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
billing-ktx = { module = "com.android.billingclient:billing-ktx", version.ref = "billing" }
play-services-ads = { module = "com.google.android.gms:play-services-ads", version.ref = "ads" }
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }
firebase-config = { module = "com.google.firebase:firebase-config" }
junit = { module = "junit:junit", version.ref = "junit" }
androidx-junit = { module = "androidx.test.ext:junit", version.ref = "androidxJunit" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
```

- [ ] **Step 3: Configure the app module**

Create `ai-photo-cleaner/app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.air.cleaner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.air.cleaner"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:scan"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:photos"))
    implementation(project(":feature:videos"))
    implementation(project(":feature:paywall"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

- [ ] **Step 4: Add app manifest and entry points**

Create `ai-photo-cleaner/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />

    <application
        android:name=".App"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AIPhotoCleaner">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Create `ai-photo-cleaner/app/src/main/java/com/air/cleaner/App.kt`:

```kotlin
package com.air.cleaner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application()
```

Create `ai-photo-cleaner/app/src/main/java/com/air/cleaner/MainActivity.kt`:

```kotlin
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
        setContent {
            AIPhotoCleanerApp()
        }
    }
}
```

- [ ] **Step 5: Build check**

Run:

```powershell
.\gradlew :app:assembleDebug
```

Expected: Gradle reaches dependency resolution and then fails only if generated resources such as launcher icons or XML backup rules have not been created yet.

- [ ] **Step 6: Commit**

```powershell
git add ai-photo-cleaner
git commit -m "chore: create android app skeleton"
```

## Task 2: Add Shared UI Theme And Regional Design Profiles

**Files:**
- Create: `ai-photo-cleaner/core/ui/build.gradle.kts`
- Create: `ai-photo-cleaner/core/ui/src/main/java/com/air/cleaner/core/ui/theme/CleanerTheme.kt`
- Create: `ai-photo-cleaner/core/ui/src/main/java/com/air/cleaner/core/ui/components/ActionCard.kt`
- Create: `ai-photo-cleaner/core/i18n/build.gradle.kts`
- Create: `ai-photo-cleaner/core/i18n/src/main/java/com/air/cleaner/core/i18n/SupportedLocale.kt`
- Create: `ai-photo-cleaner/core/i18n/src/main/java/com/air/cleaner/core/i18n/RegionalDesignProfile.kt`
- Create: `ai-photo-cleaner/core/i18n/src/test/java/com/air/cleaner/core/i18n/RegionalDesignProfileTest.kt`

- [ ] **Step 1: Write regional design profile tests**

Create `ai-photo-cleaner/core/i18n/src/test/java/com/air/cleaner/core/i18n/RegionalDesignProfileTest.kt`:

```kotlin
package com.air.cleaner.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalDesignProfileTest {
    @Test
    fun arabicUsesRtlAndConservativeDensity() {
        val profile = RegionalDesignProfile.forLanguageTag("ar")
        assertEquals(LayoutDirectionPreference.Rtl, profile.layoutDirection)
        assertEquals(VisualDensity.Comfortable, profile.visualDensity)
        assertTrue(profile.trustEmphasis.contains(TrustCue.UserControl))
    }

    @Test
    fun germanUsesPrivacyFirstTone() {
        val profile = RegionalDesignProfile.forLanguageTag("de")
        assertEquals(CopyTone.PrivacyFirst, profile.copyTone)
        assertTrue(profile.trustEmphasis.contains(TrustCue.SubscriptionClarity))
    }

    @Test
    fun japanUsesCompactHighTrustTone() {
        val profile = RegionalDesignProfile.forLanguageTag("ja")
        assertEquals(VisualDensity.Compact, profile.visualDensity)
        assertEquals(CopyTone.Precise, profile.copyTone)
    }
}
```

- [ ] **Step 2: Implement locale metadata**

Create `ai-photo-cleaner/core/i18n/src/main/java/com/air/cleaner/core/i18n/SupportedLocale.kt`:

```kotlin
package com.air.cleaner.core.i18n

data class SupportedLocale(
    val languageTag: String,
    val displayName: String,
    val storePriority: StorePriority,
)

enum class StorePriority {
    Launch,
    Expansion,
}

val supportedLocales = listOf(
    SupportedLocale("en-US", "English", StorePriority.Launch),
    SupportedLocale("zh-CN", "简体中文", StorePriority.Launch),
    SupportedLocale("es", "Español", StorePriority.Launch),
    SupportedLocale("pt-BR", "Português (Brasil)", StorePriority.Launch),
    SupportedLocale("ru", "Русский", StorePriority.Launch),
    SupportedLocale("ja", "日本語", StorePriority.Launch),
    SupportedLocale("ko", "한국어", StorePriority.Launch),
    SupportedLocale("de", "Deutsch", StorePriority.Launch),
    SupportedLocale("fr", "Français", StorePriority.Launch),
    SupportedLocale("ar", "العربية", StorePriority.Launch),
    SupportedLocale("id", "Bahasa Indonesia", StorePriority.Expansion),
    SupportedLocale("th", "ไทย", StorePriority.Expansion),
    SupportedLocale("vi", "Tiếng Việt", StorePriority.Expansion),
    SupportedLocale("tr", "Türkçe", StorePriority.Expansion),
    SupportedLocale("it", "Italiano", StorePriority.Expansion),
    SupportedLocale("el", "Ελληνικά", StorePriority.Expansion),
    SupportedLocale("pl", "Polski", StorePriority.Expansion),
    SupportedLocale("nl", "Nederlands", StorePriority.Expansion),
    SupportedLocale("sv", "Svenska", StorePriority.Expansion),
    SupportedLocale("hi", "हिन्दी", StorePriority.Expansion),
)
```

- [ ] **Step 3: Implement regional design profile logic**

Create `ai-photo-cleaner/core/i18n/src/main/java/com/air/cleaner/core/i18n/RegionalDesignProfile.kt`:

```kotlin
package com.air.cleaner.core.i18n

data class RegionalDesignProfile(
    val layoutDirection: LayoutDirectionPreference,
    val visualDensity: VisualDensity,
    val copyTone: CopyTone,
    val trustEmphasis: Set<TrustCue>,
) {
    companion object {
        fun forLanguageTag(languageTag: String): RegionalDesignProfile {
            val language = languageTag.substringBefore("-")
            return when (language) {
                "ar" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Rtl,
                    visualDensity = VisualDensity.Comfortable,
                    copyTone = CopyTone.Reassuring,
                    trustEmphasis = setOf(TrustCue.UserControl, TrustCue.CulturalNeutrality),
                )
                "de", "fr", "nl", "sv" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Standard,
                    copyTone = CopyTone.PrivacyFirst,
                    trustEmphasis = setOf(TrustCue.Privacy, TrustCue.SubscriptionClarity),
                )
                "ja", "ko" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Compact,
                    copyTone = CopyTone.Precise,
                    trustEmphasis = setOf(TrustCue.UserControl, TrustCue.Accuracy),
                )
                "es", "pt" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Standard,
                    copyTone = CopyTone.BenefitForward,
                    trustEmphasis = setOf(TrustCue.ValueForMoney, TrustCue.VisibleResult),
                )
                "hi", "id", "vi", "th", "tr", "pl", "ru", "el", "it" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Comfortable,
                    copyTone = CopyTone.ResultFirst,
                    trustEmphasis = setOf(TrustCue.VisibleResult, TrustCue.FreePreview),
                )
                else -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Standard,
                    copyTone = CopyTone.Direct,
                    trustEmphasis = setOf(TrustCue.Privacy, TrustCue.UserControl),
                )
            }
        }
    }
}

enum class LayoutDirectionPreference { Ltr, Rtl }
enum class VisualDensity { Compact, Standard, Comfortable }
enum class CopyTone { Direct, PrivacyFirst, Precise, BenefitForward, ResultFirst, Reassuring }
enum class TrustCue { Privacy, UserControl, SubscriptionClarity, CulturalNeutrality, Accuracy, ValueForMoney, VisibleResult, FreePreview }
```

- [ ] **Step 4: Add the UI theme**

Create `ai-photo-cleaner/core/ui/src/main/java/com/air/cleaner/core/ui/theme/CleanerTheme.kt`:

```kotlin
package com.air.cleaner.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanerColorScheme = lightColorScheme(
    primary = Color(0xFF176B87),
    onPrimary = Color.White,
    secondary = Color(0xFF4C6F64),
    onSecondary = Color.White,
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF14211F),
    surface = Color.White,
    onSurface = Color(0xFF14211F),
    error = Color(0xFFB42318),
    onError = Color.White,
)

@Composable
fun CleanerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CleanerColorScheme,
        content = content,
    )
}
```

- [ ] **Step 5: Add a reusable action card**

Create `ai-photo-cleaner/core/ui/src/main/java/com/air/cleaner/core/ui/components/ActionCard.kt`:

```kotlin
package com.air.cleaner.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    metric: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                Text(text = metric, style = MaterialTheme.typography.labelLarge)
            }
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
```

- [ ] **Step 6: Run tests**

Run:

```powershell
.\gradlew :core:i18n:testDebugUnitTest
```

Expected: regional design profile tests pass.

- [ ] **Step 7: Commit**

```powershell
git add ai-photo-cleaner/core
git commit -m "feat: add shared theme and regional locale profiles"
```

## Task 3: Create Figma Product Design Brief

**Files:**
- Create: `ai-photo-cleaner/docs/figma/mobile-flow-brief.md`
- Create: `ai-photo-cleaner/docs/figma/regional-design-checklist.md`
- Create: `ai-photo-cleaner/docs/figma/screen-copy-matrix.md`

- [ ] **Step 1: Write the Figma mobile flow brief**

Create `ai-photo-cleaner/docs/figma/mobile-flow-brief.md`:

```markdown
# AI Photo Cleaner Figma Mobile Flow Brief

## Goal

Design the first Android mobile flow for `AI Photo Cleaner: Clean Storage`.

## Required Screens

1. First launch and permission trust screen
2. Scan progress screen
3. Dashboard with cleanup category cards
4. Duplicate photo group review
5. Similar photo smart selection review
6. Large video review and compression estimate
7. Delete confirmation bottom sheet
8. Paywall
9. Rewarded ad unlock prompt
10. Settings and subscription management

## Emotional User Journey

The design should move the user from storage anxiety to safe control:

1. The phone is full.
2. The app finds concrete waste.
3. The user previews before deleting.
4. The app shows visible storage recovered.
5. Premium appears as a faster path, not a trap.

## Global Visual Direction

- Trustworthy utility app, not a playful consumer social app.
- No alarmist red except destructive confirmation.
- Use sample photo/video preview tiles, not abstract decoration.
- Put storage recovered and next action above decorative content.
- Paywall appears only after the scan value is visible.
```

- [ ] **Step 2: Write the regional design checklist**

Create `ai-photo-cleaner/docs/figma/regional-design-checklist.md`:

```markdown
# Regional Design Checklist

## en-US

- Direct value statement.
- Clear privacy reassurance.
- Dashboard shows recoverable storage first.
- Paywall copy is restrained and transparent.

## de

- Test long text in buttons, cards, and paywall.
- Permission copy should emphasize privacy and user control.
- Subscription cancellation wording must be visible.
- Avoid high-pressure visual treatment.

## ar

- Full RTL layout.
- Mirrored navigation and progress direction.
- Larger tap targets for expanded text.
- Culturally neutral media examples.

## ja

- Compact layout.
- Precise microcopy.
- Avoid exaggerated emotional claims.
- Emphasize safe review before deletion.

## hi

- Test text expansion in dashboard cards.
- Keep onboarding fast.
- Show free scan result before paywall.
- Ensure low-end device readability.
```

- [ ] **Step 3: Write the screen copy matrix**

Create `ai-photo-cleaner/docs/figma/screen-copy-matrix.md`:

```markdown
# Screen Copy Matrix

## Permission Trust Screen

en-US: Find photos and videos that waste storage. Nothing is deleted without your confirmation.
de: Finden Sie Fotos und Videos, die Speicher belegen. Ohne Ihre Bestätigung wird nichts gelöscht.
ar: ابحث عن الصور ومقاطع الفيديو التي تستهلك المساحة. لن يتم حذف أي شيء بدون تأكيدك.
ja: ストレージを圧迫している写真や動画を見つけます。確認なしに削除されることはありません。
hi: स्टोरेज घेरने वाली फ़ोटो और वीडियो ढूंढें। आपकी पुष्टि के बिना कुछ भी हटाया नहीं जाएगा।

## Scan Result Headline

en-US: You can free up {space}
de: Sie können {space} freigeben
ar: يمكنك توفير {space}
ja: {space} を空けられます
hi: आप {space} खाली कर सकते हैं

## Paywall Headline

en-US: Clean faster with unlimited smart cleanup
de: Schneller bereinigen mit unbegrenzter intelligenter Reinigung
ar: نظّف أسرع باستخدام التنظيف الذكي غير المحدود
ja: 無制限のスマート整理でより速くクリーンアップ
hi: अनलिमिटेड स्मार्ट क्लीनअप से तेज़ी से साफ़ करें
```

- [ ] **Step 4: Figma execution note**

When a Figma file is available, use `figma:figma-use` before any `use_figma` call and `figma:figma-generate-design` for full mobile screens. Build a single mobile flow board first, then duplicate representative screens for `en-US`, `de`, `ar`, `ja`, and `hi`.

- [ ] **Step 5: Commit**

```powershell
git add ai-photo-cleaner/docs/figma
git commit -m "docs: add figma mobile flow brief"
```

## Task 4: Implement Onboarding And Permission Flow

**Files:**
- Create: `ai-photo-cleaner/feature/onboarding/build.gradle.kts`
- Create: `ai-photo-cleaner/feature/onboarding/src/main/java/com/air/cleaner/feature/onboarding/OnboardingScreen.kt`
- Create: `ai-photo-cleaner/core/permissions/build.gradle.kts`
- Create: `ai-photo-cleaner/core/permissions/src/main/java/com/air/cleaner/core/permissions/MediaPermissionState.kt`
- Create: `ai-photo-cleaner/core/permissions/src/test/java/com/air/cleaner/core/permissions/MediaPermissionStateTest.kt`

- [ ] **Step 1: Write permission state tests**

Create `ai-photo-cleaner/core/permissions/src/test/java/com/air/cleaner/core/permissions/MediaPermissionStateTest.kt`:

```kotlin
package com.air.cleaner.core.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionStateTest {
    @Test
    fun android13RequiresImageAndVideoPermissions() {
        val state = MediaPermissionState(
            sdkInt = 33,
            readImagesGranted = true,
            readVideoGranted = false,
            legacyReadGranted = false,
        )

        assertFalse(state.canScanMedia)
    }

    @Test
    fun legacyAndroidUsesReadExternalStorage() {
        val state = MediaPermissionState(
            sdkInt = 32,
            readImagesGranted = false,
            readVideoGranted = false,
            legacyReadGranted = true,
        )

        assertTrue(state.canScanMedia)
    }
}
```

- [ ] **Step 2: Implement permission state**

Create `ai-photo-cleaner/core/permissions/src/main/java/com/air/cleaner/core/permissions/MediaPermissionState.kt`:

```kotlin
package com.air.cleaner.core.permissions

data class MediaPermissionState(
    val sdkInt: Int,
    val readImagesGranted: Boolean,
    val readVideoGranted: Boolean,
    val legacyReadGranted: Boolean,
) {
    val canScanMedia: Boolean
        get() = if (sdkInt >= 33) {
            readImagesGranted && readVideoGranted
        } else {
            legacyReadGranted
        }
}
```

- [ ] **Step 3: Implement onboarding screen**

Create `ai-photo-cleaner/feature/onboarding/src/main/java/com/air/cleaner/feature/onboarding/OnboardingScreen.kt`:

```kotlin
package com.air.cleaner.feature.onboarding

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

@Composable
fun OnboardingScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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

        Button(onClick = onRequestPermission) {
            Text("Scan my photos")
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew :core:permissions:testDebugUnitTest
```

Expected: permission state tests pass.

- [ ] **Step 5: Commit**

```powershell
git add ai-photo-cleaner/core/permissions ai-photo-cleaner/feature/onboarding
git commit -m "feat: add onboarding permission flow"
```

## Task 5: Implement Media Scan Domain Models And Duplicate Detection

**Files:**
- Create: `ai-photo-cleaner/domain/cleaning/build.gradle.kts`
- Create: `ai-photo-cleaner/domain/cleaning/src/main/java/com/air/cleaner/domain/cleaning/MediaItem.kt`
- Create: `ai-photo-cleaner/domain/cleaning/src/main/java/com/air/cleaner/domain/cleaning/DuplicatePhotoDetector.kt`
- Create: `ai-photo-cleaner/domain/cleaning/src/test/java/com/air/cleaner/domain/cleaning/DuplicatePhotoDetectorTest.kt`

- [ ] **Step 1: Write duplicate detector tests**

Create `ai-photo-cleaner/domain/cleaning/src/test/java/com/air/cleaner/domain/cleaning/DuplicatePhotoDetectorTest.kt`:

```kotlin
package com.air.cleaner.domain.cleaning

import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicatePhotoDetectorTest {
    @Test
    fun groupsItemsWithSameContentHash() {
        val detector = DuplicatePhotoDetector()
        val items = listOf(
            MediaItem("1", "a.jpg", 100, 1_000, "hash-a"),
            MediaItem("2", "b.jpg", 120, 1_100, "hash-a"),
            MediaItem("3", "c.jpg", 130, 1_200, "hash-b"),
        )

        val groups = detector.findDuplicates(items)

        assertEquals(1, groups.size)
        assertEquals(listOf("1", "2"), groups.single().items.map { it.id })
    }
}
```

- [ ] **Step 2: Implement media models**

Create `ai-photo-cleaner/domain/cleaning/src/main/java/com/air/cleaner/domain/cleaning/MediaItem.kt`:

```kotlin
package com.air.cleaner.domain.cleaning

data class MediaItem(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val contentHash: String?,
)

data class DuplicateGroup(
    val key: String,
    val items: List<MediaItem>,
) {
    val recoverableBytes: Long = items.drop(1).sumOf { it.sizeBytes }
}
```

- [ ] **Step 3: Implement duplicate detector**

Create `ai-photo-cleaner/domain/cleaning/src/main/java/com/air/cleaner/domain/cleaning/DuplicatePhotoDetector.kt`:

```kotlin
package com.air.cleaner.domain.cleaning

class DuplicatePhotoDetector {
    fun findDuplicates(items: List<MediaItem>): List<DuplicateGroup> {
        return items
            .asSequence()
            .filter { it.contentHash != null }
            .groupBy { it.contentHash.orEmpty() }
            .filterValues { it.size > 1 }
            .map { (hash, groupItems) ->
                DuplicateGroup(
                    key = hash,
                    items = groupItems.sortedBy { it.dateTakenMillis },
                )
            }
            .sortedByDescending { it.recoverableBytes }
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew :domain:cleaning:testDebugUnitTest
```

Expected: duplicate detector tests pass.

- [ ] **Step 5: Commit**

```powershell
git add ai-photo-cleaner/domain/cleaning
git commit -m "feat: add duplicate photo detection"
```

## Task 6: Implement Scan Dashboard UI With Fake Data

**Files:**
- Create: `ai-photo-cleaner/feature/dashboard/build.gradle.kts`
- Create: `ai-photo-cleaner/feature/dashboard/src/main/java/com/air/cleaner/feature/dashboard/CleanupCategory.kt`
- Create: `ai-photo-cleaner/feature/dashboard/src/main/java/com/air/cleaner/feature/dashboard/DashboardScreen.kt`
- Modify: `ai-photo-cleaner/app/src/main/java/com/air/cleaner/MainActivity.kt`

- [ ] **Step 1: Add category model**

Create `ai-photo-cleaner/feature/dashboard/src/main/java/com/air/cleaner/feature/dashboard/CleanupCategory.kt`:

```kotlin
package com.air.cleaner.feature.dashboard

data class CleanupCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val recoverableLabel: String,
    val actionLabel: String,
)

val previewCategories = listOf(
    CleanupCategory("duplicates", "Duplicate photos", "Review exact copies", "1.8 GB", "Review"),
    CleanupCategory("similar", "Similar photos", "Pick the best shot", "3.4 GB", "Review"),
    CleanupCategory("blurry", "Blurry photos", "Check low-quality images", "640 MB", "Review"),
    CleanupCategory("videos", "Large videos", "Compress or remove big files", "5.2 GB", "Open"),
)
```

- [ ] **Step 2: Add dashboard screen**

Create `ai-photo-cleaner/feature/dashboard/src/main/java/com/air/cleaner/feature/dashboard/DashboardScreen.kt`:

```kotlin
package com.air.cleaner.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.air.cleaner.core.ui.components.ActionCard

@Composable
fun DashboardScreen(
    categories: List<CleanupCategory>,
    onCategoryClick: (CleanupCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("You can free up 11.0 GB", style = MaterialTheme.typography.headlineMedium)
        Text("Review before deleting. You are always in control.", style = MaterialTheme.typography.bodyMedium)
        categories.forEach { category ->
            ActionCard(
                title = category.title,
                subtitle = category.subtitle,
                metric = category.recoverableLabel,
                actionLabel = category.actionLabel,
                onAction = { onCategoryClick(category) },
            )
        }
    }
}
```

- [ ] **Step 3: Wire dashboard as temporary start screen**

Modify `MainActivity.kt` so `setContent` wraps `DashboardScreen` in `CleanerTheme` until navigation is implemented:

```kotlin
setContent {
    CleanerTheme {
        DashboardScreen(
            categories = previewCategories,
            onCategoryClick = {},
        )
    }
}
```

- [ ] **Step 4: Run build**

Run:

```powershell
.\gradlew :app:assembleDebug
```

Expected: debug APK builds and opens to the dashboard.

- [ ] **Step 5: Commit**

```powershell
git add ai-photo-cleaner/app ai-photo-cleaner/feature/dashboard
git commit -m "feat: add cleanup dashboard"
```

## Task 7: Add Localization Resource Baseline

**Files:**
- Create: `ai-photo-cleaner/app/src/main/res/values/strings.xml`
- Create: `ai-photo-cleaner/app/src/main/res/values-de/strings.xml`
- Create: `ai-photo-cleaner/app/src/main/res/values-ar/strings.xml`
- Create: `ai-photo-cleaner/app/src/main/res/values-ja/strings.xml`
- Create: `ai-photo-cleaner/app/src/main/res/values-hi/strings.xml`
- Create: `ai-photo-cleaner/docs/localization/locale-rollout.md`

- [ ] **Step 1: Add baseline English strings**

Create `ai-photo-cleaner/app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">AI Photo Cleaner</string>
    <string name="permission_title">Free up space safely</string>
    <string name="permission_body">Find duplicate photos, blurry shots, screenshots, and large videos. Nothing is deleted without your confirmation.</string>
    <string name="permission_cta">Scan my photos</string>
    <string name="dashboard_headline">You can free up %1$s</string>
    <string name="dashboard_subtitle">Review before deleting. You are always in control.</string>
    <string name="category_duplicate_photos">Duplicate photos</string>
    <string name="category_similar_photos">Similar photos</string>
    <string name="category_blurry_photos">Blurry photos</string>
    <string name="category_large_videos">Large videos</string>
    <string name="action_review">Review</string>
    <string name="paywall_headline">Clean faster with unlimited smart cleanup</string>
</resources>
```

- [ ] **Step 2: Add German stress-test locale**

Create `ai-photo-cleaner/app/src/main/res/values-de/strings.xml`:

```xml
<resources>
    <string name="app_name">AI Photo Cleaner</string>
    <string name="permission_title">Speicher sicher freigeben</string>
    <string name="permission_body">Finden Sie doppelte Fotos, unscharfe Aufnahmen, Screenshots und große Videos. Ohne Ihre Bestätigung wird nichts gelöscht.</string>
    <string name="permission_cta">Meine Fotos scannen</string>
    <string name="dashboard_headline">Sie können %1$s freigeben</string>
    <string name="dashboard_subtitle">Prüfen Sie alles vor dem Löschen. Sie behalten immer die Kontrolle.</string>
    <string name="category_duplicate_photos">Doppelte Fotos</string>
    <string name="category_similar_photos">Ähnliche Fotos</string>
    <string name="category_blurry_photos">Unscharfe Fotos</string>
    <string name="category_large_videos">Große Videos</string>
    <string name="action_review">Prüfen</string>
    <string name="paywall_headline">Schneller bereinigen mit unbegrenzter intelligenter Reinigung</string>
</resources>
```

- [ ] **Step 3: Add Arabic RTL stress-test locale**

Create `ai-photo-cleaner/app/src/main/res/values-ar/strings.xml`:

```xml
<resources>
    <string name="app_name">AI Photo Cleaner</string>
    <string name="permission_title">وفّر المساحة بأمان</string>
    <string name="permission_body">اعثر على الصور المكررة واللقطات غير الواضحة ولقطات الشاشة ومقاطع الفيديو الكبيرة. لن يتم حذف أي شيء بدون تأكيدك.</string>
    <string name="permission_cta">فحص صوري</string>
    <string name="dashboard_headline">يمكنك توفير %1$s</string>
    <string name="dashboard_subtitle">راجع كل شيء قبل الحذف. أنت تتحكم دائمًا.</string>
    <string name="category_duplicate_photos">صور مكررة</string>
    <string name="category_similar_photos">صور متشابهة</string>
    <string name="category_blurry_photos">صور غير واضحة</string>
    <string name="category_large_videos">مقاطع فيديو كبيرة</string>
    <string name="action_review">مراجعة</string>
    <string name="paywall_headline">نظّف أسرع باستخدام التنظيف الذكي غير المحدود</string>
</resources>
```

- [ ] **Step 4: Add Japanese stress-test locale**

Create `ai-photo-cleaner/app/src/main/res/values-ja/strings.xml`:

```xml
<resources>
    <string name="app_name">AI Photo Cleaner</string>
    <string name="permission_title">安全に空き容量を増やす</string>
    <string name="permission_body">重複写真、ぼやけた写真、スクリーンショット、大きな動画を見つけます。確認なしに削除されることはありません。</string>
    <string name="permission_cta">写真をスキャン</string>
    <string name="dashboard_headline">%1$s を空けられます</string>
    <string name="dashboard_subtitle">削除前に確認できます。操作は常にあなたが管理します。</string>
    <string name="category_duplicate_photos">重複写真</string>
    <string name="category_similar_photos">類似写真</string>
    <string name="category_blurry_photos">ぼやけた写真</string>
    <string name="category_large_videos">大きな動画</string>
    <string name="action_review">確認</string>
    <string name="paywall_headline">無制限のスマート整理でより速くクリーンアップ</string>
</resources>
```

- [ ] **Step 5: Add Hindi stress-test locale**

Create `ai-photo-cleaner/app/src/main/res/values-hi/strings.xml`:

```xml
<resources>
    <string name="app_name">AI Photo Cleaner</string>
    <string name="permission_title">सुरक्षित रूप से जगह खाली करें</string>
    <string name="permission_body">डुप्लिकेट फ़ोटो, धुंधली तस्वीरें, स्क्रीनशॉट और बड़े वीडियो ढूंढें। आपकी पुष्टि के बिना कुछ भी हटाया नहीं जाएगा।</string>
    <string name="permission_cta">मेरी फ़ोटो स्कैन करें</string>
    <string name="dashboard_headline">आप %1$s खाली कर सकते हैं</string>
    <string name="dashboard_subtitle">हटाने से पहले समीक्षा करें। नियंत्रण हमेशा आपके पास है।</string>
    <string name="category_duplicate_photos">डुप्लिकेट फ़ोटो</string>
    <string name="category_similar_photos">मिलती-जुलती फ़ोटो</string>
    <string name="category_blurry_photos">धुंधली फ़ोटो</string>
    <string name="category_large_videos">बड़े वीडियो</string>
    <string name="action_review">समीक्षा करें</string>
    <string name="paywall_headline">अनलिमिटेड स्मार्ट क्लीनअप से तेज़ी से साफ़ करें</string>
</resources>
```

- [ ] **Step 6: Document remaining locale rollout**

Create `ai-photo-cleaner/docs/localization/locale-rollout.md`:

```markdown
# Locale Rollout

## Phase 1 Stress-Test Locales

- en-US
- de
- ar
- ja
- hi

## Phase 2 Full Launch Locales

- zh-CN
- es
- pt-BR
- ru
- ko
- fr
- id
- th
- vi
- tr
- it
- el
- pl
- nl
- sv

## Validation

- No missing string resources.
- No hardcoded UI strings in Compose screens.
- Arabic layout mirrors correctly.
- German and Hindi strings do not overflow buttons or cards.
```

- [ ] **Step 7: Run lint build**

Run:

```powershell
.\gradlew :app:assembleDebug
```

Expected: string resources compile.

- [ ] **Step 8: Commit**

```powershell
git add ai-photo-cleaner/app/src/main/res ai-photo-cleaner/docs/localization
git commit -m "feat: add localization resource baseline"
```

## Task 8: Add Monetization And Analytics Interfaces

**Files:**
- Create: `ai-photo-cleaner/core/analytics/src/main/java/com/air/cleaner/core/analytics/AnalyticsEvent.kt`
- Create: `ai-photo-cleaner/core/billing/src/main/java/com/air/cleaner/core/billing/PremiumProduct.kt`
- Create: `ai-photo-cleaner/core/ads/src/main/java/com/air/cleaner/core/ads/RewardedUnlock.kt`

- [ ] **Step 1: Add analytics event contract**

Create `AnalyticsEvent.kt`:

```kotlin
package com.air.cleaner.core.analytics

sealed interface AnalyticsEvent {
    data object PermissionViewed : AnalyticsEvent
    data class PermissionResult(val granted: Boolean) : AnalyticsEvent
    data object ScanStarted : AnalyticsEvent
    data class ScanCompleted(val recoverableBytes: Long) : AnalyticsEvent
    data class CategoryOpened(val categoryId: String) : AnalyticsEvent
    data class PaywallShown(val placement: String) : AnalyticsEvent
    data class RewardedAdCompleted(val unlockType: String) : AnalyticsEvent
    data class DeleteCompleted(val itemCount: Int, val recoveredBytes: Long) : AnalyticsEvent
}

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
```

- [ ] **Step 2: Add billing product model**

Create `PremiumProduct.kt`:

```kotlin
package com.air.cleaner.core.billing

enum class PremiumProduct(val productId: String) {
    Weekly("premium_weekly"),
    Monthly("premium_monthly"),
    Yearly("premium_yearly"),
}

data class PremiumState(
    val isPremium: Boolean,
    val activeProduct: PremiumProduct?,
)
```

- [ ] **Step 3: Add rewarded unlock model**

Create `RewardedUnlock.kt`:

```kotlin
package com.air.cleaner.core.ads

enum class RewardedUnlock {
    BatchClean,
    VideoCompression,
    SmartSelection,
}

interface RewardedUnlockRepository {
    fun isUnlocked(unlock: RewardedUnlock): Boolean
    fun markUnlocked(unlock: RewardedUnlock)
}
```

- [ ] **Step 4: Commit**

```powershell
git add ai-photo-cleaner/core/analytics ai-photo-cleaner/core/billing ai-photo-cleaner/core/ads
git commit -m "feat: add monetization analytics contracts"
```

## Task 9: Compliance Review Before Internal Test

**Files:**
- Create: `ai-photo-cleaner/docs/compliance/play-store-checklist.md`

- [ ] **Step 1: Add compliance checklist**

Create `ai-photo-cleaner/docs/compliance/play-store-checklist.md`:

```markdown
# Google Play Compliance Checklist

## Store Listing

- App description matches implemented features.
- No antivirus, virus remover, phone booster, CPU cooler, battery repair, or impossible functionality claims.
- Screenshots show real app screens or accurate mockups.

## Permissions

- Media permission requested only after user-facing explanation.
- Contacts permission requested only when contacts cleanup exists.
- No unrelated dangerous permissions.

## Deletion

- No item is deleted without explicit user confirmation.
- Android system deletion confirmation is explained as a normal protection step.

## Subscriptions

- Paywall has a visible close path.
- Weekly, monthly, and yearly prices are shown clearly.
- Cancellation wording is visible.
- Introductory offers are localized if enabled.

## Ads

- Ads do not mimic system warnings.
- Ads do not appear during deletion confirmation or payment.
- Rewarded ad exchange is explicit.

## Privacy

- Analytics do not log filenames, contact names, image contents, or sensitive media metadata.
- Privacy policy describes media scanning, billing, ads, and analytics.
```

- [ ] **Step 2: Commit**

```powershell
git add ai-photo-cleaner/docs/compliance
git commit -m "docs: add play store compliance checklist"
```

## Task 10: Internal Test Readiness

**Files:**
- Create: `ai-photo-cleaner/docs/testing/internal-test-plan.md`

- [ ] **Step 1: Add internal test plan**

Create `ai-photo-cleaner/docs/testing/internal-test-plan.md`:

```markdown
# Internal Test Plan

## Devices

- Android 15 phone
- Android 14 phone
- Android 13 phone
- Android 11 or 12 lower-end phone

## Media Libraries

- Empty gallery
- 1,000 media files
- 10,000 media files
- Mixed screenshots, duplicate photos, similar photos, blurry photos, and large videos

## Required Checks

- Permission explanation appears before system permission prompt.
- Scan progress updates without blocking UI.
- Duplicate groups are understandable.
- User can deselect items before deletion.
- Delete confirmation shows item count and estimated recovered space.
- Paywall appears only after visible scan value.
- Rewarded unlock does not trigger during critical deletion flow.
- Arabic RTL screen is usable.
- German and Hindi strings do not overflow primary buttons.
```

- [ ] **Step 2: Commit**

```powershell
git add ai-photo-cleaner/docs/testing
git commit -m "docs: add internal test plan"
```

## Self-Review

Spec coverage:

- Android native stack: covered by Tasks 1 and 2.
- 20 locales and regional design adaptation: covered by Tasks 2, 3, and 7.
- Figma design flow: covered by Task 3.
- Permission trust flow: covered by Task 4.
- Duplicate photo detection: covered by Task 5.
- Dashboard: covered by Task 6.
- Monetization and ads: covered by Task 8.
- Compliance: covered by Task 9.
- Internal test readiness: covered by Task 10.

Known remaining implementation work after this plan:

- MediaStore scanning repository
- Similar photo perceptual hash
- Blur scoring
- Video compression implementation
- Photo review screen
- Paywall UI
- Google Play Billing live integration
- AdMob live integration
- Firebase live setup
- Figma canvas creation once a target file is available

These are intentionally reserved for the next implementation plan because they are separate subsystems with meaningful testing and review scope.
