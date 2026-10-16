package com.joebuildsthings.frost.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.joebuildsthings.frost.FrostViewModel
import com.joebuildsthings.frost.ui.pages.AlbumsPage
import com.joebuildsthings.frost.ui.pages.ArtistsPage
import com.joebuildsthings.frost.ui.pages.FoldersPage
import com.joebuildsthings.frost.ui.pages.PlaylistsPage
import com.joebuildsthings.frost.ui.pages.SongsPage
import com.joebuildsthings.frost.ui.theme.Accent
import com.joebuildsthings.frost.ui.theme.AccentTint
import com.joebuildsthings.frost.ui.theme.BarHazeStyle
import com.joebuildsthings.frost.ui.theme.Ink
import com.joebuildsthings.frost.ui.theme.InkMuted
import com.joebuildsthings.frost.ui.theme.glassEdge
import com.joebuildsthings.frost.ui.theme.glowBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val AUDIO_PERMISSION: String =
    if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

private val TabLabels = listOf("Songs", "Albums", "Artists", "Folders", "Playlists")

@Composable
fun FrostApp(vm: FrostViewModel) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermission(context, AUDIO_PERMISSION)) }
    var asked by remember { mutableStateOf(false) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LifecycleResumeEffect(Unit) {
        granted = hasPermission(context, AUDIO_PERMISSION)
        onPauseOrDispose { }
    }

    LaunchedEffect(granted) {
        if (granted && !vm.scanned) vm.scan()
        if (granted && Build.VERSION.SDK_INT >= 33 && !askedNotifications &&
            !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            askedNotifications = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    val stack = remember { mutableStateListOf<Overlay>() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var barsHeight by remember { mutableStateOf(0.dp) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val top = stack.lastOrNull()
    val push: (Overlay) -> Unit = { stack.add(it) }
    val pop: () -> Unit = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    val barsVisible = granted && top !is Overlay.NowPlaying && top !is Overlay.Search
    val bottomPad = if (barsVisible) barsHeight + 12.dp else navBottom + 16.dp

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .glowBackground()
        ) {
            if (!granted) {
                PermissionGate(
                    asked = asked,
                    onAllow = { audioLauncher.launch(AUDIO_PERMISSION) },
                    onSettings = { openAppSettings(context) }
                )
            } else {
                Shell(vm, tab, bottomPad, push)
                if (top != null) {
                    OverlayHost(top, vm, bottomPad, push, pop)
                }
            }
        }

        if (barsVisible) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { barsHeight = with(density) { it.height.toDp() } }
            ) {
                MiniPlayer(vm, hazeState, padNav = top != null) {
                    if (top !is Overlay.NowPlaying) push(Overlay.NowPlaying)
                }
                if (top == null) {
                    TabBar(tab, hazeState) { tab = it }
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(asked: Boolean, onAllow: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Songs", "Your music, no strings.", null, null)
        StateCard(
            modifier = Modifier.weight(1f),
            icon = FrostIcons.MusicNote,
            title = "See your music here",
            body = if (asked) {
                "Access was denied. Allow music access in settings so Frost can build your library."
            } else {
                "Frost reads audio files stored on this phone to build your library."
            }
        ) {
            if (asked) {
                PrimaryButton("Open settings", onSettings)
            } else {
                PrimaryButton("Allow access and scan", onAllow)
            }
        }
    }
}

@Composable
private fun Shell(vm: FrostViewModel, tab: Int, bottomPad: Dp, onPush: (Overlay) -> Unit) {
    val songsState = rememberLazyListState()
    val albumsState = rememberLazyGridState()
    val artistsState = rememberLazyListState()
    val foldersState = rememberLazyListState()
    val playlistsState = rememberLazyListState()

    val subtitle = when (tab) {
        0 -> "${vm.tracks.size} songs on this phone"
        1 -> "${vm.albums.size} albums"
        2 -> "${vm.artists.size} artists"
        3 -> "${vm.folders.size} folders"
        else -> "${vm.playlists.size} playlists"
    }

    Column(Modifier.fillMaxSize()) {
        Header(
            TabLabels[tab],
            if (vm.scanned) subtitle else "Your music, no strings.",
            { onPush(Overlay.Search) },
            { onPush(Overlay.Settings) }
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!vm.scanned) {
                StateCard(
                    modifier = Modifier.fillMaxSize(),
                    icon = FrostIcons.MusicNote,
                    title = "Scanning your library",
                    body = "Looking for songs on this phone."
                ) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                }
            } else {
                when (tab) {
                    0 -> SongsPage(vm, songsState, bottomPad)
                    1 -> AlbumsPage(vm, albumsState, bottomPad, onPush)
                    2 -> ArtistsPage(vm, artistsState, bottomPad, onPush)
                    3 -> FoldersPage(vm, foldersState, bottomPad, onPush)
                    else -> PlaylistsPage(vm, playlistsState, bottomPad, onPush)
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, onSearch: (() -> Unit)?, onSettings: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("FROST", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = InkMuted, fontSize = 13.sp)
        }
        if (onSearch != null) IconCircle(Icons.Rounded.Search, "Search", onSearch)
        if (onSearch != null && onSettings != null) Spacer(Modifier.width(8.dp))
        if (onSettings != null) IconCircle(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

@Composable
private fun OverlayHost(
    overlay: Overlay,
    vm: FrostViewModel,
    bottomPad: Dp,
    onPush: (Overlay) -> Unit,
    onBack: () -> Unit
) {
    Box(Modifier.fillMaxSize().glowBackground()) {
        when (overlay) {
            is Overlay.Detail -> DetailScreen(overlay.source, vm, bottomPad, onBack)
            Overlay.Search -> SearchScreen(vm, bottomPad, onBack, onPush)
            Overlay.Settings -> SettingsScreen(vm, bottomPad, onBack)
            Overlay.NowPlaying -> NowPlayingScreen(vm, onBack)
        }
    }
}

@Composable
private fun TabBar(selected: Int, hazeState: HazeState, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val icons = listOf<ImageVector>(
        FrostIcons.MusicNote,
        FrostIcons.Album,
        Icons.Rounded.Person,
        FrostIcons.Folder,
        FrostIcons.QueueMusic
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeEffect(state = hazeState, style = BarHazeStyle)
            .glassEdge(shape)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        TabLabels.forEachIndexed { index, label ->
            val active = index == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) AccentTint else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icons[index],
                    contentDescription = label,
                    tint = if (active) Accent else InkMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    label,
                    color = if (active) Accent else InkMuted,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(vm: FrostViewModel, hazeState: HazeState, padNav: Boolean, onOpen: () -> Unit) {
    val track = vm.current ?: return
    val shape = RoundedCornerShape(24.dp)
    Row(
        (if (padNav) Modifier.navigationBarsPadding() else Modifier)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
            .hazeEffect(state = hazeState, style = BarHazeStyle)
            .glassEdge(shape)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Art(track.albumId, 128, Modifier.size(46.dp), 14.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                track.title,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
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
        IconButton(onClick = { vm.previous() }) {
            Icon(FrostIcons.SkipPrevious, contentDescription = "Previous song", tint = Ink)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Accent)
                .clickable { vm.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (vm.isPlaying) FrostIcons.Pause else FrostIcons.Play,
                contentDescription = if (vm.isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = { vm.next() }) {
            Icon(FrostIcons.SkipNext, contentDescription = "Next song", tint = Ink)
        }
    }
}
