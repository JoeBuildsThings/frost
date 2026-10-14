package com.joebuildsthings.frost

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.joebuildsthings.frost.data.MediaStoreRepository
import com.joebuildsthings.frost.data.Track
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Frost"

class FrostViewModel(app: Application) : AndroidViewModel(app) {

    private val player: ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var scanned by mutableStateOf(false)
        private set
    var current by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                current = tracks.getOrNull(player.currentMediaItemIndex)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error", error)
            }
        })
    }

    fun scan() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            val found = try {
                withContext(Dispatchers.IO) { MediaStoreRepository.loadTracks(getApplication()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Library scan failed", e)
                emptyList()
            }
            tracks = found
            current = null
            player.setMediaItems(
                found.map { MediaItem.Builder().setMediaId(it.id.toString()).setUri(it.uri).build() }
            )
            player.prepare()
            scanned = true
            loading = false
        }
    }

    fun play(track: Track) {
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        current = track
        player.seekTo(index, 0L)
        player.play()
    }

    fun togglePlay() {
        if (tracks.isEmpty()) return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (current == null) current = tracks.first()
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0L)
            player.play()
        }
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0L)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
