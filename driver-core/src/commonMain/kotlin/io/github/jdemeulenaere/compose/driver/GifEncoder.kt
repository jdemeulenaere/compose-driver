package io.github.jdemeulenaere.compose.driver

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.http.ContentType
import io.ktor.server.routing.RoutingContext
import java.io.OutputStream
import java.nio.file.Files
import kotlin.io.path.absolutePathString

internal expect fun writePng(image: ImageBitmap, out: OutputStream)

internal expect fun writeRawPixels(image: ImageBitmap, out: OutputStream)

internal enum class VideoFormat(val extension: String, val contentType: ContentType) {
    MP4("mp4", ContentType("video", "mp4")),
    WEBM("webm", ContentType("video", "webm"));

    companion object {
        fun fromString(value: String): VideoFormat =
            when (value.lowercase()) {
                "mp4" -> MP4
                "webm" -> WEBM
                else -> throw IllegalArgumentException(
                    "Unknown video format '$value'. Use 'mp4' or 'webm'."
                )
            }
    }
}

internal suspend fun RoutingContext.respondGif(
    frames: List<ImageBitmap>,
    timeBetweenFramesMs: Long,
) {
    val dir = Files.createTempDirectory("compose-driver-gif")
    try {
        writeFramePngs(frames, dir)

        // Make the time between frames at least 20ms, otherwise browsers will slow down the GIF.
        val framerate = 1_000.0 / timeBetweenFramesMs.coerceAtLeast(20L)
        val outputGif = dir.resolve("output.gif")
        val cmd =
            listOf(
                "ffmpeg",
                "-y",
                "-framerate",
                framerate.toString(),
                "-i",
                "$dir/frame_%03d.png",
                "-vf",
                "split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse",
                outputGif.absolutePathString(),
            )

        runFfmpeg(cmd, dir)
        call.respondStream(ContentType.Image.GIF) { Files.copy(outputGif, this) }
    } finally {
        dir.toFile().deleteRecursively()
    }
}

internal suspend fun RoutingContext.respondVideo(
    frames: List<ImageBitmap>,
    timeBetweenFramesMs: Long,
    format: VideoFormat,
) {
    val dir = Files.createTempDirectory("compose-driver-video")
    try {
        writeFramePngs(frames, dir)

        val framerate = 1_000.0 / timeBetweenFramesMs.coerceAtLeast(20L)
        val outputFile = dir.resolve("output.${format.extension}")
        val cmd = buildList {
            addAll(listOf("ffmpeg", "-y", "-framerate", framerate.toString()))
            addAll(listOf("-i", "$dir/frame_%03d.png"))
            when (format) {
                VideoFormat.MP4 -> addAll(listOf("-c:v", "libx264", "-pix_fmt", "yuv420p"))
                VideoFormat.WEBM -> addAll(listOf("-c:v", "libvpx-vp9", "-pix_fmt", "yuv420p"))
            }
            add(outputFile.absolutePathString())
        }

        runFfmpeg(cmd, dir)
        call.respondStream(format.contentType) { Files.copy(outputFile, this) }
    } finally {
        dir.toFile().deleteRecursively()
    }
}

private fun writeFramePngs(frames: List<ImageBitmap>, dir: java.nio.file.Path) {
    frames.forEachIndexed { index, image ->
        val frameName = "frame_${index.toString().padStart(3, '0')}.png"
        Files.newOutputStream(dir.resolve(frameName)).use { writePng(image, it) }
    }
}

private fun runFfmpeg(cmd: List<String>, dir: java.nio.file.Path) {
    val process = ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true).start()
    val exitCode = process.waitFor()
    check(exitCode == 0) {
        val errorOutput = process.inputStream.bufferedReader().readText()
        "${cmd.first()} failed with exit code $exitCode: $errorOutput"
    }
}
