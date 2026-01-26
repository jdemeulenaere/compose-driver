package com.github.jdemeulenaere.compose.driver.desktop

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.github.jdemeulenaere.compose.driver.startComposeDriverServer

fun main() {
    startComposeDriverServer {
        MaterialTheme {
            var navigateBack by remember { mutableStateOf(false) }
            var clicked by remember { mutableStateOf(false) }
            var longClicked by remember { mutableStateOf(false) }
            var counter by remember { mutableStateOf(0) }

            val backNavigationState =
                rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(backNavigationState, onBackCompleted = { navigateBack = true })

            Surface(
                Modifier.fillMaxSize()
                    .combinedClickable(
                        onClick = { clicked = true },
                        onLongClick = { longClicked = true },
                    )
            ) {
                Column {
                    Text("backPressed: $navigateBack")
                    Text("clicked: $clicked")
                    Text("longClicked: $longClicked")
                    Button(onClick = { counter++ }, Modifier.testTag("button")) {
                        Text("Counter: $counter")
                    }
                }
            }
        }
    }
}
