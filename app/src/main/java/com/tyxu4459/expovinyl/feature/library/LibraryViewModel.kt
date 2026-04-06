package com.tyxu4459.expovinyl.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyxu4459.expovinyl.data.repository.LibraryRepository
import com.tyxu4459.expovinyl.media.PlaybackController
import com.tyxu4459.expovinyl.model.PlaylistSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
  val playlists: List<PlaylistSummary> = emptyList(),
  val isLoading: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
  private val libraryRepository: LibraryRepository,
  private val playbackController: PlaybackController,
) : ViewModel() {
  val uiState: StateFlow<LibraryUiState> =
    libraryRepository.observePlaylists()
      .map { playlists ->
        LibraryUiState(
          playlists = playlists,
          isLoading = false,
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

  fun removePlaylist(playlistId: String) {
    viewModelScope.launch {
      libraryRepository.removePlaylist(playlistId)
      playbackController.onPlaylistRemoved(playlistId)
    }
  }
}
