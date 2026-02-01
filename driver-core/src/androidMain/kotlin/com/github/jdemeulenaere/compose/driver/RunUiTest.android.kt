package com.github.jdemeulenaere.compose.driver

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@OptIn(ExperimentalTestApi::class)
actual fun runUiTest(
    effectContext: CoroutineContext,
    runTestContext: CoroutineContext,
    testTimeout: Duration,
    block: suspend ComposeUiTest.() -> Unit,
) = runComposeUiTest(effectContext, runTestContext, testTimeout, block)
