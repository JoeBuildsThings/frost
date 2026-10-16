package com.joebuildsthings.frost

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.joebuildsthings.frost.data.AlbumItem
import com.joebuildsthings.frost.data.ArtistItem
import com.joebuildsthings.frost.data.FolderItem
import com.joebuildsthings.frost.data.MediaStoreRepository
import com.joebuildsthings.frost.data.Playlist
import com.joebuildsthings.frost.data.Track
import com.joebuildsthings.frost.data.UserData
import com.joebuildsthings.frost.data.UserStore
import com.joebuildsthings.frost.playback.PlaybackService
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Frost"

private class Library(
    val tracks: List<Track>,
    val albums: List<AlbumItem>,
    val artists: List<ArtistItem>,
    val folders: List<FolderItem>,
    val allFolders: List<FolderItem>
)

class FrostViewModel(app: Application) : AndroidViewModel(app) {

    private val store = UserStore(app)
    private val ioSerial = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val userReady = CompletableDeferred<Unit>()
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var controller: MediaController? = null
    private var rawTracks: List<Track> = emptyList()
    private var byId: Map<Long, Track> = emptyMap()
    private var sleepJob: Job? = null

    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var albums by mutableStateOf<List<AlbumItem>>(emptyList())
        private set
    var artists by mutableStateOf<List<ArtistItem>>(emptyList())
        private set
    var folders by mutableStateOf<List<FolderItem>>(emptyList())
        private set
    var allFolders by mutableStateOf<List<FolderItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var scanned by mutableStateOf(false)
        private set

    var favorites by mutableStateOf<Set<Long>>(emptySet())
        private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var excluded by mutableStateOf<Set<String>>(emptySet())
        private set
    var minSeconds by mutableStateOf(30)
        private set

