package com.github.jdemeulenaere.compose.driver.plugin

import org.gradle.api.provider.Property

interface DriverSettingsPluginExtension {
    /**
     * The name of the subproject to add to the root project and that will automatically depend on
     * all other Kotlin Android or Multiplatform subprojects that use the Compose compiler plugin.
     *
     * When set, use `./gradlew :[androidProjectName]:run
     * -Dcompose.driver.composable=com.example.foo.FooKt.MyFoo` to start the server using the
     * `MyFoo` composable inside `Foo.kt`. Note that this will run within a Robolectric unit test,
     * so start-up is expected to be slower than the desktop project.
     */
    val androidProjectName: Property<String>

    /**
     * The name of the subproject to add to the root project and that will automatically depend on
     * all other Kotlin JVM or Multiplatform subprojects that use the Compose compiler plugin.
     *
     * When set, use `./gradlew :[desktopProjectName]:run
     * -Dcompose.driver.composable=com.example.foo.FooKt.MyFoo` to start the server using the
     * `MyFoo` composable inside `Foo.kt`.
     */
    val desktopProjectName: Property<String>
}
