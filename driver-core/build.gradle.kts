plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

group = "com.github.jdemeulenaere"

version = providers.gradleProperty("compose.driver.version").get()

kotlin {
    jvm()
    androidLibrary {
        namespace = "com.github.jdemeulenaere.compose.driver"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.androidx.navigation.event)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.call.logging)
                implementation(libs.ktor.server.status.pages)
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            val publicationName = this.name
            if (publicationName == "kotlinMultiplatform") {
                artifactId = "compose-driver-core"
            } else if (publicationName == "jvm") {
                artifactId = "compose-driver-jvm"
            } else if (publicationName == "android") {
                artifactId = "compose-driver-android"
            }
        }
    }
}
