package io.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

internal actual fun writePng(image: ImageBitmap, out: OutputStream) {
    ImageIO.write(image.toAwtImage(), "png", out)
}

internal actual fun writeRawPixels(image: ImageBitmap, out: OutputStream) {
    val awtImage = image.toAwtImage()
    // Convert to TYPE_INT_ARGB to get consistent pixel layout.
    val argbImage = if (awtImage.type == BufferedImage.TYPE_INT_ARGB) {
        awtImage
    } else {
        BufferedImage(awtImage.width, awtImage.height, BufferedImage.TYPE_INT_ARGB).also {
            it.createGraphics().apply {
                drawImage(awtImage, 0, 0, null)
                dispose()
            }
        }
    }
    val pixels = (argbImage.raster.dataBuffer as DataBufferInt).data
    // Convert ARGB int array to BGRA byte array for ffmpeg rawvideo.
    val buf = ByteBuffer.allocate(pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (pixel in pixels) {
        // ARGB int (big-endian): A[31:24] R[23:16] G[15:8] B[7:0]
        // We need BGRA byte order for ffmpeg.
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
