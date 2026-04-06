package com.tyxu4459.expovinyl.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
  tableName = "playlist_tracks",
  primaryKeys = ["playlist_id", "track_id"],
)
data class PlaylistTrackCrossRef(
  @ColumnInfo(name = "playlist_id")
  val playlistId: String,
  @ColumnInfo(name = "track_id")
  val trackId: Long,
  @ColumnInfo(name = "sort_order")
  val sortOrder: Int,
)

