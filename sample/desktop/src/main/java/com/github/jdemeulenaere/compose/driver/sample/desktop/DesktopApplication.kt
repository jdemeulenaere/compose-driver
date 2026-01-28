package com.github.jdemeulenaere.compose.driver.sample.desktop

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import com.github.jdemeulenaere.compose.driver.sample.multiplatform.MultiplatformApplication

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun DesktopApplication() {
    MultiplatformApplication(name = "DesktopApplication")
}
