package com.joebuildsthings.frost.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.frost.FrostViewModel
import com.joebuildsthings.frost.ui.theme.Accent
import com.joebuildsthings.frost.ui.theme.AccentTint
import com.joebuildsthings.frost.ui.theme.Ink
import com.joebuildsthings.frost.ui.theme.InkMuted
import com.joebuildsthings.frost.ui.theme.glass
import com.joebuildsthings.frost.ui.theme.glassFlat

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun SettingsScreen(vm: FrostViewModel, bottomPad: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(14.dp))
            Text("Settings", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "library") {
                SettingsCard("Library") {
                    Text("Hide songs shorter than", color = InkMuted, fontSize = 13.sp)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("Show all", vm.minSeconds == 0) { vm.setMinSeconds(0) }
                        Chip("30 seconds", vm.minSeconds == 30) { vm.setMinSeconds(30) }
                        Chip("60 seconds", vm.minSeconds == 60) { vm.setMinSeconds(60) }
                    }
                    Spacer(Modifier.height(2.dp))
                    GhostButton("Scan again", { vm.scan() }, Modifier.fillMaxWidth())
                }
            }
            item(key = "sleep") {
                SettingsCard("Sleep timer") {
                    Text("Pause playback after a set time.", color = InkMuted, fontSize = 13.sp)
                    SleepChips(vm)
                }
            }
            item(key = "folders_head") {
                SettingsCard("Hidden folders") {
                    Text(
                        "Turn a folder on to keep its songs out of your library, such as voice notes or call recordings.",
                        color = InkMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            items(vm.allFolders, key = { "f_" + it.path }) { folder ->
                val hidden = folder.path in vm.excluded
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glassFlat()
                        .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBlock(FrostIcons.Folder, Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            folder.name,
                            color = Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("${folder.tracks.size} songs", color = InkMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = hidden,
                        onCheckedChange = { vm.setFolderHidden(folder.path, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = AccentTint
                        )
                    )
                }
            }
            item(key = "about") {
                SettingsCard("About") {
                    Text("Frost $version", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Frost plays songs stored on this phone. It has no internet permission, and your favorites and playlists stay on this device.",
                        color = InkMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
