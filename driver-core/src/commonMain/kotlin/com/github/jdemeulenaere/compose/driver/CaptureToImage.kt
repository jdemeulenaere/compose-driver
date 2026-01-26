package com.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction

internal expect fun SemanticsNodeInteraction.captureToImage(): ImageBitmap
