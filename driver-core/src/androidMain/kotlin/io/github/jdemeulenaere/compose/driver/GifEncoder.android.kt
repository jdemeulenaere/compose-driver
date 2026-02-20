package io.github.jdemeulenaere.compose.driver

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.OutputStream
import java.nio.ByteBuffer

internal actual fun writePng(image: ImageBitmap, out: OutputStream) {
    image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, out)
}

internal actual fun writeRawPixels(image: ImageBitmap, out: OutputStream) {
    val bitmap = image.asAndroidBitmap()
    // Bitmap stores pixels as ARGB_8888 by default.
    // Allocate buffer and copy pixels, then convert ARGB to BGRA for ffmpeg.
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val buf = ByteBuffer.allocate(pixels.size * 4)
    for (pixel in pixels) {
        val a = (pixel shr 24) and 0xFF
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        buf.put(b.toByte())
        buf.put(g.toByte())
        buf.put(r.toByte())
        buf.put(a.toByte())
    }
    out.write(buf.array())
}
