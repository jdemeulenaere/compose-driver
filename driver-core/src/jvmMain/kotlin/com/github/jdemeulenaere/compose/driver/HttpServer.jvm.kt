package com.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import io.ktor.http.ContentType
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingCall
import javax.imageio.ImageIO

internal actual val ApplicationEngineFactory: ApplicationEngineFactory<*, *> = Netty

internal actual suspend fun RoutingCall.respondImage(image: ImageBitmap) {
    respondOutputStream(ContentType.Image.PNG) { ImageIO.write(image.toAwtImage(), "png", this) }
}
