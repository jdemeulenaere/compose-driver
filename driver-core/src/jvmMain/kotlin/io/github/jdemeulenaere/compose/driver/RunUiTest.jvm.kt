package io.github.jdemeulenaere.compose.driver

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@OptIn(ExperimentalTestApi::class)
actual fun runUiTest(
    effectContext: CoroutineContext,
    runTestContext: CoroutineContext,
    testTimeout: Duration,
    block: suspend ComposeUiTest.() -> Unit,
) {
    val size =
        System.getProperty("compose.driver.desktop.window.width")?.let { width ->
            System.getProperty("compose.driver.desktop.window.height")?.let { height ->
                Size(width.toFloat(), height.toFloat())
            }
        } ?: Size(1024.0f, 768.0f)
    val density =
        System.getProperty("compose.driver.desktop.window.density")?.let { Density(it.toFloat()) }
            ?: Density(1f)
    runSkikoComposeUiTest(size, density, effectContext, runTestContext, testTimeout, block)
}
