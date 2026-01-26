package com.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage

internal actual fun SemanticsNodeInteraction.captureToImage(): ImageBitmap = captureToImage()
