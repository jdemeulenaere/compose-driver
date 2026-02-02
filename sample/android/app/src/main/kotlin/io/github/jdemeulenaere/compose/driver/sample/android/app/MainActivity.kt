package io.github.jdemeulenaere.compose.driver.sample.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jdemeulenaere.compose.driver.sample.android.AndroidApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AndroidApplication() }
    }
}
