package com.joebuildsthings.frost.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri
)
