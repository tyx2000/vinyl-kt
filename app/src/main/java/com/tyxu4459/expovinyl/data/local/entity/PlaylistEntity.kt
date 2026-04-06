package com.tyxu4459.expovinyl.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  @ColumnInfo(name = "created_at")
  val createdAt: Long,
)

