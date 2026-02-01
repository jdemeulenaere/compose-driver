rootProject.name = "compose-driver-sample"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    val version = providers.gradleProperty("compose.driver.version").get()
    val isDev = version.endsWith("-SNAPSHOT")
    if (isDev) {
        includeBuild("../")
    }

    repositories {
        val useMavenLocal =
            providers.gradleProperty("compose.driver.local").orNull?.toBoolean() ?: false
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

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.github.jdemeulenaere.compose.driver") {
                useVersion(version)
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        val useMavenLocal =
            providers.gradleProperty("compose.driver.local").map { it.toBoolean() }.getOrElse(false)
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
    android()
    desktop()
}

include(":android:lib")

include(":desktop")

include(":multiplatform")
