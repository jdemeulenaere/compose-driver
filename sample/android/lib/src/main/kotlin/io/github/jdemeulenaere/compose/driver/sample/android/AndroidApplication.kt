package io.github.jdemeulenaere.compose.driver.sample.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import io.github.jdemeulenaere.compose.driver.sample.multiplatform.MultiplatformApplication

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun AndroidApplication() {
    MultiplatformApplication(name = "AndroidApplication")
}
