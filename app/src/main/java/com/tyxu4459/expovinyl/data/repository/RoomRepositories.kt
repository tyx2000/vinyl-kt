package com.tyxu4459.expovinyl.data.repository

import android.net.Uri
import com.tyxu4459.expovinyl.data.local.dao.PlaybackSnapshotDao
import com.tyxu4459.expovinyl.data.local.dao.PlaylistDao
import com.tyxu4459.expovinyl.data.local.entity.PlaybackSnapshotEntity
import com.tyxu4459.expovinyl.data.local.entity.PlaylistEntity
import com.tyxu4459.expovinyl.data.local.entity.TrackEntity
import com.tyxu4459.expovinyl.model.ImportProgress
import com.tyxu4459.expovinyl.model.ImportTracksResult
import com.tyxu4459.expovinyl.model.PlayMode
import com.tyxu4459.expovinyl.model.PlaybackSnapshot
import com.tyxu4459.expovinyl.model.PlaylistDetail
import com.tyxu4459.expovinyl.model.PlaylistSummary
import com.tyxu4459.expovinyl.model.Track
import com.tyxu4459.expovinyl.platform.file.TrackImportManager
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class RoomLibraryRepository @Inject constructor(
  private val playlistDao: PlaylistDao,
  private val trackImportManager: TrackImportManager,
) : LibraryRepository {
  override fun observePlaylists(): Flow<List<PlaylistSummary>> =
    playlistDao.observePlaylistSummaries().map { rows ->
      rows.map { row ->
        PlaylistSummary(
          id = row.id,
          name = row.name,
          trackCount = row.trackCount,
        )
      }
    }

  override fun observePlaylistDetail(playlistId: String): Flow<PlaylistDetail?> =
    combine(
      playlistDao.observePlaylistTracks(playlistId),
      observePlaylists(),
    ) { tracks, playlists ->
      val playlist = playlists.firstOrNull { it.id == playlistId } ?: return@combine null
      PlaylistDetail(
        id = playlist.id,
        name = playlist.name,
        tracks = tracks.map { row ->
          Track(
            id = row.trackId,
            uri = row.uri,
            displayName = row.displayName,
            artist = row.artist,
            album = row.album,
            durationMs = row.durationMs,
            importedAt = row.importedAt,
          )
        },
      )
    }

  override suspend fun getPlaylistDetail(playlistId: String): PlaylistDetail? =
    observePlaylistDetail(playlistId).first()

  override suspend fun createPlaylist(name: String) {
    playlistDao.upsertPlaylist(
      PlaylistEntity(
        id = "p-${UUID.randomUUID()}",
        name = name.trim().ifBlank { "New Playlist" },
        createdAt = System.currentTimeMillis(),
      ),
    )
  }

  override suspend fun removePlaylist(playlistId: String) {
    val playlist = getPlaylistDetail(playlistId) ?: return
    playlist.tracks.forEach { track ->
      removeTrackFromPlaylist(playlistId, track.id)
    }
    playlistDao.deleteAllPlaylistTracks(playlistId)
    playlistDao.deletePlaylist(playlistId)
  }

  override suspend fun importTracks(
    playlistId: String,
    uris: List<Uri>,
    shouldCancel: (() -> Boolean)?,
    onProgress: ((ImportProgress) -> Unit)?,
  ): ImportTracksResult {
    val result =
      trackImportManager.importTracks(
        uris = uris,
        shouldCancel = shouldCancel,
        onProgress = onProgress,
      )

    result.tracks
      .sortedWith(
        compareBy<Track>({ it.displayName.lowercase(Locale.ROOT) }, { it.importedAt }),
      )
      .forEach { track ->
      addTrackToPlaylist(playlistId, track)
    }

    return result
  }

  override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
    if (
      playlistDao.playlistContainsSignature(
        playlistId = playlistId,
        displayName = track.displayName,
        durationMs = track.durationMs,
      )
    ) {
      deleteManagedFile(track.uri)
      return
    }

    val existingTrack =
      playlistDao.findTrackBySignature(
        displayName = track.displayName,
        durationMs = track.durationMs,
      )

    if (existingTrack != null) {
      playlistDao.insertPlaylistTrack(
        com.tyxu4459.expovinyl.data.local.entity.PlaylistTrackCrossRef(
          playlistId = playlistId,
          trackId = existingTrack.id,
          sortOrder = playlistDao.getNextSortOrder(playlistId),
        ),
      )
      if (existingTrack.uri != track.uri) {
        deleteManagedFile(track.uri)
      }
      return
    }

    playlistDao.addTrackToPlaylist(
      playlistId = playlistId,
      track = TrackEntity(
        uri = track.uri,
        displayName = track.displayName,
        artist = track.artist,
        album = track.album,
        durationMs = track.durationMs,
        importedAt = track.importedAt,
      ),
    )
  }

  override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
    val targetTrack = playlistDao.findPlaylistTrackById(playlistId, trackId) ?: return
    playlistDao.deletePlaylistTrack(playlistId, trackId)
    if (playlistDao.countTrackReferences(trackId) == 0) {
      playlistDao.deleteTrack(targetTrack)
      deleteManagedFile(targetTrack.uri)
    }
  }

  private fun deleteManagedFile(uri: String?) {
    if (uri.isNullOrBlank()) return
    val path = uri.removePrefix("file://")
    runCatching {
      File(path).takeIf { it.exists() }?.delete()
    }
  }
}

@Singleton
class RoomPlaybackRepository @Inject constructor(
  private val playbackSnapshotDao: PlaybackSnapshotDao,
) : PlaybackRepository {
  override fun observeSnapshot(): Flow<PlaybackSnapshot?> =
    playbackSnapshotDao.observeSnapshot().map { entity ->
      entity?.let {
        PlaybackSnapshot(
          playlistId = it.playlistId,
          trackId = it.trackId,
          queueIndex = it.queueIndex,
          positionMs = it.positionMs,
          isPlaying = it.isPlaying,
          playMode = PlayMode.valueOf(it.playMode),
          sleepTimerEndsAt = it.sleepTimerEndsAt,
        )
      }
    }

  override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
    playbackSnapshotDao.upsert(
      PlaybackSnapshotEntity(
        playlistId = snapshot.playlistId,
        trackId = snapshot.trackId,
        queueIndex = snapshot.queueIndex,
        positionMs = snapshot.positionMs,
        isPlaying = snapshot.isPlaying,
        playMode = snapshot.playMode.name,
        sleepTimerEndsAt = snapshot.sleepTimerEndsAt,
      ),
    )
  }

  override suspend fun clearSnapshot() {
    playbackSnapshotDao.clear()
  }
}
