package io.github.jdemeulenaere.compose.driver.sample.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

fun main() {
    singleWindowApplication(WindowState(width = 410.dp, height = 920.dp)) { DesktopApplication() }
}
