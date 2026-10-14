package com.joebuildsthings.frost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.frost.ui.theme.AccentCyan
import com.joebuildsthings.frost.ui.theme.AccentCyanDim
import com.joebuildsthings.frost.ui.theme.BgDeep
import com.joebuildsthings.frost.ui.theme.FrostTheme
import com.joebuildsthings.frost.ui.theme.GlassBorder
import com.joebuildsthings.frost.ui.theme.GlassBorderSoft
import com.joebuildsthings.frost.ui.theme.GlassFill
import com.joebuildsthings.frost.ui.theme.GlassFillStrong
import com.joebuildsthings.frost.ui.theme.InkBright
import com.joebuildsthings.frost.ui.theme.InkMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrostTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDeep
                ) {
                    FrostHome()
                }
            }
        }
    }
}

@Composable
private fun FrostHome() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Frost",
                color = InkBright,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your music, no strings.",
                color = InkMuted,
                fontSize = 13.sp
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassFillStrong)
                        .border(1.dp, GlassBorderSoft, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.LibraryMusic,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    "Library empty",
                    color = InkBright,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Scanning arrives in the next build.",
                    color = InkMuted,
                    fontSize = 12.sp
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorderSoft, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Now playing", color = InkMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Nothing yet", color = InkBright, fontSize = 14.sp)
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentCyanDim)
                        .border(1.dp, AccentCyan, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(">", color = AccentCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
