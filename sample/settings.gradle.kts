rootProject.name = "compose-driver-sample"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    val isDev = providers.gradleProperty("compose.driver.dev").orNull?.toBoolean() ?: false
    if (isDev) {
        includeBuild("../")
    }

    val driverVersion =
        if (!isDev) {
            providers.gradleProperty("compose.driver.version").get()
        } else {
            null
        }

    val useMavenLocal =
        providers.gradleProperty("compose.driver.local").orNull?.toBoolean() ?: false

    repositories {
        if (useMavenLocal) {
            mavenLocal()
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    driverVersion?.let { version ->
        resolutionStrategy {
            eachPlugin {
                if (requested.id.id == "com.github.jdemeulenaere.compose.driver") {
                    useVersion(version)
                }
            }
        }
    }
}

dependencyResolutionManagement {
    val useMavenLocal =
        providers.gradleProperty("compose.driver.local").map { it.toBoolean() }.getOrElse(false)
    repositories {
        if (useMavenLocal) {
            mavenLocal()
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.github.jdemeulenaere.compose.driver")
}

composeDriver {
    androidProjectName.set("compose-driver-android")
    desktopProjectName.set("compose-driver-desktop")
}

include(":android:lib")

include(":desktop")

include(":multiplatform")
