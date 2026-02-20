package io.github.jdemeulenaere.compose.driver.sample.multiplatform

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.testTag
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
private fun MultiplatformApplication() {
    MultiplatformApplication(name = "MultiplatformApplication")
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun MultiplatformApplication(name: String) {
    MaterialTheme {
        LookaheadScope {
            Scaffold { padding ->
                var navigatedBack by remember { mutableStateOf(false) }
                var clicked by remember { mutableStateOf(false) }
                var longClicked by remember { mutableStateOf(false) }
                var counter by remember { mutableIntStateOf(0) }

                val backNavigationState =
                    rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
                NavigationBackHandler(
                    backNavigationState,
                    onBackCompleted = { navigatedBack = true },
                )

                Surface(
                    Modifier.fillMaxSize()
                        .combinedClickable(
                            onClick = { clicked = true },
                            onLongClick = { longClicked = true },
                        )
                ) {
                    Column(Modifier.padding(padding)) {
                        Text("Hello World from $name !")
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

                        var showDropdown by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { showDropdown = !showDropdown },
                                Modifier.testTag("dropdownToggle"),
                            ) {
                                Text("Toggle Dropdown")
                            }
                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Option 1") },
                                    onClick = { showDropdown = false },
                                    modifier = Modifier.testTag("dropdownOption1"),
                                )
                                DropdownMenuItem(
                                    text = { Text("Option 2") },
                                    onClick = { showDropdown = false },
                                    modifier = Modifier.testTag("dropdownOption2"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
