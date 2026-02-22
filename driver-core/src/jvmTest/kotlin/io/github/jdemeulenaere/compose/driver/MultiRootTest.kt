@file:OptIn(ExperimentalTestApi::class)

package io.github.jdemeulenaere.compose.driver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot as isRootMatcher
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.window.Popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests verifying that the compose-driver multi-root handling works correctly when a Popup is
 * displayed (which creates a second semantic root node).
 */
class MultiRootTest {

    @Test
    fun multipleRootsExist_whenPopupIsOpen() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }
            waitForIdle()

            val roots = onAllNodes(isRootMatcher()).fetchSemanticsNodes()
            assertTrue(
                roots.size >= 2,
                "Expected at least 2 roots when a Popup is open, but found ${roots.size}",
            )
        }
    }

    @Test
    fun printAllRoots_containsBothMainAndPopupContent() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }
            waitForIdle()

            val allRoots = onAllNodes(isRootMatcher())
            val output = allRoots.printToString(maxDepth = Int.MAX_VALUE)

            assertTrue(
                "mainContent" in output,
                "All-roots printToString should contain mainContent tag.\nOutput:\n$output",
            )
            assertTrue(
                "popupContent" in output,
                "All-roots printToString should contain popupContent tag.\nOutput:\n$output",
            )
        }
    }

    @Test
    fun firstRoot_canBeUsedForScreenshot() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }
            waitForIdle()

            // This is what resolveNode(null) does — get the first root via onAllNodes(isRoot())[0].
            // It should not crash even with multiple roots.
            val firstRoot = onAllNodes(isRootMatcher())[0]
            val image = firstRoot.captureToImage()
            assertTrue(image.width > 0, "Screenshot should produce a non-empty image")
            assertTrue(image.height > 0, "Screenshot should produce a non-empty image")
        }
    }

    @Test
    fun nodeSelection_findsNodeInPopup() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }
            waitForIdle()

            // onNode(hasTestTag(...)) searches across ALL roots, so it should find nodes
            // inside the popup's root.
            val node = onNode(hasTestTag("popupContent")).fetchSemanticsNode()
            assertTrue(
                node.config.any { it.key.name == "TestTag" && it.value == "popupContent" },
                "Should find popupContent node inside popup root",
            )
        }
    }

    @Test
    fun nodeSelection_findsNodeInMainContent() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }
            waitForIdle()

            val node = onNode(hasTestTag("mainContent")).fetchSemanticsNode()
            assertTrue(
                node.config.any { it.key.name == "TestTag" && it.value == "mainContent" },
                "Should find mainContent node in main root",
            )
        }
    }

    @Test
    fun firstRoot_printToString_succeeds() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }
            waitForIdle()

            // This is what /printTree?nodeTag=... does — uses onNode(matcher) which searches
            // all roots. Without a tag, the driver uses onAllNodes(isRoot())[0].
            val firstRoot = onAllNodes(isRootMatcher())[0]
            val output = firstRoot.printToString()
            assertTrue(output.isNotEmpty(), "First root printToString should produce output")
        }
    }

    @Test
    fun waitForIdle_withMultipleRoots_succeeds() {
        runSkikoComposeUiTest {
            setContent { TestContentWithPopup() }

            // waitForIdle should work without crashing even with multiple roots.
            waitForIdle()

            // Verify the tree is intact.
            val roots = onAllNodes(isRootMatcher()).fetchSemanticsNodes()
            assertTrue(roots.isNotEmpty(), "Should have at least one root after waitForIdle")
        }
    }
}

@Composable
private fun TestContentWithPopup() {
    Box(Modifier.fillMaxSize().testTag("mainContent")) {
        BasicText("Main content")
        Popup {
            Box(Modifier.testTag("popupContent")) {
                BasicText("Popup content")
            }
        }
    }
}
