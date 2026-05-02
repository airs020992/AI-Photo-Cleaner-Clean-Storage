plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.air.cleaner.domain.cleaning"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }
}
