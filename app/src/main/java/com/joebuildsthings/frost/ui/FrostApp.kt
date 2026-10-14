package com.joebuildsthings.frost.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.joebuildsthings.frost.FrostViewModel
import com.joebuildsthings.frost.data.Track
import com.joebuildsthings.frost.ui.theme.Accent
import com.joebuildsthings.frost.ui.theme.AccentTint
import com.joebuildsthings.frost.ui.theme.Ink
import com.joebuildsthings.frost.ui.theme.InkMuted
import com.joebuildsthings.frost.ui.theme.glass
import com.joebuildsthings.frost.ui.theme.glassFlat

private val AUDIO_PERMISSION: String =
    if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun FrostApp(vm: FrostViewModel) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = hasAudioPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(granted) {
        if (granted && !vm.scanned && !vm.loading) vm.scan()
    }

    val subtitle = if (vm.scanned && vm.tracks.isNotEmpty()) {
        "${vm.tracks.size} songs on this phone"
    } else {
        "Your music, no strings."
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(subtitle)
            when {
                !granted -> StateCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.LibraryMusic,
                    title = "See your music here",
                    body = if (asked) {
                        "Access was denied. Allow music access in settings so Frost can build your library."
                    } else {
                        "Frost reads audio files stored on this phone to build your library."
                    }
                ) {
                    if (asked) {
                        PrimaryButton("Open settings") { openAppSettings(context) }
                    } else {
                        PrimaryButton("Allow access and scan") { launcher.launch(AUDIO_PERMISSION) }
                    }
                }

                vm.loading || !vm.scanned -> StateCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Scanning your library",
                    body = "Looking for songs on this phone."
                ) {
                    CircularProgressIndicator(
                        color = Accent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }

                vm.tracks.isEmpty() -> StateCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.MusicNote,
                    title = "No songs found",
                    body = "Add music to this phone, then scan again."
                ) {
                    PrimaryButton("Scan again") { vm.scan() }
                }

                else -> TrackList(
                    modifier = Modifier.weight(1f),
                    tracks = vm.tracks,
                    current = vm.current,
                    onPlay = vm::play
                )
            }
        }

        if (granted && vm.tracks.isNotEmpty()) {
            PlayerBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                track = vm.current,
                isPlaying = vm.isPlaying,
                onToggle = vm::togglePlay,
                onNext = vm::next,
                onPrevious = vm::previous
            )
        }
    }
}

@Composable
private fun Header(subtitle: String) {
    Column(
        Modifier
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
    ) {
        Text("Frost", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = InkMuted, fontSize = 14.sp)
    }
}

@Composable
private fun TrackList(
    modifier: Modifier,
    tracks: List<Track>,
    current: Track?,
    onPlay: (Track) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                active = track.id == current?.id,
                onClick = { onPlay(track) }
            )
        }
    }
}

@Composable
private fun TrackRow(track: Track, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassFlat()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) Accent else AccentTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (active) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = if (active) Color.White else Accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = InkMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(formatDuration(track.durationMs), color = InkMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PlayerBar(
    modifier: Modifier,
    track: Track?,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .glass(RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "NOW PLAYING",
                color = InkMuted,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                track?.title ?: "Pick a song",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track != null) {
                Text(
                    track.artist,
                    color = InkMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous song", tint = Ink)
        }
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Accent)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next song", tint = Ink)
        }
    }
}

@Composable
private fun StateCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
            }
            Text(
                title,
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                body,
                color = InkMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            content()
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
