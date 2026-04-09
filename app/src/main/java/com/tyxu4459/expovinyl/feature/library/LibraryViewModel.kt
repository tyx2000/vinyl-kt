package com.tyxu4459.expovinyl.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyxu4459.expovinyl.data.repository.LibraryRepository
import com.tyxu4459.expovinyl.media.PlaybackController
import com.tyxu4459.expovinyl.model.PlaylistSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
  val playlists: List<PlaylistSummary> = emptyList(),
  val isLoading: Boolean = false,
  val isScanning: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
  private val libraryRepository: LibraryRepository,
  private val playbackController: PlaybackController,
) : ViewModel() {
  private val isScanning = MutableStateFlow(false)

  val uiState: StateFlow<LibraryUiState> =
    combine(
      libraryRepository.observePlaylists(),
      isScanning,
    ) { playlists, scanning ->
        LibraryUiState(
          playlists = playlists,
          isLoading = false,
          isScanning = scanning,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(isLoading = true),
      )

  fun createPlaylist(name: String) {
    viewModelScope.launch {
      libraryRepository.createPlaylist(name)
    }
  }

  fun scanDeviceAudioToAlbums() {
    viewModelScope.launch {
      if (isScanning.value) return@launch
      isScanning.value = true
      runCatching {
        libraryRepository.scanDeviceAudioToAlbums()
      }
      isScanning.value = false
    }
  }

  fun removePlaylist(playlistId: String) {
    viewModelScope.launch {
      libraryRepository.removePlaylist(playlistId)
      playbackController.onPlaylistRemoved(playlistId)
    }
  }
}
