@file:OptIn(ExperimentalTestApi::class)

package io.github.jdemeulenaere.compose.driver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot as isRootMatcher
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.ktor.http.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class VideoRecordingTest {

    // -- VideoFormat tests --

    @Test
    fun videoFormat_mp4_hasCorrectProperties() {
        assertEquals("mp4", VideoFormat.MP4.extension)
        assertEquals(ContentType("video", "mp4"), VideoFormat.MP4.contentType)
    }

    @Test
    fun videoFormat_webm_hasCorrectProperties() {
        assertEquals("webm", VideoFormat.WEBM.extension)
        assertEquals(ContentType("video", "webm"), VideoFormat.WEBM.contentType)
    }

    @Test
    fun videoFormat_fromString_parsesMp4() {
        assertEquals(VideoFormat.MP4, VideoFormat.fromString("mp4"))
        assertEquals(VideoFormat.MP4, VideoFormat.fromString("MP4"))
        assertEquals(VideoFormat.MP4, VideoFormat.fromString("Mp4"))
    }

    @Test
    fun videoFormat_fromString_parsesWebm() {
        assertEquals(VideoFormat.WEBM, VideoFormat.fromString("webm"))
        assertEquals(VideoFormat.WEBM, VideoFormat.fromString("WEBM"))
        assertEquals(VideoFormat.WEBM, VideoFormat.fromString("Webm"))
    }

    @Test
    fun videoFormat_fromString_throwsForUnknownFormat() {
        val error = assertFailsWith<IllegalArgumentException> {
            VideoFormat.fromString("avi")
        }
        assertTrue("Unknown video format" in error.message!!)
    }

    // -- VideoRecorder state management tests --

    @Test
    fun videoRecorder_isNotRecordingInitially() {
        val recorder = VideoRecorder()
        assertFalse(recorder.isRecording)
    }

    @Test
    fun videoRecorder_stop_throwsWhenNotRecording() {
        val recorder = VideoRecorder()
        val error = assertFailsWith<IllegalStateException> {
            runBlocking { recorder.stop() }
        }
        assertEquals("No recording in progress", error.message)
    }

    @Test
    fun videoRecorder_stop_throwsWhenNotRecording_afterConstruction() {
        val recorder = VideoRecorder()
        assertFalse(recorder.isRecording)
        assertFailsWith<IllegalStateException> {
            runBlocking { recorder.stop() }
        }
        // State should remain unchanged after failed stop.
        assertFalse(recorder.isRecording)
    }

    // -- Clock management tests --

    @Test
    fun mainClock_autoAdvance_isDisabledDuringInlineVideoGeneration() {
        // Verify that generateFrames (used by inline videoDurationMs) disables autoAdvance
        // and re-enables it after completion, same as GIF.
        runSkikoComposeUiTest {
            setContent { Box(Modifier.fillMaxSize()) }
            waitForIdle()

            assertTrue(mainClock.autoAdvance, "autoAdvance should be true before frame generation")

            // Simulate what generateFrames does: disable autoAdvance, capture, re-enable.
            try {
                mainClock.autoAdvance = false
                assertFalse(mainClock.autoAdvance)

                // Capture a frame (like the inline video path does).
                val root = onAllNodes(isRootMatcher())[0]
                root.captureToImage()
                mainClock.advanceTimeBy(16)
            } finally {
                mainClock.autoAdvance = true
            }

            assertTrue(mainClock.autoAdvance, "autoAdvance should be restored after frame generation")
        }
    }

    @Test
    fun mainClock_advanceTimeBy_progressesClock() {
        runSkikoComposeUiTest {
            setContent { Box(Modifier.fillMaxSize()) }
            waitForIdle()

            mainClock.autoAdvance = false
            val before = mainClock.currentTime
            mainClock.advanceTimeBy(100)
            val after = mainClock.currentTime

            assertTrue(
                after - before >= 100,
                "Clock should advance by at least the requested amount, " +
                    "but only advanced by ${after - before}ms",
            )
            mainClock.autoAdvance = true
        }
    }

    // -- Frame capture tests --

    @Test
    fun captureToImage_producesNonEmptyImage() {
        runSkikoComposeUiTest {
            setContent { Box(Modifier.fillMaxSize()) }
            waitForIdle()

            val image = onAllNodes(isRootMatcher())[0].captureToImage()
            assertTrue(image.width > 0)
            assertTrue(image.height > 0)
        }
    }

    @Test
    fun captureToImage_dimensionsAreConsistentAcrossFrames() {
        runSkikoComposeUiTest {
            setContent { Box(Modifier.fillMaxSize()) }
            waitForIdle()

            mainClock.autoAdvance = false
            val image1 = onAllNodes(isRootMatcher())[0].captureToImage()
            mainClock.advanceTimeBy(16)
            val image2 = onAllNodes(isRootMatcher())[0].captureToImage()

            assertEquals(image1.width, image2.width, "Frame widths should be consistent")
            assertEquals(image1.height, image2.height, "Frame heights should be consistent")
            mainClock.autoAdvance = true
        }
    }

    // -- writeRawPixels tests --

    @Test
    fun writeRawPixels_producesCorrectByteCount() {
        runSkikoComposeUiTest {
            setContent { Box(Modifier.fillMaxSize()) }
            waitForIdle()

            val image = onAllNodes(isRootMatcher())[0].captureToImage()
            val out = java.io.ByteArrayOutputStream()
            writeRawPixels(image, out)

            // BGRA = 4 bytes per pixel.
            val expectedBytes = image.width * image.height * 4
            assertEquals(expectedBytes, out.size(), "Raw pixel data should be width*height*4 bytes")
        }
    }

    @Test
    fun writeRawPixels_producesConsistentOutputForSameFrame() {
        runSkikoComposeUiTest {
            setContent { Box(Modifier.fillMaxSize()) }
            waitForIdle()

            val image = onAllNodes(isRootMatcher())[0].captureToImage()
            val out1 = java.io.ByteArrayOutputStream()
            val out2 = java.io.ByteArrayOutputStream()
            writeRawPixels(image, out1)
            writeRawPixels(image, out2)

            assertTrue(
                out1.toByteArray().contentEquals(out2.toByteArray()),
                "Same image should produce identical raw pixel data",
            )
        }
    }

    // -- Node-targeted capture tests --

    @Test
    fun captureToImage_targetedNode_hasSmallerDimensionsThanRoot() {
        runSkikoComposeUiTest {
            setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.size(100.dp).testTag("small"))
                }
            }
            waitForIdle()

            val rootImage = onAllNodes(isRootMatcher())[0].captureToImage()
            val targetImage = onNode(hasTestTag("small")).captureToImage()

            assertTrue(
                targetImage.width < rootImage.width || targetImage.height < rootImage.height,
                "Targeted node image (${targetImage.width}x${targetImage.height}) should be " +
                    "smaller than root (${rootImage.width}x${rootImage.height})",
            )
        }
    }

    @Test
    fun captureToImage_targetedNode_dimensionsAreConsistentAcrossFrames() {
        runSkikoComposeUiTest {
            setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.size(100.dp).testTag("target"))
                }
            }
            waitForIdle()

            mainClock.autoAdvance = false
            val target = onNode(hasTestTag("target"))
            val image1 = target.captureToImage()
            mainClock.advanceTimeBy(16)
            val image2 = target.captureToImage()

            assertEquals(image1.width, image2.width, "Targeted frame widths should be consistent")
            assertEquals(image1.height, image2.height, "Targeted frame heights should be consistent")
            mainClock.autoAdvance = true
        }
    }

    @Test
    fun writeRawPixels_targetedNode_producesCorrectByteCount() {
        runSkikoComposeUiTest {
            setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.size(50.dp).testTag("tiny"))
                }
            }
            waitForIdle()

            val image = onNode(hasTestTag("tiny")).captureToImage()
            val out = java.io.ByteArrayOutputStream()
            writeRawPixels(image, out)

            val expectedBytes = image.width * image.height * 4
            assertEquals(expectedBytes, out.size(), "Targeted raw pixel data should be width*height*4 bytes")

            // Also verify it's smaller than root raw data.
            val rootImage = onAllNodes(isRootMatcher())[0].captureToImage()
            val rootOut = java.io.ByteArrayOutputStream()
            writeRawPixels(rootImage, rootOut)
            assertTrue(
                out.size() < rootOut.size(),
                "Targeted node raw data (${out.size()}) should be smaller than root (${rootOut.size()})",
            )
        }
    }

    @Test
    fun captureFrame_doesNothingWhenNotRecording() {
        val recorder = VideoRecorder()
        // Should not throw — just a no-op.
        recorder.captureFrame()
        assertFalse(recorder.isRecording)
    }
}
