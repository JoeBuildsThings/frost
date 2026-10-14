package com.joebuildsthings.frost.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object MediaStoreRepository {

    fun loadTracks(context: Context): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        val found = ArrayList<Track>()

        context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artist = cursor.getString(artistCol)
                found.add(
                    Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Untitled",
                        artist = if (artist.isNullOrBlank() || artist == MediaStore.UNKNOWN_STRING) {
                            "Unknown artist"
                        } else {
                            artist
                        },
                        album = cursor.getString(albumCol) ?: "",
                        durationMs = cursor.getLong(durationCol),
                        uri = ContentUris.withAppendedId(collection, id)
                    )
                )
            }
        }
        return found.sortedBy { it.title.lowercase() }
    }
}
