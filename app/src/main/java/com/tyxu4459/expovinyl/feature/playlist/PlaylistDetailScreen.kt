package com.tyxu4459.expovinyl.feature.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tyxu4459.expovinyl.feature.player.PlayerViewModel

private fun formatDuration(durationMs: Long?): String {
  if (durationMs == null || durationMs <= 0L) return "--:--"
  val totalSeconds = (durationMs / 1000L).toInt()
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun DeleteTrackDialog(
  targetName: String?,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  if (targetName == null) return

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    textContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    title = { Text("Delete Track") },
    text = { Text("Delete \"$targetName\"?") },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text("Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailRoute(
  modifier: Modifier = Modifier,
  viewModel: PlaylistDetailViewModel = hiltViewModel(),
  playerViewModel: PlayerViewModel = hiltViewModel(),
) {
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val miniPlayerState by playerViewModel.miniPlayerState.collectAsStateWithLifecycle()
  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.importTracks(uris)
    }
  }
  PlaylistDetailScreen(
    state = state.value,
    currentlyPlayingTrackId =
      miniPlayerState.currentTrackId?.takeIf {
        miniPlayerState.playlistId == state.value.playlist?.id
      },
    onImportClick = { importLauncher.launch(arrayOf("audio/*")) },
    onCancelImport = viewModel::cancelImport,
    onTrackClick = viewModel::playTrack,
    onDeleteTrack = viewModel::removeTrack,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
  state: PlaylistDetailUiState,
  currentlyPlayingTrackId: Long?,
  onImportClick: () -> Unit,
  onCancelImport: () -> Unit,
  onTrackClick: (Int) -> Unit,
  onDeleteTrack: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hapticFeedback = LocalHapticFeedback.current
  val listState = rememberLazyListState()
  var pendingDeleteTrackId by remember { mutableStateOf<Long?>(null) }
  var pendingDeleteTrackName by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(currentlyPlayingTrackId, state.playlist?.id) {
    val targetTrackId = currentlyPlayingTrackId ?: return@LaunchedEffect
    val tracks = state.playlist?.tracks ?: return@LaunchedEffect
    val targetIndex = tracks.indexOfFirst { it.id == targetTrackId }
    if (targetIndex < 0) return@LaunchedEffect
    val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
    if (!isVisible) {
      listState.animateScrollToItem(targetIndex)
    }
  }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = state.playlist?.name ?: "Playlist",
              style = MaterialTheme.typography.headlineSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
          // navigationIcon = {
          //   TextButton(onClick = onBackClick) {
          //     Text("Back")
          //   }
          // },
          actions = {
            FilledTonalButton(
              onClick = onImportClick,
              enabled = !state.isImporting,
              modifier = Modifier.padding(end = 12.dp),
              shape = RoundedCornerShape(18.dp),
            ) {
              Text(if (state.isImporting) "Importing" else "Import")
            }
          }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
      when {
        state.isLoading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }

        state.playlist == null -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text("Playlist not found")
          }
        }

        state.playlist.tracks.isEmpty() -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "No tracks yet",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Import local audio files into this playlist.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
          ) {
            itemsIndexed(
              items = state.playlist.tracks,
              key = { _, track -> track.id },
            ) { index, track ->
              Column(
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                      enabled = !state.isImporting,
                      onClick = {
                        if (index >= 0) {
                          onTrackClick(index)
                        }
                      },
                      onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        pendingDeleteTrackId = track.id
                        pendingDeleteTrackName = track.displayName
                      },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (track.id == currentlyPlayingTrackId) {
                    Box(
                      modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(
                          color = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(999.dp),
                        ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                  } else {
                    Spacer(modifier = Modifier.width(16.dp))
                  }
                  Text(
                    text = track.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.width(24.dp))
                  Text(
                    text = formatDuration(track.durationMs),
                    modifier = Modifier.width(48.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.End,
                  )
                }
                if (index < state.playlist.tracks.lastIndex) {
                  HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                  )
                }
              }
            }

            item {
              Spacer(modifier = Modifier.height(if (state.isImporting) 108.dp else 0.dp))
            }
          }
        }
      }

      if (state.isImporting) {
        Card(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 20.dp),
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "Importing local audio files",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
              Text(
                text = "${state.importProgress.processedCount}/${state.importProgress.totalCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Text(
              text =
                "Copied: ${state.importProgress.copiedCount}  Failed: ${state.importProgress.failedCount}  Unsupported: ${state.importProgress.skippedCount}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
            )
            TextButton(
              onClick = onCancelImport,
              shape = CircleShape,
            ) {
              Text("Cancel")
            }
          }
        }
      }
    }
  }

  DeleteTrackDialog(
    targetName = pendingDeleteTrackName,
    onDismiss = {
      pendingDeleteTrackId = null
      pendingDeleteTrackName = null
    },
    onConfirm = {
      pendingDeleteTrackId?.let(onDeleteTrack)
      pendingDeleteTrackId = null
      pendingDeleteTrackName = null
    },
  )
}
