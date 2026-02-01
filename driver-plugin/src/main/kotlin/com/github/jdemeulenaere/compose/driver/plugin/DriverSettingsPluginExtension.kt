package com.github.jdemeulenaere.compose.driver.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
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

abstract class DriverPlatformConfiguration {
    abstract val name: Property<String>
    abstract val enabled: Property<Boolean>
    internal abstract val dependencyActions: ListProperty<Action<DependencyHandler>>

    fun dependencies(action: Action<DependencyHandler>) {
        dependencyActions.add(action)
    }
}

abstract class DesktopDriverConfiguration : DriverPlatformConfiguration() {
    abstract val widthDp: Property<Int>
    abstract val heightDp: Property<Int>
    abstract val density: Property<Float>
}

abstract class AndroidDriverConfiguration : DriverPlatformConfiguration() {
    @get:Nested abstract val robolectric: RobolectricConfiguration

    abstract val missingDimensionsStrategy: MapProperty<String, String>

    fun missingDimensionStrategy(dimension: String, flavor: String) {
        missingDimensionsStrategy.put(dimension, flavor)
    }

    fun robolectric(action: Action<RobolectricConfiguration>) {
        action.execute(robolectric)
    }
}

interface RobolectricConfiguration {
    val sdk: Property<Int>
    val qualifiers: Property<String>
}
