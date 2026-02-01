package com.github.jdemeulenaere.compose.driver.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** Generate a test class that starts the Compose Driver server in a Robolectric environment. */
abstract class GenerateAndroidTestClassTask : DefaultTask() {
    @get:OutputDirectory abstract val outputDir: DirectoryProperty
    @get:Input @get:Optional abstract val sdk: Property<Int>
    @get:Input @get:Optional abstract val qualifiers: Property<String>

    @TaskAction
    fun generate() {
        val packageName = "com.github.jdemeulenaere.compose.driver.android"
        val file =
            outputDir
                .get()
                .asFile
                .resolve(packageName.replace('.', '/'))
                .resolve("ComposeDriverTest.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.github.jdemeulenaere.compose.driver.android

            import androidx.test.ext.junit.runners.AndroidJUnit4
            import com.github.jdemeulenaere.compose.driver.startComposeDriverServer
            import org.junit.Assume
            import org.junit.Test
            import org.junit.runner.RunWith
            import org.robolectric.annotation.Config
            import org.robolectric.annotation.GraphicsMode

            /** Test class generated to start the Compose Driver server in a Robolectric environment. Do not edit. */
            @RunWith(AndroidJUnit4::class)
            @GraphicsMode(GraphicsMode.Mode.NATIVE)
            ${configAnnotation()}
            class ComposeDriverTest {
                @Test
                fun test() {
                    // Only run the server in this test if `compose.driver.enabled` is true. This makes this
                    // test be a no-op when running all tests in the project.
                    val enabled = System.getProperty("compose.driver.enabled")?.toBoolean() ?: false
                    Assume.assumeTrue("compose-driver is enabled", enabled)

                    startComposeDriverServer()
                }
            }
            """
                .trimIndent()
        )
    }

    private fun configAnnotation(): String {
        val sdk = sdk.orElse(36).get()
        val qualifiers = qualifiers.orElse("w410dp-h920dp-xhdpi").get()
        return "@Config(sdk = [$sdk], qualifiers = \"$qualifiers\")"
    }
}
