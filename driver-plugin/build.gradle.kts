plugins {
    `kotlin-dsl`
    alias(libs.plugins.androidLint)
}

dependencies { lintChecks(libs.androidx.lint.gradle) }

gradlePlugin {
    plugins {
        create("driverSettingsPlugin") {
            id = "com.github.jdemeulenaere.compose.driver"
            implementationClass =
                "com.github.jdemeulenaere.compose.driver.plugin.DriverSettingsPlugin"
        }
    }
}
