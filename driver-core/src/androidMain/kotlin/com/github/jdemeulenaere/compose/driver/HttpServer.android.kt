package com.github.jdemeulenaere.compose.driver

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import io.ktor.http.ContentType
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingCall

internal actual val ApplicationEngineFactory: ApplicationEngineFactory<*, *> = Netty

internal actual suspend fun RoutingCall.respondImage(image: ImageBitmap) {
    respondOutputStream(ContentType.Image.PNG) {
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, this)
    }
}
