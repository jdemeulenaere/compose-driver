package com.github.jdemeulenaere.compose.driver.sample.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import com.github.jdemeulenaere.compose.driver.sample.multiplatform.MultiplatformApplication

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun AndroidApplication() {
    MultiplatformApplication(name = "AndroidApplication")
}
