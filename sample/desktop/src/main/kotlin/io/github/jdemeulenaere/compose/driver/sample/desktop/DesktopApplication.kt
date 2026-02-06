package io.github.jdemeulenaere.compose.driver.sample.desktop

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import io.github.jdemeulenaere.compose.driver.sample.multiplatform.MultiplatformApplication

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun DesktopApplication() {
    MultiplatformApplication(name = "DesktopApplication")
}