    var current by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
        private set
    var queue by mutableStateOf<List<Track>>(emptyList())
        private set
    var queueIndex by mutableStateOf(0)
        private set
    var sleepEndsAt by mutableStateOf(0L)
        private set
    var sleepMinutes by mutableStateOf(0)
        private set

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) rebuildQueue(player)
            syncPlayer(player)
        }
    }

    init {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { store.load() }
            favorites = data.favorites
            playlists = data.playlists
            excluded = data.excluded
            minSeconds = data.minSeconds
            userReady.complete(Unit)
        }
        connect()
    }

    private fun connect() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                rebuildQueue(c)
                syncPlayer(c)
                pending.forEach { it(c) }
                pending.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Player connection failed", e)
            }
        }, ContextCompat.getMainExecutor(app))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending.add(block)
    }

    private fun rebuildQueue(player: Player) {
        val list = ArrayList<Track>(player.mediaItemCount)
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val id = item.mediaId.toLongOrNull()
            val known = if (id != null) byId[id] else null
            list.add(
                known ?: Track(
                    id = id ?: -1L,
                    title = item.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = item.mediaMetadata.artist?.toString() ?: "",
                    album = "",
                    albumId = 0L,
                    trackNo = 0,
                    durationMs = 0L,
                    folder = "",
                    uri = Uri.EMPTY
                )
            )
        }
        queue = list
    }

    private fun syncPlayer(player: Player) {
        isPlaying = player.isPlaying
        shuffle = player.shuffleModeEnabled
        repeatMode = player.repeatMode
        queueIndex = player.currentMediaItemIndex
        current = queue.getOrNull(player.currentMediaItemIndex)
    }

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()

    // ---------- library ----------

    fun scan() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            userReady.await()
            val raw = try {
                withContext(Dispatchers.IO) { MediaStoreRepository.loadTracks(getApplication()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Library scan failed", e)
                emptyList<Track>()
            }
            rawTracks = raw
            byId = raw.associateBy { it.id }
            val ex = excluded
            val min = minSeconds
            applyLibrary(withContext(Dispatchers.Default) { buildLibrary(raw, ex, min) })
            controller?.let {
                rebuildQueue(it)
                syncPlayer(it)
            }
            scanned = true
            loading = false
        }
    }

    private fun refilter() {
        val ex = excluded
        val min = minSeconds
        viewModelScope.launch {
            applyLibrary(withContext(Dispatchers.Default) { buildLibrary(rawTracks, ex, min) })
        }
    }

    private fun applyLibrary(lib: Library) {
        tracks = lib.tracks
        albums = lib.albums
        artists = lib.artists
        folders = lib.folders
        allFolders = lib.allFolders
    }

    private fun buildLibrary(raw: List<Track>, excluded: Set<String>, minSeconds: Int): Library {
        val minMs = minSeconds * 1000L
        val kept = raw
            .filter { it.durationMs >= minMs && it.folder !in excluded }
            .sortedBy { it.title.lowercase() }

        val albums = kept.groupBy { it.albumId }.map { (id, list) ->
            val sorted = list.sortedWith(compareBy<Track>({ it.trackNo }, { it.title.lowercase() }))
            val names = sorted.map { it.artist }.distinct()
            AlbumItem(id, sorted.first().album, if (names.size > 1) "Various artists" else names.first(), sorted)
        }.sortedBy { it.name.lowercase() }

        val artists = kept.groupBy { it.artist }.map { (name, list) ->
            ArtistItem(name, list.map { it.albumId }.distinct().size, list)
        }.sortedBy { it.name.lowercase() }

        fun folderItems(src: List<Track>): List<FolderItem> =
            src.groupBy { it.folder }.map { (path, list) ->
                FolderItem(
                    path,
                    path.substringAfterLast('/').ifEmpty { "Root" },
                    list.sortedBy { it.title.lowercase() }
                )
            }.sortedBy { it.name.lowercase() }

        return Library(
            kept,
            albums,
            artists,
            folderItems(kept),
            folderItems(raw.filter { it.durationMs >= minMs })
        )
    }

    // ---------- playback ----------

    fun play(list: List<Track>, startIndex: Int) {
        if (list.isEmpty()) return
        withController { c ->
            c.shuffleModeEnabled = false
            c.setMediaItems(list.map { it.toMediaItem() }, startIndex.coerceIn(0, list.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun shuffleAll(list: List<Track>) {
        if (list.isEmpty()) return
        withController { c ->
            c.shuffleModeEnabled = true
            c.setMediaItems(list.map { it.toMediaItem() }, list.indices.random(), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playNext(track: Track) {
        withController { c ->
            if (c.mediaItemCount == 0) {
                play(listOf(track), 0)
            } else {
                c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem())
            }
        }
    }

    fun addToQueue(track: Track) {
        withController { c ->
            if (c.mediaItemCount == 0) {
                play(listOf(track), 0)
            } else {
                c.addMediaItem(track.toMediaItem())
            }
        }
    }

    fun jumpTo(index: Int) {
        withController { c ->
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else if (c.mediaItemCount == 0) {
            play(tracks, 0)
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun positionMs(): Long = controller?.currentPosition ?: 0L

    fun durationMs(): Long {
        val d = controller?.duration ?: C.TIME_UNSET
        return if (d == C.TIME_UNSET || d < 0L) 0L else d
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepMinutes = minutes
        if (minutes <= 0) {
            sleepEndsAt = 0L
            return
        }
        val ms = minutes * 60_000L
        sleepEndsAt = System.currentTimeMillis() + ms
        sleepJob = viewModelScope.launch {
            delay(ms)
            controller?.pause()
            sleepEndsAt = 0L
            sleepMinutes = 0
        }
    }

    // ---------- favorites, playlists, settings ----------

    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        persist()
    }

    fun createPlaylist(name: String, firstTrackId: Long? = null): String {
        val clean = name.trim().ifEmpty { "New playlist" }
        val p = Playlist(UUID.randomUUID().toString(), clean, listOfNotNull(firstTrackId))
        playlists = playlists + p
        persist()
        return p.id
    }

    fun renamePlaylist(id: String, name: String) {
        val clean = name.trim().ifEmpty { return }
        playlists = playlists.map { if (it.id == id) it.copy(name = clean) else it }
        persist()
    }

    fun deletePlaylist(id: String) {
        playlists = playlists.filter { it.id != id }
        persist()
    }

    fun addToPlaylist(playlistId: String, trackId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
        }
        persist()
    }

    fun removeFromPlaylist(playlistId: String, trackId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds.filter { id -> id != trackId }) else it
        }
        persist()
    }

    fun updateMinSeconds(seconds: Int) {
        minSeconds = seconds
        persist()
        refilter()
    }

    fun setFolderHidden(path: String, hidden: Boolean) {
        excluded = if (hidden) excluded + path else excluded - path
        persist()
        refilter()
    }

    private fun persist() {
        val snapshot = UserData(favorites, playlists, excluded, minSeconds)
        viewModelScope.launch(ioSerial) { store.save(snapshot) }
    }

    override fun onCleared() {
        sleepJob?.cancel()
        controller?.release()
        ioSerial.close()
        super.onCleared()
    }
}
