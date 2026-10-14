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
import com.joebuildsthings.frost.ui.theme.GlassBackground

class MainActivity : ComponentActivity() {

    private val vm: FrostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            FrostTheme {
                GlassBackground {
                    FrostApp(vm)
                }
            }
        }
    }
}
