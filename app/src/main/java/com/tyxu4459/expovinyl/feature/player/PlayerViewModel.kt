package com.tyxu4459.expovinyl.feature.player

import androidx.lifecycle.ViewModel
import com.tyxu4459.expovinyl.media.PlaybackController
import com.tyxu4459.expovinyl.model.MiniPlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PlayerViewModel @Inject constructor(
  private val playbackController: PlaybackController,
) : ViewModel() {
  val miniPlayerState: StateFlow<MiniPlayerUiState> = playbackController.miniPlayerState

  fun onPlayPauseClick() {
    playbackController.togglePlayPause()
  }

  fun onPreviousClick() {
    playbackController.playPrevious()
  }

  fun onNextClick() {
    playbackController.playNext()
  }

  fun onSeekTo(positionMs: Long) {
    playbackController.seekTo(positionMs)
  }

  fun onCyclePlayModeClick() {
    playbackController.cyclePlayMode()
  }

  fun onSetSleepTimer(minutes: Int?) {
    playbackController.setSleepTimerMinutes(minutes)
  }
}
