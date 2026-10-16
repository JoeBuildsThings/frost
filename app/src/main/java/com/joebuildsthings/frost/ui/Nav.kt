package com.joebuildsthings.frost.ui

sealed interface DetailSource {
    data class Album(val id: Long) : DetailSource
    data class Artist(val name: String) : DetailSource
    data class Folder(val path: String) : DetailSource
    data class Playlist(val id: String) : DetailSource
    data object Favorites : DetailSource
}

sealed interface Overlay {
    data class Detail(val source: DetailSource) : Overlay
    data object Search : Overlay
    data object Settings : Overlay
    data object NowPlaying : Overlay
}
