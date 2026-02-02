plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "io.github.jdemeulenaere.compose.driver.sample.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions { unitTests { isIncludeAndroidResources = true } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(projects.multiplatform)
    implementation(libs.compose.material3)
}
