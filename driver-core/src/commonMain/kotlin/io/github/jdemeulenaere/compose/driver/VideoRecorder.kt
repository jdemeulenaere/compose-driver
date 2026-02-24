package io.github.jdemeulenaere.compose.driver

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isRoot as isRootMatcher
import io.ktor.http.ContentType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Session-based video recorder that pipes raw frames to ffmpeg in real-time.
 *
 * Frames are captured cooperatively: the server calls [captureFrame] before and after each action
 * endpoint while recording is active. This avoids monopolizing the single-threaded test dispatcher
 * with a background loop.
 *
 * An optional [SemanticsNodeInteraction] can be provided to [start] to record a specific composable
 * instead of the full root. The target node's dimensions are measured once at start time and must
 * remain stable for the duration of the recording (ffmpeg requires fixed frame dimensions).
 */
@OptIn(ExperimentalTestApi::class)
internal class VideoRecorder {
    private var state: RecordingState? = null

    val isRecording: Boolean get() = state != null

    /**
     * Starts recording frames from [target] (or the first root node if `null`).
     *
     * The target node's dimensions are captured once and used for all subsequent frames. If the
     * target changes size during recording, frames will still be captured at the original
     * dimensions, which may cause visual artifacts or ffmpeg errors.
     */
    fun start(test: ComposeUiTest, format: VideoFormat, fps: Int, target: SemanticsNodeInteraction? = null) {
        check(state == null) { "Recording is already in progress" }

        val captureTarget = target ?: test.onAllNodes(isRootMatcher())[0]
        val image = captureTarget.captureToImage()
        val width = image.width
        val height = image.height

        val tempDir = Files.createTempDirectory("compose-driver-video")
        val outputFile = tempDir.resolve("output.${format.extension}")

        val cmd = buildList {
            addAll(
                listOf(
                    "ffmpeg", "-y",
                    "-f", "rawvideo",
                    "-pixel_format", "bgra",
                    "-video_size", "${width}x${height}",
                    "-framerate", fps.toString(),
                    "-i", "pipe:0",
                )
            )
            when (format) {
                VideoFormat.MP4 -> addAll(listOf("-c:v", "libx264", "-pix_fmt", "yuv420p"))
                VideoFormat.WEBM -> addAll(listOf("-c:v", "libvpx-vp9", "-pix_fmt", "yuv420p"))
            }
            add(outputFile.absolutePathString())
        }

        val process = ProcessBuilder(cmd)
            .directory(tempDir.toFile())
            .redirectErrorStream(true)
            .start()

        test.mainClock.autoAdvance = false
        val timeBetweenFramesMs = 1_000L / fps

        state = RecordingState(
            process = process,
            tempDir = tempDir,
            outputFile = outputFile,
            format = format,
            test = test,
            captureTarget = captureTarget,
            timeBetweenFramesMs = timeBetweenFramesMs,
        )

        // Capture the initial frame.
        captureFrame()
    }

    /**
     * Captures a single frame of the current UI state. Call this from the test dispatcher context
     * (i.e., within `withContext(runTestContext)`). Advances the virtual clock by one frame interval.
     */
    fun captureFrame() {
        val recording = state ?: return
        recording.test.waitForIdle()
        val image = recording.captureTarget.captureToImage()
        writeRawPixels(image, recording.process.outputStream)
        recording.process.outputStream.flush()
        recording.test.mainClock.advanceTimeBy(recording.timeBetweenFramesMs)
    }

    fun stop(): RecordingResult {
        val recording = checkNotNull(state) { "No recording in progress" }
        state = null

        try {
            // Capture a final frame before stopping.
            recording.test.waitForIdle()
            val image = recording.captureTarget.captureToImage()
            writeRawPixels(image, recording.process.outputStream)
            recording.process.outputStream.flush()

            recording.process.outputStream.close()
            recording.process.waitFor()

            recording.test.mainClock.autoAdvance = true

            val exitCode = recording.process.exitValue()
            check(exitCode == 0) {
                val errorOutput = recording.process.inputStream.bufferedReader().readText()
                "ffmpeg failed with exit code $exitCode: $errorOutput"
            }

            return RecordingResult(
                file = recording.outputFile,
                contentType = recording.format.contentType,
                tempDir = recording.tempDir,
            )
        } catch (e: Throwable) {
            recording.test.mainClock.autoAdvance = true
            recording.tempDir.toFile().deleteRecursively()
            throw e
        }
    }

    class RecordingResult(
        val file: Path,
        val contentType: ContentType,
        private val tempDir: Path,
    ) {
        fun cleanup() {
            tempDir.toFile().deleteRecursively()
        }
    }

    private class RecordingState(
        val process: Process,
        val tempDir: Path,
        val outputFile: Path,
        val format: VideoFormat,
        val test: ComposeUiTest,
        val captureTarget: SemanticsNodeInteraction,
        val timeBetweenFramesMs: Long,
    )
}
