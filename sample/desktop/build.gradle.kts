plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.application)
}

application { mainClass = "com.github.jdemeulenaere.compose.driver.sample.desktop.MainKt" }

dependencies {
    implementation(projects.multiplatform)
    implementation(libs.compose.material3)
    implementation(compose.desktop.currentOs)
}

// These tasks fail with Compose Multiplatform 1.10.0, disable them for now.
tasks.named("distTar").configure { enabled = false }

tasks.named("distZip").configure { enabled = false }
