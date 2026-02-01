package com.github.jdemeulenaere.compose.driver.plugin

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

abstract class DriverSettingsPluginExtension {
    @get:Nested abstract val android: AndroidDriverConfiguration

    @get:Nested abstract val desktop: DesktopDriverConfiguration

    fun android(action: Action<AndroidDriverConfiguration>) {
        android.enabled.set(true)
        action.execute(android)
    }

    fun android() {
        android.enabled.set(true)
    }

    fun desktop(action: Action<DesktopDriverConfiguration>) {
        desktop.enabled.set(true)
        action.execute(desktop)
    }

    fun desktop() {
        desktop.enabled.set(true)
    }
}

interface DriverPlatformConfiguration {
    val name: Property<String>
    val enabled: Property<Boolean>
}

interface DesktopDriverConfiguration : DriverPlatformConfiguration {
    val widthDp: Property<Int>
    val heightDp: Property<Int>
    val density: Property<Float>
}

interface AndroidDriverConfiguration : DriverPlatformConfiguration {
    @get:Nested val robolectric: RobolectricConfiguration

    fun robolectric(action: Action<RobolectricConfiguration>) {
        action.execute(robolectric)
    }
}

interface RobolectricConfiguration {
    val sdk: Property<Int>
    val qualifiers: Property<String>
}
