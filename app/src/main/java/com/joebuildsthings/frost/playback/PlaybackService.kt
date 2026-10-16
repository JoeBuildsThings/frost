package com.joebuildsthings.frost.playback

import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var exo: ExoPlayer? = null

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == PlaybackPrefs.KEEP_PLAYING) {
            val keep = prefs.getBoolean(PlaybackPrefs.KEEP_PLAYING, true)
            exo?.setAudioAttributes(audioAttributes, !keep)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(PlaybackPrefs.FILE, MODE_PRIVATE)
        val keep = prefs.getBoolean(PlaybackPrefs.KEEP_PLAYING, true)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, !keep)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo = player
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        session = MediaSession.Builder(this, player)
            .setCallback(ResolveCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        getSharedPreferences(PlaybackPrefs.FILE, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        exo = null
        super.onDestroy()
    }
}

private class ResolveCallback : MediaSession.Callback {
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val resolved = mediaItems.map { item ->
            val id = item.mediaId.toLongOrNull()
            if (item.localConfiguration == null && id != null) {
                item.buildUpon()
                    .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
                    .build()
            } else {
                item
            }
        }.toMutableList()
        return Futures.immediateFuture(resolved)
    }
}
