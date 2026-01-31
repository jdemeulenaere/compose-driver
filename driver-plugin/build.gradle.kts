plugins {
    `kotlin-dsl`
    alias(libs.plugins.androidLint)
    `maven-publish`
}

group = "com.github.jdemeulenaere"

version = "0.1.0"

dependencies { lintChecks(libs.androidx.lint.gradle) }

gradlePlugin {
    plugins {
        create("driverSettingsPlugin") {
            id = "com.github.jdemeulenaere.compose.driver"
            implementationClass =
                "com.github.jdemeulenaere.compose.driver.plugin.DriverSettingsPlugin"
            displayName = "Compose Driver Plugin"
            description = "Plugin to add and automatically configure Compose Driver in a Compose project"
        }
    }
}
