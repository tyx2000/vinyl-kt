package com.tyxu4459.expovinyl.di

import android.content.Context
import androidx.room.Room
import com.tyxu4459.expovinyl.data.local.VinylDatabase
import com.tyxu4459.expovinyl.data.local.dao.PlaybackSnapshotDao
import com.tyxu4459.expovinyl.data.local.dao.PlaylistDao
import com.tyxu4459.expovinyl.data.repository.LibraryRepository
import com.tyxu4459.expovinyl.data.repository.PlaybackRepository
import com.tyxu4459.expovinyl.data.repository.RoomLibraryRepository
import com.tyxu4459.expovinyl.data.repository.RoomPlaybackRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): VinylDatabase =
    Room.databaseBuilder(context, VinylDatabase::class.java, "vinyl.db").build()

  @Provides
  fun providePlaylistDao(database: VinylDatabase): PlaylistDao = database.playlistDao()

  @Provides
  fun providePlaybackSnapshotDao(database: VinylDatabase): PlaybackSnapshotDao =
    database.playbackSnapshotDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds
  @Singleton
  abstract fun bindLibraryRepository(
    implementation: RoomLibraryRepository,
  ): LibraryRepository

  @Binds
  @Singleton
  abstract fun bindPlaybackRepository(
    implementation: RoomPlaybackRepository,
  ): PlaybackRepository
}

