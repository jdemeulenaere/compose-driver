@file:OptIn(ExperimentalTestApi::class)

package io.github.jdemeulenaere.compose.driver

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.withKeysDown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for key event dispatching, including modifier key support. */
class KeyEventTest {

    @Test
    fun pressKey_sendsDownAndUpEvents() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput { pressKey(Key.A) }
        }

        val downEvents = events.filter { it.type == KeyEventType.KeyDown }
        val upEvents = events.filter { it.type == KeyEventType.KeyUp }
        assertEquals(1, downEvents.size, "Expected 1 KeyDown event")
        assertEquals(1, upEvents.size, "Expected 1 KeyUp event")
        assertEquals(Key.A, downEvents[0].key)
        assertEquals(Key.A, upEvents[0].key)
    }

    @Test
    fun keyDown_sendsOnlyDownEvent() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput { keyDown(Key.Enter) }
        }

        assertTrue(events.all { it.type == KeyEventType.KeyDown }, "Expected only KeyDown events")
        assertEquals(Key.Enter, events.first().key)
    }

    @Test
    fun keyUp_sendsOnlyUpEvent() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        val upEvents = events.filter { it.type == KeyEventType.KeyUp }
        assertEquals(1, upEvents.size)
        assertEquals(Key.Enter, upEvents[0].key)
    }

    @Test
    fun pressKey_withShiftModifier_reportsShiftPressed() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput {
                withKeysDown(listOf(Key.ShiftLeft)) { pressKey(Key.A) }
            }
        }

        // Find the KeyDown for A (not for the modifier itself)
        val keyADown = events.first { it.type == KeyEventType.KeyDown && it.key == Key.A }
        assertTrue(keyADown.isShiftPressed, "Shift should be reported as pressed during Key.A down")
    }

    @Test
    fun pressKey_withCtrlModifier_reportsCtrlPressed() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput {
                withKeysDown(listOf(Key.CtrlLeft)) { pressKey(Key.C) }
            }
        }

        val keyCDown = events.first { it.type == KeyEventType.KeyDown && it.key == Key.C }
        assertTrue(keyCDown.isCtrlPressed, "Ctrl should be reported as pressed during Key.C down")
    }

    @Test
    fun pressKey_withMultipleModifiers_reportsAllModifiersPressed() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput {
                withKeysDown(listOf(Key.CtrlLeft, Key.ShiftLeft)) { pressKey(Key.Delete) }
            }
        }

        val deleteDown =
            events.first { it.type == KeyEventType.KeyDown && it.key == Key.Delete }
        assertTrue(deleteDown.isCtrlPressed, "Ctrl should be pressed")
        assertTrue(deleteDown.isShiftPressed, "Shift should be pressed")
    }

    @Test
    fun pressKey_withoutModifiers_reportsNoModifiersPressed() {
        val events = runWithKeyRecorder { node ->
            node.performKeyInput { pressKey(Key.A) }
        }

        val keyADown = events.first { it.type == KeyEventType.KeyDown && it.key == Key.A }
        assertTrue(!keyADown.isShiftPressed, "Shift should not be pressed")
        assertTrue(!keyADown.isCtrlPressed, "Ctrl should not be pressed")
        assertTrue(!keyADown.isAltPressed, "Alt should not be pressed")
        assertTrue(!keyADown.isMetaPressed, "Meta should not be pressed")
    }

    @Test
    fun keyByName_resolvesLetterKeys() {
        val key = keyByName("A")
        assertEquals(Key.A, key)
    }

    @Test
    fun keyByName_resolvesSpecialKeys() {
        assertEquals(Key.Enter, keyByName("Enter"))
        assertEquals(Key.Escape, keyByName("Escape"))
        assertEquals(Key.Backspace, keyByName("Backspace"))
        assertEquals(Key.Tab, keyByName("Tab"))
        assertEquals(Key.Spacebar, keyByName("Spacebar"))
    }

    @Test
    fun keyByName_resolvesDirectionKeys() {
        assertEquals(Key.DirectionUp, keyByName("DirectionUp"))
        assertEquals(Key.DirectionDown, keyByName("DirectionDown"))
        assertEquals(Key.DirectionLeft, keyByName("DirectionLeft"))
        assertEquals(Key.DirectionRight, keyByName("DirectionRight"))
    }

    @Test
    fun keyByName_resolvesModifierKeys() {
        assertEquals(Key.ShiftLeft, keyByName("ShiftLeft"))
        assertEquals(Key.ShiftRight, keyByName("ShiftRight"))
        assertEquals(Key.CtrlLeft, keyByName("CtrlLeft"))
        assertEquals(Key.CtrlRight, keyByName("CtrlRight"))
        assertEquals(Key.AltLeft, keyByName("AltLeft"))
        assertEquals(Key.MetaLeft, keyByName("MetaLeft"))
    }

    @Test
    fun keyByName_resolvesFunctionKeys() {
        assertEquals(Key.F1, keyByName("F1"))
        assertEquals(Key.F12, keyByName("F12"))
    }

    @Test
    fun keyByName_throwsForUnknownKey() {
        try {
            keyByName("NonExistentKey")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown key"))
        }
    }

    /**
     * Sets up a focused composable that records all key events, runs the given [block] against
     * the node, and returns the recorded events.
     */
    private fun runWithKeyRecorder(
        block: (androidx.compose.ui.test.SemanticsNodeInteraction) -> Unit,
    ): List<KeyEvent> {
        val events = mutableStateListOf<KeyEvent>()
        runSkikoComposeUiTest {
            val focusRequester = FocusRequester()
            setContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("keyTarget")
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            events += event
                            true
                        }
                        .focusable()
                )
            }
            waitForIdle()
            runOnUiThread { focusRequester.requestFocus() }
            waitForIdle()

            block(onNode(hasTestTag("keyTarget")))
        }
        return events
    }
}
