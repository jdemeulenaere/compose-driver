package io.github.jdemeulenaere.compose.driver

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.OutputStream

internal actual fun writePng(image: ImageBitmap, out: OutputStream) {
    image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, out)
}
