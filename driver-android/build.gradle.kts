plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.github.jdemeulenaere.compose.driver.sample.android"
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

androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enable = false }
    beforeVariants(selector().all()) { it.androidTestEnabled = false }
}

dependencies {
    testImplementation(project(":driver-core"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.material3)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.slf4j.simple)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Dynamically add the run task and configure tests to enable the server. Doing so allows to
// normally run `./gradlew build` to build and test everything else without running the server.
if (gradle.startParameter.taskNames.any { it == ":driver-android:run" }) {
    afterEvaluate {
        val testDebugUnitTest =
            tasks.named<Test>("testDebugUnitTest") {
                systemProperty("compose.driver.enabled", "true")
            }
        tasks.register("run") { dependsOn(testDebugUnitTest) }
    }
}
