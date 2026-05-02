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
