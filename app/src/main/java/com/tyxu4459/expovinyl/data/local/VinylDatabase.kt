package com.tyxu4459.expovinyl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tyxu4459.expovinyl.data.local.dao.PlaybackSnapshotDao
import com.tyxu4459.expovinyl.data.local.dao.PlaylistDao
import com.tyxu4459.expovinyl.data.local.entity.PlaybackSnapshotEntity
import com.tyxu4459.expovinyl.data.local.entity.PlaylistEntity
import com.tyxu4459.expovinyl.data.local.entity.PlaylistTrackCrossRef
import com.tyxu4459.expovinyl.data.local.entity.TrackEntity

@Database(
  entities = [
    PlaylistEntity::class,
    TrackEntity::class,
    PlaylistTrackCrossRef::class,
    PlaybackSnapshotEntity::class,
  ],
  version = 1,
  exportSchema = true,
)
abstract class VinylDatabase : RoomDatabase() {
  abstract fun playlistDao(): PlaylistDao
  abstract fun playbackSnapshotDao(): PlaybackSnapshotDao
}

