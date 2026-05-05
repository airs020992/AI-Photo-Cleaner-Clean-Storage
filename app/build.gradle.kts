import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("local-signing.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.air.cleaner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aiphotoclear.storagecleaner"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localSigningProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = localSigningProperties.getProperty("storePassword")
                keyAlias = localSigningProperties.getProperty("keyAlias")
                keyPassword = localSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (localSigningProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:permissions"))
    implementation(project(":domain:cleaning"))
    implementation(project(":data:media"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:scan"))
    implementation(project(":feature:photos"))
    implementation(project(":feature:videos"))
    implementation(project(":feature:paywall"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.coil.compose)
    implementation(libs.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.media3.common)
    implementation(libs.media3.effect)
    implementation(libs.media3.transformer)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
