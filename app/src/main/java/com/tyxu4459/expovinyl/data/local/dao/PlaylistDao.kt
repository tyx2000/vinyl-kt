package com.tyxu4459.expovinyl.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tyxu4459.expovinyl.data.local.entity.PlaylistEntity
import com.tyxu4459.expovinyl.data.local.entity.PlaylistTrackCrossRef
import com.tyxu4459.expovinyl.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistSummaryRow(
  val id: String,
  val name: String,
  val trackCount: Int,
)

data class PlaylistTrackRow(
  val playlistId: String,
  val playlistName: String,
  val trackId: Long,
  val uri: String,
  val displayName: String,
  val artist: String?,
  val album: String?,
  val durationMs: Long?,
  val importedAt: Long,
  val sortOrder: Int,
)

@Dao
interface PlaylistDao {
  @Query(
    """
    SELECT p.id, p.name, COUNT(pt.track_id) AS trackCount
    FROM playlists p
    LEFT JOIN playlist_tracks pt ON pt.playlist_id = p.id
    GROUP BY p.id, p.name, p.created_at
    ORDER BY p.created_at DESC
    """,
  )
  fun observePlaylistSummaries(): Flow<List<PlaylistSummaryRow>>

  @Query(
    """
    SELECT
      p.id AS playlistId,
      p.name AS playlistName,
      t.id AS trackId,
      t.uri AS uri,
      t.display_name AS displayName,
      t.artist AS artist,
      t.album AS album,
      t.duration_ms AS durationMs,
      t.imported_at AS importedAt,
      pt.sort_order AS sortOrder
    FROM playlists p
    JOIN playlist_tracks pt ON pt.playlist_id = p.id
    JOIN tracks t ON t.id = pt.track_id
    WHERE p.id = :playlistId
    ORDER BY pt.sort_order ASC, t.imported_at ASC
    """,
  )
  fun observePlaylistTracks(playlistId: String): Flow<List<PlaylistTrackRow>>

  @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
  suspend fun getPlaylist(playlistId: String): PlaylistEntity?

  @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
  suspend fun findPlaylistByName(name: String): PlaylistEntity?

  @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM playlist_tracks WHERE playlist_id = :playlistId")
  suspend fun getNextSortOrder(playlistId: String): Int

  @Upsert
  suspend fun upsertPlaylist(playlist: PlaylistEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTrack(track: TrackEntity): Long

  @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
  suspend fun findTrackByUri(uri: String): TrackEntity?

  @Query(
    """
    SELECT * FROM tracks
    WHERE display_name = :displayName
      AND COALESCE(duration_ms, -1) = COALESCE(:durationMs, -1)
    LIMIT 1
    """,
  )
  suspend fun findTrackBySignature(displayName: String, durationMs: Long?): TrackEntity?

  @Query(
    """
    SELECT t.* FROM tracks t
    JOIN playlist_tracks pt ON pt.track_id = t.id
    WHERE pt.playlist_id = :playlistId
      AND t.id = :trackId
    LIMIT 1
    """,
  )
  suspend fun findPlaylistTrackById(playlistId: String, trackId: Long): TrackEntity?

  @Query(
    """
    SELECT EXISTS(
      SELECT 1 FROM tracks t
      JOIN playlist_tracks pt ON pt.track_id = t.id
      WHERE pt.playlist_id = :playlistId
        AND t.display_name = :displayName
        AND COALESCE(t.duration_ms, -1) = COALESCE(:durationMs, -1)
    )
    """,
  )
  suspend fun playlistContainsSignature(
    playlistId: String,
    displayName: String,
    durationMs: Long?,
  ): Boolean

  @Query(
    """
    SELECT EXISTS(
      SELECT 1 FROM tracks t
      JOIN playlist_tracks pt ON pt.track_id = t.id
      WHERE pt.playlist_id = :playlistId
        AND t.uri = :uri
    )
    """,
  )
  suspend fun playlistContainsUri(
    playlistId: String,
    uri: String,
  ): Boolean

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

  @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId AND track_id = :trackId")
  suspend fun deletePlaylistTrack(playlistId: String, trackId: Long)

  @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId")
  suspend fun deleteAllPlaylistTracks(playlistId: String)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: String)

  @Query("SELECT COUNT(*) FROM playlist_tracks WHERE track_id = :trackId")
  suspend fun countTrackReferences(trackId: Long): Int

  @Delete
  suspend fun deleteTrack(track: TrackEntity)

  @Transaction
  suspend fun addTrackToPlaylist(
    playlistId: String,
    track: TrackEntity,
  ): Long {
    val trackId = insertTrack(track).takeIf { it > 0 } ?: findTrackByUri(track.uri)?.id ?: 0L
    insertPlaylistTrack(
      PlaylistTrackCrossRef(
        playlistId = playlistId,
        trackId = trackId,
        sortOrder = getNextSortOrder(playlistId),
      ),
    )
    return trackId
  }
}
