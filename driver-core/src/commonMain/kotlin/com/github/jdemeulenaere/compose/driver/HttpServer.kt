package com.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.routing.RoutingCall

internal expect val ApplicationEngineFactory: ApplicationEngineFactory<*, *>

internal expect suspend fun RoutingCall.respondImage(image: ImageBitmap)

internal expect suspend fun RoutingCall.respondGif(
    frames: List<ImageBitmap>,
    timeBetweenFramesMs: Long
)
