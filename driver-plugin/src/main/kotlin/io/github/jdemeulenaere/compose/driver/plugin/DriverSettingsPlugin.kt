package io.github.jdemeulenaere.compose.driver.plugin

import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

internal class DriverSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        configureComposeDriverProject(settings)
    }
}

private fun configureComposeDriverProject(settings: Settings) {
    val extension =
        settings.extensions.create("composeDriver", DriverSettingsPluginExtension::class.java)

    extension.android.enabled.convention(false)
    extension.android.name.convention("compose-driver-android")

    extension.desktop.enabled.convention(false)
    extension.desktop.name.convention("compose-driver-desktop")

    settings.gradle.settingsEvaluated {
        val version = composeDriverVersion()
        val driverDependency = driverDependency(version)
        val androidProjectPath =
            if (extension.android.enabled.get()) {
                val name = extension.android.name.get()
                settings.addProject(
                    name,
                    androidBuildFile(
                        projectName = name,
                        driverDependency = driverDependency,
                        sdk = extension.android.robolectric.sdk.orNull,
                        qualifiers = extension.android.robolectric.qualifiers.orNull,
                        missingDimensionsStrategy =
                            extension.android.missingDimensionsStrategy.getOrElse(emptyMap()),
                    ),
                )
            } else {
                null
            }

        val desktopProjectPath =
            if (extension.desktop.enabled.get()) {
                val name = extension.desktop.name.get()
                settings.addProject(
                    name,
                    desktopBuildFile(
                        driverDependency = driverDependency,
                        configuration = extension.desktop,
                    ),
                )
            } else {
                null
            }

        check(androidProjectPath != null || desktopProjectPath != null) {
            "Compose Driver plugin was applied but neither android nor desktop targets were enabled.\n" +
                "Please add `android()` or `desktop()` to the `composeDriver { ... }` block in settings.gradle.kts."
        }

        addDependencies(settings, androidProjectPath, desktopProjectPath, extension)
    }
}

private fun composeDriverVersion(): String {
    val props = Properties()
    DriverSettingsPlugin::class.java.getResourceAsStream("/compose-driver.properties").use {
        props.load(it)
    }
    return props.getProperty("version")
}

private fun Settings.addProject(projectName: String, buildFile: String): String {
    val projectPath = ":$projectName"
    val projectDir =
        this.rootProject.projectDir.resolve("build").resolve("compose-driver").resolve(projectName)

    // Set the content of the build file. Note that we don't dynamically configure the project
    // and its extensions here because this unfortunately leads to classpath issues between
    // settings and projects. Generating a build.gradle(.kts) file here works best.
    projectDir.mkdirs()
    projectDir.resolve("build.gradle.kts").writeText(buildFile)

    include(projectPath)
    project(projectPath).projectDir = projectDir
    return projectPath
}

private fun addDependencies(
    settings: Settings,
    androidProjectPath: String?,
    desktopProjectPath: String?,
    extension: DriverSettingsPluginExtension,
) {
    settings.gradle.beforeProject {
        fun addDependency(path: String, projectPath: String) {
            project.logger.lifecycle("Added project $path as dependency to $desktopProjectPath")
            val targetProject = project(projectPath)
            if (targetProject.state.executed) {
                targetProject.dependencies.add("implementation", project(path))
            } else {
                targetProject.afterEvaluate { dependencies.add("implementation", project(path)) }
            }
        }

        if (path == androidProjectPath) {
            project.plugins.withId("com.android.library") {
                extension.android.dependencyActions.getOrElse(emptyList()).forEach {
                    it.execute(dependencies)
                }
            }
            return@beforeProject
        }
        if (path == desktopProjectPath) {
            project.plugins.withId("application") {
                extension.desktop.dependencyActions.getOrElse(emptyList()).forEach {
                    it.execute(dependencies)
                }
            }
            return@beforeProject
        }

        project.plugins.withId("org.jetbrains.kotlin.plugin.compose") {
            val path = project.path
            project.plugins.withId("com.android.library") {
                androidProjectPath?.let { addDependency(path, it) }
            }
            project.plugins.withId("org.jetbrains.kotlin.jvm") {
                desktopProjectPath?.let { addDependency(path, it) }
            }
            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
                androidProjectPath?.let { addDependency(path, it) }
                desktopProjectPath?.let { addDependency(path, it) }
            }
        }
    }
}

