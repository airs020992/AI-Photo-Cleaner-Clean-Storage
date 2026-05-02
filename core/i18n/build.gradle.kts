plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.air.cleaner.core.i18n"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }
}
