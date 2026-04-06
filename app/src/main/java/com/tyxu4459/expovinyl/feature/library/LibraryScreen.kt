package com.tyxu4459.expovinyl.feature.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.tyxu4459.expovinyl.model.PlaylistSummary

private data class PlaylistNameValidation(
  val normalized: String,
  val isValid: Boolean,
  val error: String?,
)

private fun validatePlaylistName(input: String): PlaylistNameValidation {
  val normalized = input.trim()
  if (normalized.isEmpty()) {
    return PlaylistNameValidation(
      normalized = normalized,
      isValid = false,
      error = "Enter a playlist name.",
    )
  }

  var weightedLength = 0
  normalized.forEach { char ->
    when {
      char in '\u4E00'..'\u9FFF' -> weightedLength += 3
      char.isLetterOrDigit() -> weightedLength += 1
      else -> {
        return PlaylistNameValidation(
          normalized = normalized,
          isValid = false,
          error = "Use only Chinese characters, letters, or numbers.",
        )
      }
    }
  }

  if (weightedLength > 18) {
    return PlaylistNameValidation(
      normalized = normalized,
      isValid = false,
      error = "Use up to 6 Chinese characters or 18 English characters.",
    )
  }

  return PlaylistNameValidation(
    normalized = normalized,
    isValid = true,
    error = null,
  )
}

@Composable
private fun DeletePlaylistDialog(
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
    title = { Text("Delete Playlist") },
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

@Composable
private fun CreatePlaylistDialog(
  visible: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  if (!visible) return

  var input by remember { mutableStateOf("") }
  var showValidation by remember { mutableStateOf(false) }
  val validation = validatePlaylistName(input)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    textContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    tonalElevation = 0.dp,
    title = { Text("Create Playlist") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = input,
          onValueChange = { next ->
            input = next
            showValidation = false
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          label = { Text("Playlist Name") },
          isError = showValidation && !validation.isValid,
          supportingText =
            if (showValidation && !validation.isValid) {
              {
                Text(validation.error.orEmpty())
              }
            } else {
              null
            },
          keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
              unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.86f),
              disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
              errorContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              focusedLabelColor = MaterialTheme.colorScheme.primary,
              unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              cursorColor = MaterialTheme.colorScheme.primary,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
              errorBorderColor = MaterialTheme.colorScheme.error,
              errorLabelColor = MaterialTheme.colorScheme.error,
              errorSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      FilledTonalButton(
        onClick = {
          if (validation.isValid) {
            onConfirm(validation.normalized)
          } else {
            showValidation = true
          }
        },
        shape = RoundedCornerShape(14.dp),
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        colors =
          ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
      ) {
        Text("Cancel")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryRoute(
  onPlaylistClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LibraryViewModel = hiltViewModel(),
) {
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  LibraryScreen(
    state = state.value,
    onPlaylistClick = onPlaylistClick,
    onCreatePlaylist = viewModel::createPlaylist,
    onDeletePlaylist = viewModel::removePlaylist,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
  state: LibraryUiState,
  onPlaylistClick: (String) -> Unit,
  onCreatePlaylist: (String) -> Unit,
  onDeletePlaylist: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var dialogVisible by remember { mutableStateOf(false) }
  var pendingDeletePlaylist by remember { mutableStateOf<PlaylistSummary?>(null) }
  val hapticFeedback = LocalHapticFeedback.current

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = "Vinyl",
              style = MaterialTheme.typography.headlineSmall,
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
          actions = {
            FilledTonalButton(
              onClick = { dialogVisible = true },
              modifier = Modifier.padding(end = 12.dp),
              shape = RoundedCornerShape(18.dp),
            ) {
              Text("Create")
            }
          }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
      }
    },
  ) { innerPadding ->
    when {
      state.isLoading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      }

      state.playlists.isEmpty() -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "No playlists yet",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
          itemsIndexed(
            items = state.playlists,
            key = { _, playlist -> playlist.id },
          ) { index, playlist ->
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                  onClick = { onPlaylistClick(playlist.id) },
                  onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    pendingDeletePlaylist = playlist
                  },
                ),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = playlist.name,
                  modifier = Modifier.weight(1f),
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                  text = "${playlist.trackCount}",
                  modifier = Modifier.width(40.dp),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Clip,
                  textAlign = TextAlign.End,
                )
              }
              if (index < state.playlists.lastIndex) {
                HorizontalDivider(
                  modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                )
              }
            }
          }

          item {
            Spacer(modifier = Modifier.height(0.dp))
          }
        }
      }
    }
  }

  CreatePlaylistDialog(
    visible = dialogVisible,
    onDismiss = { dialogVisible = false },
    onConfirm = { name ->
      dialogVisible = false
      onCreatePlaylist(name)
    },
  )

  DeletePlaylistDialog(
    targetName = pendingDeletePlaylist?.name,
    onDismiss = { pendingDeletePlaylist = null },
    onConfirm = {
      pendingDeletePlaylist?.id?.let(onDeletePlaylist)
      pendingDeletePlaylist = null
    },
  )
}