private const val COMPOSABLE_PROPERTY = "compose.driver.composable"

private fun driverDependency(version: String): String {
    return if (version.endsWith("-SNAPSHOT")) {
        "project(\":compose-driver:driver-core\")"
    } else {
        "\"io.github.jdemeulenaere:compose-driver:$version\""
    }
}

private fun androidBuildFile(
    projectName: String,
    driverDependency: String,
    sdk: Int?,
    qualifiers: String?,
    missingDimensionsStrategy: Map<String, String>,
): String {
    val sdkProperty = sdk?.let { "sdk.set($it)" } ?: ""
    val qualifiersProperty = qualifiers?.let { "qualifiers.set(\"$it\")" } ?: ""

    val missingDimensionsStrategyProperties =
        missingDimensionsStrategy.entries.joinToString("\n") { (dimension, flavor) ->
            "missingDimensionStrategy(\"$dimension\", \"$flavor\")"
        }

    return $$"""
    /** This file was generated by the Compose Driver plugin. Do not edit. */
    import io.github.jdemeulenaere.compose.driver.plugin.GenerateAndroidTestClassTask
    import java.util.Locale
    
    plugins { id("com.android.library") }
    
    android {
        namespace = "io.github.jdemeulenaere.compose.driver.android"
        compileSdk = 36
    
        defaultConfig {
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            $$missingDimensionsStrategyProperties
        }
    
        testOptions { unitTests { isIncludeAndroidResources = true } }
    }
    
    androidComponents {
        beforeVariants(selector().withBuildType("release")) { it.enable = false }
        beforeVariants(selector().all()) { it.androidTestEnabled = false }
    
        onVariants { variant ->
            variant.hostTests.forEach { (_, test) ->
                val variantName =
                    variant.name.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                val task =
                    tasks.register<GenerateAndroidTestClassTask>("generateTestClassFor$variantName") {
                        outputDir.set(layout.buildDirectory.dir("generated/source/test/$variantName"))
                        $$sdkProperty
                        $$qualifiersProperty
                    }
                test.sources.java!!.addGeneratedSourceDirectory(task, GenerateAndroidTestClassTask::outputDir)
            }
        }
    }
    
    dependencies {
        testImplementation($$driverDependency)
        testImplementation("androidx.test.ext:junit:1.3.0")
        testImplementation("org.robolectric:robolectric:4.16.1")
        debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.0")
    }
    
    // Dynamically add the run task and configure tests to enable the server. Doing so allows to
    // normally run `./gradlew build` to build and test everything else without running the server.
    if (gradle.startParameter.taskNames.any { it == ":$$projectName:run" }) {
        afterEvaluate {
            val testDebugUnitTest =
                tasks.named<Test>("testDebugUnitTest") {
                    systemProperty("compose.driver.enabled", "true")
                    
                    val prop = "$$COMPOSABLE_PROPERTY"
                    System.getProperty(prop)?.let { systemProperty(prop, it) }
                }
            tasks.register("run") { dependsOn(testDebugUnitTest) }
        }
    }
    """
        .trimIndent()
}

private fun desktopBuildFile(
    driverDependency: String,
    configuration: DesktopDriverConfiguration,
): String {
    val width = configuration.widthDp.orNull
    val height = configuration.heightDp.orNull
    val density = configuration.density.orNull

    return """
    /** This file was generated by the Compose Driver plugin. Do not edit. */
    plugins { application }

    application { mainClass = "io.github.jdemeulenaere.compose.driver.MainKt" }

    dependencies {
        implementation($driverDependency)
        implementation("org.slf4j:slf4j-simple:2.0.17")
    }

    // Forward the configurations to the `run` task.
    tasks.named<JavaExec>("run") {
        val prop = "$COMPOSABLE_PROPERTY"
        System.getProperty(prop)?.let { systemProperty(prop, it) }
        
        ${width?.let { "systemProperty(\"compose.driver.desktop.window.width\", $it)" } ?: ""}
        ${height?.let { "systemProperty(\"compose.driver.desktop.window.height\", $it)" } ?: ""}
        ${density?.let { "systemProperty(\"compose.driver.desktop.window.density\", ${it}f)" } ?: ""}
    }

    // These tasks fail with Compose Multiplatform 1.10.0, disable them for now.
    tasks.named("distTar").configure { enabled = false }
    tasks.named("distZip").configure { enabled = false }
    """
        .trimIndent()
}
