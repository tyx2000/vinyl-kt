package com.tyxu4459.expovinyl.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "tracks",
  indices = [Index(value = ["uri"], unique = true)],
)
data class TrackEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val uri: String,
  @ColumnInfo(name = "display_name")
  val displayName: String,
  val artist: String? = null,
  val album: String? = null,
  @ColumnInfo(name = "duration_ms")
  val durationMs: Long? = null,
  @ColumnInfo(name = "imported_at")
  val importedAt: Long,
)

