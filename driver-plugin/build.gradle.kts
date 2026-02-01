plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.androidLint)
    alias(libs.plugins.gradlePluginPublish)
}

group = "io.github.jdemeulenaere"

version = providers.gradleProperty("compose.driver.version").get()

dependencies { lintChecks(libs.androidx.lint.gradle) }

val generateVersionFile by
    tasks.registering {
        val outputDir = layout.buildDirectory.dir("generated/resources")
        val version = project.version
        inputs.property("version", version)
        outputs.dir(outputDir)
        doLast {
            val propsFile = outputDir.get().file("compose-driver.properties").asFile
            propsFile.parentFile.mkdirs()
            propsFile.writeText("version=$version")
        }
    }

sourceSets.main { resources.srcDir(generateVersionFile) }

gradlePlugin {
    plugins {
        create("driverSettingsPlugin") {
            id = "io.github.jdemeulenaere.compose.driver"
            implementationClass =
                "io.github.jdemeulenaere.compose.driver.plugin.DriverSettingsPlugin"
            displayName = "Compose Driver Plugin"
            description =
                "Plugin to add and automatically configure Compose Driver in a Compose project"
            tags.set(
                listOf("compose", "driver", "ui-testing", "android", "desktop", "multiplatform")
            )
        }
    }

    website.set("https://github.com/jdemeulenaere/compose-driver")
    vcsUrl.set("https://github.com/jdemeulenaere/compose-driver")
}
