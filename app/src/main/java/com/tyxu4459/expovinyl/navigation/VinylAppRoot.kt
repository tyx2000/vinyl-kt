package com.tyxu4459.expovinyl.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tyxu4459.expovinyl.feature.library.LibraryRoute
import com.tyxu4459.expovinyl.feature.player.MiniPlayerBar
import com.tyxu4459.expovinyl.feature.player.PlayerViewModel
import com.tyxu4459.expovinyl.feature.playlist.PlaylistDetailRoute
import com.tyxu4459.expovinyl.ui.theme.AuroraBackground

object Destinations {
  const val Library = "library"
  const val Playlist = "playlist/{playlistId}"

  fun playlist(playlistId: String) = "playlist/$playlistId"
}

private const val NAV_TRANSITION_MS = 280

@Composable
fun VinylAppRoot(
  modifier: Modifier = Modifier,
  playerViewModel: PlayerViewModel = hiltViewModel(),
) {
  val navController = rememberNavController()
  val navBackStackEntry = navController.currentBackStackEntryAsState()
  val miniPlayerState = playerViewModel.miniPlayerState.collectAsStateWithLifecycle()
  val density = LocalDensity.current
  var miniPlayerOccupiedHeightPx by remember { mutableIntStateOf(0) }
  val miniPlayerInset =
    if (miniPlayerState.value.visible) {
      with(density) { miniPlayerOccupiedHeightPx.toDp() + 24.dp }
    } else {
      0.dp
    }

  Box(modifier = modifier.fillMaxSize()) {
    AuroraBackground()

    if (miniPlayerInset > 0.dp) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(miniPlayerInset)
          .background(MaterialTheme.colorScheme.primaryContainer),
      )
    }

    NavHost(
      navController = navController,
      startDestination = Destinations.Library,
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = miniPlayerInset),
    ) {
      composable(Destinations.Library) {
        LibraryRoute(
          onPlaylistClick = { playlistId ->
            navController.navigate(Destinations.playlist(playlistId))
          },
        )
      }

      composable(
        route = Destinations.Playlist,
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        enterTransition = {
          slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(NAV_TRANSITION_MS),
          )
        },
        exitTransition = {
          slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(NAV_TRANSITION_MS),
          )
        },
        popEnterTransition = {
          slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(NAV_TRANSITION_MS),
          )
        },
        popExitTransition = {
          slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(NAV_TRANSITION_MS),
          )
        },
      ) {
        PlaylistDetailRoute()
      }
    }

    MiniPlayerBar(
      state = miniPlayerState.value,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 20.dp)
        .onSizeChanged { miniPlayerOccupiedHeightPx = it.height },
      onOpenPlayerClick = {
        val targetPlaylistId = miniPlayerState.value.playlistId ?: return@MiniPlayerBar
        val currentPlaylistId =
          navBackStackEntry.value?.arguments?.getString("playlistId")
        if (currentPlaylistId == targetPlaylistId) {
          return@MiniPlayerBar
        }
        navController.navigate(Destinations.playlist(targetPlaylistId))
      },
      onPreviousClick = playerViewModel::onPreviousClick,
      onPlayPauseClick = playerViewModel::onPlayPauseClick,
      onNextClick = playerViewModel::onNextClick,
      onCyclePlayModeClick = playerViewModel::onCyclePlayModeClick,
      onSetSleepTimer = playerViewModel::onSetSleepTimer,
      onSeekTo = playerViewModel::onSeekTo,
    )
  }
}
