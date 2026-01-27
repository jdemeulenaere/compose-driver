plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

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
                implementation(libs.kotlin.stdlib)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.call.logging)
                implementation(libs.ktor.server.status.pages)
            }
        }

        androidMain { }
    }
}
