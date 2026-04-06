package com.tyxu4459.expovinyl.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tyxu4459.expovinyl.data.repository.LibraryRepository
import com.tyxu4459.expovinyl.data.repository.PlaybackRepository
import com.tyxu4459.expovinyl.model.MiniPlayerUiState
import com.tyxu4459.expovinyl.model.PlayMode
import com.tyxu4459.expovinyl.model.PlaybackSnapshot
import com.tyxu4459.expovinyl.model.Track
import com.tyxu4459.expovinyl.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class PlaybackController @Inject constructor(
  @ApplicationContext private val context: Context,
  private val libraryRepository: LibraryRepository,
  private val playbackRepository: PlaybackRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val controllerState = MutableStateFlow<MediaController?>(null)
  private val _miniPlayerState = MutableStateFlow(MiniPlayerUiState())
  private val appArtworkData by lazy { loadAppArtwork() }
  private var currentPlaylistId: String? = null
  private var currentPlayMode: PlayMode = PlayMode.LOOP
  private var currentSleepTimerEndsAt: Long? = null
  private var lastPersistedAt = 0L
  private var isRestoring = false
  private var hasRestoredSnapshot = false

  val miniPlayerState: StateFlow<MiniPlayerUiState> = _miniPlayerState.asStateFlow()

  private val listener = object : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {
      pushState()
      scope.launch {
        persistSnapshot(force = true)
      }
    }
  }

  init {
    connect()
    scope.launch {
      awaitController()
      restoreSavedPlayback()
    }
    scope.launch {
      while (isActive) {
        delay(500)
        checkSleepTimer()
        pushState()
        persistSnapshot(force = false)
      }
    }
  }

  fun togglePlayPause() {
    controllerState.value?.let { controller ->
      if (controller.isPlaying) {
        controller.pause()
      } else {
        controller.play()
      }
    }
  }

  fun playNext() {
    controllerState.value?.seekToNextMediaItem()
  }

  fun playPrevious() {
    controllerState.value?.seekToPreviousMediaItem()
  }

  fun seekTo(positionMs: Long) {
    controllerState.value?.seekTo(positionMs.coerceAtLeast(0L))
  }

  fun cyclePlayMode() {
    currentPlayMode =
      when (currentPlayMode) {
        PlayMode.LOOP -> PlayMode.SINGLE
        PlayMode.SINGLE -> PlayMode.LOOP
      }
    controllerState.value?.let { controller ->
      applyPlayMode(controller, currentPlayMode)
      pushState()
      scope.launch {
        persistSnapshot(force = true)
      }
    }
  }

  fun setSleepTimerMinutes(minutes: Int?) {
    currentSleepTimerEndsAt =
      if (minutes == null || minutes <= 0) {
        null
      } else {
        System.currentTimeMillis() + minutes * 60_000L
      }
    pushState()
    scope.launch {
      persistSnapshot(force = true)
    }
  }

  fun onPlaylistRemoved(playlistId: String) {
    if (currentPlaylistId != playlistId) return
    scope.launch {
      clearPlayback()
    }
  }

  fun onTrackRemoved(
    playlistId: String,
    removedTrackId: Long,
    remainingTracks: List<Track>,
  ) {
    if (currentPlaylistId != playlistId) return
    scope.launch {
      val controller = awaitController()
      val currentTrackId = controller.currentMediaItem?.mediaId?.toLongOrNull()
      if (currentTrackId == null) return@launch

      if (remainingTracks.isEmpty()) {
        clearPlayback()
        return@launch
      }

      val wasPlaying = controller.isPlaying
      val previousIndex = controller.currentMediaItemIndex.coerceAtLeast(0)
      val previousPosition = controller.currentPosition.coerceAtLeast(0L)
      val keptTrackId = currentTrackId.takeUnless { it == removedTrackId }
      val nextIndex =
        if (keptTrackId != null) {
          remainingTracks.indexOfFirst { it.id == keptTrackId }.takeIf { it >= 0 } ?: 0
        } else {
          previousIndex.coerceIn(0, remainingTracks.lastIndex)
        }

      controller.setMediaItems(
        buildMediaItems(
          playlistId = playlistId,
          tracks = remainingTracks,
        ),
        nextIndex,
        if (keptTrackId != null) previousPosition else 0L,
      )
      applyPlayMode(controller, currentPlayMode)
      controller.prepare()
      if (wasPlaying) {
        controller.play()
      } else {
        controller.pause()
      }
      pushState()
      persistSnapshot(force = true)
    }
  }

  fun playTracks(
    playlistId: String,
    tracks: List<Track>,
    startIndex: Int,
  ) {
    if (tracks.isEmpty()) return
    scope.launch {
      currentPlaylistId = playlistId
      currentPlayMode = PlayMode.LOOP
      val controller = awaitController()
      val items = buildMediaItems(
        playlistId = playlistId,
        tracks = tracks,
      )
      controller.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
      applyPlayMode(controller, currentPlayMode)
      controller.prepare()
      controller.play()
      pushState()
      persistSnapshot(force = true)
    }
  }

  private fun connect() {
    val sessionToken = SessionToken(
      context,
      ComponentName(context, VinylMediaService::class.java),
    )
    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    controllerFuture.addListener(
      {
        runCatching { controllerFuture.get() }.getOrNull()?.let { controller ->
          controller.addListener(listener)
          controllerState.value = controller
          pushState()
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  private suspend fun awaitController(): MediaController =
    controllerState.filterNotNull().first()

  private fun buildMediaItems(
    playlistId: String,
    tracks: List<Track>,
  ): List<MediaItem> =
    tracks.map { track ->
      MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(track.uri)
        .setMediaMetadata(
          MediaMetadata.Builder()
            .setTitle(track.displayName)
            .setArtworkData(appArtworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .setExtras(
              Bundle().apply {
                putString("playlist_id", playlistId)
              },
            )
            .build(),
        )
        .build()
  }

  private fun loadAppArtwork(): ByteArray? {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_media_artwork) ?: return null
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 432
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 432
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return ByteArrayOutputStream().use { stream ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      stream.toByteArray()
    }
  }

  private fun applyPlayMode(controller: MediaController, playMode: PlayMode) {
    controller.repeatMode =
      when (playMode) {
        PlayMode.LOOP -> Player.REPEAT_MODE_ALL
        PlayMode.SINGLE -> Player.REPEAT_MODE_ONE
      }
  }

  private fun checkSleepTimer() {
    val endsAt = currentSleepTimerEndsAt ?: return
    if (System.currentTimeMillis() < endsAt) {
      return
    }
    currentSleepTimerEndsAt = null
    controllerState.value?.pause()
    pushState()
    scope.launch {
      persistSnapshot(force = true)
    }
  }

  private suspend fun restoreSavedPlayback() {
    if (hasRestoredSnapshot) return
    hasRestoredSnapshot = true

    val snapshot = playbackRepository.observeSnapshot().first() ?: return
    val playlistId = snapshot.playlistId ?: return
    val playlist = libraryRepository.getPlaylistDetail(playlistId) ?: return
    if (playlist.tracks.isEmpty()) return

    val controller = awaitController()
    val startIndex = snapshot.queueIndex.coerceIn(0, playlist.tracks.lastIndex)
    val startPosition = snapshot.positionMs.coerceAtLeast(0L)

    isRestoring = true
    currentPlaylistId = playlist.id
    currentPlayMode = snapshot.playMode
    currentSleepTimerEndsAt =
      snapshot.sleepTimerEndsAt?.takeIf { it > System.currentTimeMillis() }
    controller.setMediaItems(
      buildMediaItems(
        playlistId = playlist.id,
        tracks = playlist.tracks,
      ),
      startIndex,
      startPosition,
    )
    applyPlayMode(controller, snapshot.playMode)
    controller.prepare()
    controller.pause()
    pushState()
    persistSnapshot(force = true)
    lastPersistedAt = System.currentTimeMillis()
    isRestoring = false
  }

  private suspend fun clearPlayback() {
    val controller = awaitController()
    currentPlaylistId = null
    currentPlayMode = PlayMode.LOOP
    currentSleepTimerEndsAt = null
    controller.stop()
    controller.clearMediaItems()
    pushState()
    playbackRepository.clearSnapshot()
  }

  private fun pushState() {
    val controller = controllerState.value
    if (controller == null || controller.currentMediaItem == null) {
      _miniPlayerState.value = MiniPlayerUiState()
      return
    }

    val metadata = controller.mediaMetadata
    val durationMs =
      if (controller.duration == C.TIME_UNSET) 0L else controller.duration.coerceAtLeast(0L)

    _miniPlayerState.value = MiniPlayerUiState(
      visible = true,
      title = metadata.title?.toString().orEmpty(),
      playlistId = currentPlaylistId ?: metadata.extras?.getString("playlist_id"),
      currentTrackId = controller.currentMediaItem?.mediaId?.toLongOrNull(),
      isPlaying = controller.isPlaying,
      positionMs = controller.currentPosition.coerceAtLeast(0L),
      durationMs = durationMs,
      playMode = currentPlayMode,
      sleepTimerEndsAt = currentSleepTimerEndsAt,
    )
  }

  private suspend fun persistSnapshot(force: Boolean) {
    if (isRestoring) return
    val controller = controllerState.value ?: return
    val item = controller.currentMediaItem ?: return
    val now = System.currentTimeMillis()
    if (!force && now - lastPersistedAt < 2_000L) {
      return
    }
    lastPersistedAt = now
    playbackRepository.saveSnapshot(
      PlaybackSnapshot(
        playlistId = currentPlaylistId ?: item.mediaMetadata.extras?.getString("playlist_id"),
        trackId = item.mediaId.toLongOrNull(),
        queueIndex = controller.currentMediaItemIndex.coerceAtLeast(0),
        positionMs = controller.currentPosition.coerceAtLeast(0L),
        isPlaying = controller.isPlaying,
        playMode = currentPlayMode,
        sleepTimerEndsAt = currentSleepTimerEndsAt,
      ),
    )
  }
}
