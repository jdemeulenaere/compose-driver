package com.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import java.io.OutputStream
import javax.imageio.ImageIO

internal actual fun writePng(image: ImageBitmap, out: OutputStream) {
    ImageIO.write(image.toAwtImage(), "png", out)
}
