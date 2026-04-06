package com.tyxu4459.expovinyl.model

enum class PlayMode {
  LOOP,
  SINGLE,
}

data class PlaylistSummary(
  val id: String,
  val name: String,
  val trackCount: Int,
)

data class Track(
  val id: Long,
  val uri: String,
  val displayName: String,
  val artist: String?,
  val album: String?,
  val durationMs: Long?,
  val importedAt: Long,
)

enum class ImportStage {
  COPYING,
  DONE,
  CANCELLED,
}

data class ImportProgress(
  val totalCount: Int = 0,
  val processedCount: Int = 0,
  val copiedCount: Int = 0,
  val failedCount: Int = 0,
  val skippedCount: Int = 0,
  val stage: ImportStage = ImportStage.COPYING,
)

data class ImportTracksResult(
  val tracks: List<Track>,
  val totalCandidateCount: Int,
  val processedCount: Int,
  val copiedCount: Int,
  val failedCount: Int,
  val skippedCount: Int,
  val cancelled: Boolean,
)

data class PlaylistDetail(
  val id: String,
  val name: String,
  val tracks: List<Track>,
)

data class PlaybackSnapshot(
  val playlistId: String?,
  val trackId: Long?,
  val queueIndex: Int,
  val positionMs: Long,
  val isPlaying: Boolean,
  val playMode: PlayMode,
  val sleepTimerEndsAt: Long?,
)

data class MiniPlayerUiState(
  val visible: Boolean = false,
  val title: String = "",
  val playlistId: String? = null,
  val currentTrackId: Long? = null,
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val playMode: PlayMode = PlayMode.LOOP,
  val sleepTimerEndsAt: Long? = null,
)
