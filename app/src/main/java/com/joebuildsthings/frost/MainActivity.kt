package com.joebuildsthings.frost

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.joebuildsthings.frost.ui.FrostApp
import com.joebuildsthings.frost.ui.theme.FrostTheme

class MainActivity : ComponentActivity() {

    private val vm: FrostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrimForOldDevices = Color.argb(0x66, 0x00, 0x00, 0x00)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, scrimForOldDevices),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, scrimForOldDevices)
        )
        setContent {
            FrostTheme {
                FrostApp(vm)
            }
        }
    }
}
