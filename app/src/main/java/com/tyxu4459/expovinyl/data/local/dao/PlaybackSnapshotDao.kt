package com.tyxu4459.expovinyl.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tyxu4459.expovinyl.data.local.entity.PlaybackSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackSnapshotDao {
  @Query("SELECT * FROM playback_snapshot WHERE id = 0")
  fun observeSnapshot(): Flow<PlaybackSnapshotEntity?>

  @Upsert
  suspend fun upsert(snapshot: PlaybackSnapshotEntity)

  @Query("DELETE FROM playback_snapshot WHERE id = 0")
  suspend fun clear()
}
