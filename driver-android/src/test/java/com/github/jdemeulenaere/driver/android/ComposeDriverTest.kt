package com.github.jdemeulenaere.driver.android

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jdemeulenaere.compose.driver.startComposeDriverServer
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w410dp-h920dp-xhdpi")
class ComposeDriverTest {
    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
    fun test() {
        // Only run the server in this test if `compose.driver.enabled` is true. This makes this
        // test be a no-op when running all tests in the project.
        val enabled = System.getProperty("compose.driver.enabled")?.toBoolean() ?: false
        Assume.assumeTrue("compose-driver is enabled", enabled)

        startComposeDriverServer {
            MaterialTheme {
                LookaheadScope {
                    var navigatedBack by remember { mutableStateOf(false) }
                    var clicked by remember { mutableStateOf(false) }
                    var longClicked by remember { mutableStateOf(false) }
                    var counter by remember { mutableStateOf(0) }

                    BackHandler(onBack = { navigatedBack = true })

                    Surface(
                        Modifier.fillMaxSize()
                            .combinedClickable(
                                onClick = { clicked = true },
                                onLongClick = { longClicked = true },
                            )
                    ) {
                        Column {
                            Text("backPressed: $navigatedBack")
                            Text("clicked: $clicked")
                            Text("longClicked: $longClicked")

                            if (counter % 2 == 1) {
                                Spacer(Modifier.weight(1f))
                            }

                            Button(
                                onClick = { counter++ },
                                Modifier.testTag("button")
                                    .animateBounds(lookaheadScope = this@LookaheadScope),
                            ) {
                                Text("Counter: $counter")
                            }
                        }
                    }
                }
            }
        }
    }
}
