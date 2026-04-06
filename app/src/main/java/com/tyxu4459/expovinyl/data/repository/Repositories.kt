package com.tyxu4459.expovinyl.data.repository

import android.net.Uri
import com.tyxu4459.expovinyl.model.ImportProgress
import com.tyxu4459.expovinyl.model.ImportTracksResult
import com.tyxu4459.expovinyl.model.PlaybackSnapshot
import com.tyxu4459.expovinyl.model.PlaylistDetail
import com.tyxu4459.expovinyl.model.PlaylistSummary
import com.tyxu4459.expovinyl.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
  fun observePlaylists(): Flow<List<PlaylistSummary>>
  fun observePlaylistDetail(playlistId: String): Flow<PlaylistDetail?>
  suspend fun getPlaylistDetail(playlistId: String): PlaylistDetail?
  suspend fun createPlaylist(name: String)
  suspend fun removePlaylist(playlistId: String)
  suspend fun importTracks(
    playlistId: String,
    uris: List<Uri>,
    shouldCancel: (() -> Boolean)? = null,
    onProgress: ((ImportProgress) -> Unit)? = null,
  ): ImportTracksResult
  suspend fun addTrackToPlaylist(playlistId: String, track: Track)
  suspend fun removeTrackFromPlaylist(playlistId: String, trackId: Long)
}

interface PlaybackRepository {
  fun observeSnapshot(): Flow<PlaybackSnapshot?>
  suspend fun saveSnapshot(snapshot: PlaybackSnapshot)
  suspend fun clearSnapshot()
}
