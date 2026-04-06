package com.tyxu4459.expovinyl.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_snapshot")
data class PlaybackSnapshotEntity(
  @PrimaryKey
  val id: Int = 0,
  @ColumnInfo(name = "playlist_id")
  val playlistId: String? = null,
  @ColumnInfo(name = "track_id")
  val trackId: Long? = null,
  @ColumnInfo(name = "queue_index")
  val queueIndex: Int = 0,
  @ColumnInfo(name = "position_ms")
  val positionMs: Long = 0L,
  @ColumnInfo(name = "is_playing")
  val isPlaying: Boolean = false,
  @ColumnInfo(name = "play_mode")
  val playMode: String = "LOOP",
  @ColumnInfo(name = "sleep_timer_ends_at")
  val sleepTimerEndsAt: Long? = null,
)

