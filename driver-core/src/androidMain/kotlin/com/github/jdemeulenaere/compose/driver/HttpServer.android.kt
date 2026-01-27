package com.github.jdemeulenaere.compose.driver

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import io.ktor.http.ContentType
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingCall
import java.util.concurrent.TimeUnit

internal actual val ApplicationEngineFactory: ApplicationEngineFactory<*, *> = Netty

internal actual suspend fun RoutingCall.respondImage(image: ImageBitmap) {
    respondOutputStream(ContentType.Image.PNG) {
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, this)
    }
}

internal actual suspend fun RoutingCall.respondGif(
    frames: List<ImageBitmap>,
    timeBetweenFramesMs: Long,
) {
    respondOutputStream(ContentType.Image.GIF) {
        val frames = frames
        val firstFrame = frames.first()
        val pixels = IntArray(firstFrame.width * firstFrame.height)
        val encoder = GifEncoder(this, firstFrame.width, firstFrame.height, 0)
        val options = ImageOptions().apply { setDelay(timeBetweenFramesMs, TimeUnit.MILLISECONDS) }
        frames.forEach { frame ->
            frame.readPixels(pixels)
            encoder.addImage(pixels, frame.width, options)
        }
        encoder.finishEncoding()
    }
}
