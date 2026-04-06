package com.tyxu4459.expovinyl.feature.playlist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyxu4459.expovinyl.data.repository.LibraryRepository
import com.tyxu4459.expovinyl.media.PlaybackController
import com.tyxu4459.expovinyl.model.ImportProgress
import com.tyxu4459.expovinyl.model.ImportStage
import com.tyxu4459.expovinyl.model.PlaylistDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
  val playlist: PlaylistDetail? = null,
  val isLoading: Boolean = false,
  val isImporting: Boolean = false,
  val importProgress: ImportProgress = ImportProgress(),
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val libraryRepository: LibraryRepository,
  private val playbackController: PlaybackController,
) : ViewModel() {
  private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])
  private val importUiState = MutableStateFlow(PlaylistDetailUiState())
  @Volatile private var cancelImportRequested = false

  val uiState: StateFlow<PlaylistDetailUiState> =
    combine(
      libraryRepository.observePlaylistDetail(playlistId),
      importUiState,
    ) { playlist, importState ->
      PlaylistDetailUiState(
        playlist = playlist,
        isLoading = false,
        isImporting = importState.isImporting,
        importProgress = importState.importProgress,
      )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaylistDetailUiState(isLoading = true),
      )

  fun importTracks(uris: List<Uri>) {
    if (uris.isEmpty() || importUiState.value.isImporting) return
    viewModelScope.launch {
      cancelImportRequested = false
      importUiState.value =
        PlaylistDetailUiState(
          isImporting = true,
          importProgress =
            ImportProgress(
              totalCount = uris.size,
              stage = ImportStage.COPYING,
            ),
        )
      try {
        libraryRepository.importTracks(
          playlistId = playlistId,
          uris = uris,
          shouldCancel = { cancelImportRequested },
          onProgress = { progress ->
            importUiState.update { current ->
              current.copy(
                isImporting = progress.stage == ImportStage.COPYING,
                importProgress = progress,
              )
            }
          },
        )
      } finally {
        importUiState.update { current ->
          current.copy(isImporting = false)
        }
      }
    }
  }

  fun cancelImport() {
    cancelImportRequested = true
  }

  fun removeTrack(trackId: Long) {
    val playlist = uiState.value.playlist ?: return
    val remainingTracks = playlist.tracks.filterNot { it.id == trackId }
    viewModelScope.launch {
      libraryRepository.removeTrackFromPlaylist(playlistId, trackId)
      playbackController.onTrackRemoved(
        playlistId = playlist.id,
        removedTrackId = trackId,
        remainingTracks = remainingTracks,
      )
    }
  }

  fun playTrack(index: Int) {
    val playlist = uiState.value.playlist ?: return
    if (playlist.tracks.isEmpty()) return
    playbackController.playTracks(
      playlistId = playlist.id,
      tracks = playlist.tracks,
      startIndex = index,
    )
  }

  fun togglePlayback() {
    playbackController.togglePlayPause()
  }

  fun playNext() {
    playbackController.playNext()
  }

  fun playPrevious() {
    playbackController.playPrevious()
  }
}
