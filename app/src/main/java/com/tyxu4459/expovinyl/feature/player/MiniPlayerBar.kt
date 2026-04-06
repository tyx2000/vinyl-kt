package com.tyxu4459.expovinyl.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyxu4459.expovinyl.model.MiniPlayerUiState
import com.tyxu4459.expovinyl.model.PlayMode
import kotlin.math.abs
import kotlinx.coroutines.delay

private fun formatPosition(ms: Long): String {
  val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}

private fun formatCountdown(remainingMs: Long): String {
  val clampedMs = remainingMs.coerceAtLeast(0L)
  if (clampedMs <= 0L) return "T"
  val totalSeconds = ((clampedMs + 999L) / 1000L).toInt()
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%02d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}

@Composable
private fun SleepTimerDialog(
  visible: Boolean,
  onDismiss: () -> Unit,
  onSelect: (Int?) -> Unit,
) {
  if (!visible) return

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Sleep timer") },
    text = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { onSelect(null) }) {
          Text("Off")
        }
        TextButton(onClick = { onSelect(15) }) {
          Text("15m")
        }
        TextButton(onClick = { onSelect(30) }) {
          Text("30m")
        }
        TextButton(onClick = { onSelect(60) }) {
          Text("60m")
        }
      }
    },
    confirmButton = {},
  )
}

@Composable
fun MiniPlayerBar(
  state: MiniPlayerUiState,
  modifier: Modifier = Modifier,
  onOpenPlayerClick: () -> Unit = {},
  onPreviousClick: () -> Unit = {},
  onPlayPauseClick: () -> Unit = {},
  onNextClick: () -> Unit = {},
  onCyclePlayModeClick: () -> Unit = {},
  onSetSleepTimer: (Int?) -> Unit = {},
  onSeekTo: (Long) -> Unit = {},
) {
  if (!state.visible) return

  var timerDialogVisible by remember { mutableStateOf(false) }
  var isSeeking by remember { mutableStateOf(false) }
  var sliderProgress by remember { mutableFloatStateOf(0f) }
  var pendingSeekPositionMs by remember { mutableStateOf<Long?>(null) }
  var pendingSeekOriginMs by remember { mutableStateOf<Long?>(null) }
  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

  val progress =
    if (state.durationMs > 0) {
      (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
      0f
    }
  val targetPositionMsForUi =
    when {
      isSeeking -> (sliderProgress * state.durationMs.toFloat()).toLong()
      pendingSeekPositionMs != null -> pendingSeekPositionMs ?: state.positionMs
      else -> state.positionMs
    }
  val progressTarget =
    if (state.durationMs > 0) {
      (targetPositionMsForUi.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
      0f
    }
  val animatedProgress by animateFloatAsState(
    targetValue = progressTarget,
    animationSpec = tween(durationMillis = 450, easing = LinearEasing),
    label = "mini_player_progress",
  )
  val animatedPositionMs by animateIntAsState(
    targetValue = targetPositionMsForUi.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
    animationSpec = tween(durationMillis = 450, easing = LinearEasing),
    label = "mini_player_position",
  )
  val countdownTargetMs =
    state.sleepTimerEndsAt
      ?.minus(nowMs)
      ?.coerceAtLeast(0L)
      ?: 0L
  LaunchedEffect(progress, isSeeking, pendingSeekPositionMs) {
    if (!isSeeking && pendingSeekPositionMs == null) {
      sliderProgress = progress
    }
  }
  LaunchedEffect(state.sleepTimerEndsAt) {
    while (true) {
      nowMs = System.currentTimeMillis()
      delay(500)
    }
  }
  LaunchedEffect(state.positionMs, pendingSeekPositionMs, pendingSeekOriginMs) {
    val pending = pendingSeekPositionMs ?: return@LaunchedEffect
    val origin = pendingSeekOriginMs ?: return@LaunchedEffect
    val seekDistance = abs(pending - origin)
    val movedEnough = abs(state.positionMs - origin) >= minOf(300L, seekDistance)
    val closeEnough = abs(state.positionMs - pending) <= 300L
    if (closeEnough && (movedEnough || seekDistance <= 120L)) {
      pendingSeekPositionMs = null
      pendingSeekOriginMs = null
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .clickable(onClick = onOpenPlayerClick),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
      ),
    shape = RoundedCornerShape(28.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
    elevation = cardElevation(defaultElevation = 0.75.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = state.title,
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(42.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipPrevious,
              contentDescription = "Previous",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          FilledIconButton(
            onClick = onPlayPauseClick,
            shape = CircleShape,
            modifier = Modifier.size(46.dp),
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            Icon(
              imageVector =
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (state.isPlaying) "Pause" else "Play",
            )
          }
          IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(42.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipNext,
              contentDescription = "Next",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Slider(
        value = if (isSeeking || pendingSeekPositionMs != null) sliderProgress else animatedProgress,
        onValueChange = { next ->
          isSeeking = true
          pendingSeekPositionMs = null
          pendingSeekOriginMs = null
          sliderProgress = next
        },
        onValueChangeFinished = {
          val targetPosition =
            (sliderProgress.coerceIn(0f, 1f) * state.durationMs.toFloat()).toLong()
          pendingSeekPositionMs = targetPosition
          pendingSeekOriginMs = state.positionMs.coerceAtLeast(0L)
          onSeekTo(targetPosition)
          isSeeking = false
        },
        valueRange = 0f..1f,
        colors =
          SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
          ),
        modifier = Modifier
          .fillMaxWidth()
          .graphicsLayer(
            scaleX = 0.98f,
            scaleY = 0.56f,
          ),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = { timerDialogVisible = true },
          modifier = Modifier.width(56.dp),
        ) {
          Text(
            text = formatCountdown(countdownTargetMs),
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum",
                platformStyle = PlatformTextStyle(includeFontPadding = false),
              ),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
          )
        }
        Row(
          modifier = Modifier.width(132.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = formatPosition(animatedPositionMs.toLong()),
            modifier = Modifier.width(48.dp),
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum",
                platformStyle = PlatformTextStyle(includeFontPadding = false),
              ),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Right,
          )
          Text(
            text = " / ",
            style =
              MaterialTheme.typography.labelMedium.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
              ),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
          )
          Text(
            text = formatPosition(state.durationMs),
            modifier = Modifier.width(48.dp),
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum",
                platformStyle = PlatformTextStyle(includeFontPadding = false),
              ),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Left,
          )
        }
        IconButton(
          onClick = onCyclePlayModeClick,
          modifier = Modifier.size(40.dp),
        ) {
          AnimatedContent(
            targetState = state.playMode,
            transitionSpec = {
              val enter =
                fadeIn(animationSpec = tween(180)) +
                  slideInVertically(animationSpec = tween(180)) { it / 3 }
              val exit =
                fadeOut(animationSpec = tween(160)) +
                  slideOutVertically(animationSpec = tween(160)) { -it / 3 }
              enter togetherWith exit
            },
            label = "play_mode_switch",
          ) { playMode ->
            Text(
              text = if (playMode == PlayMode.SINGLE) "S" else "L",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    }
  }

  SleepTimerDialog(
    visible = timerDialogVisible,
    onDismiss = { timerDialogVisible = false },
    onSelect = { minutes ->
      timerDialogVisible = false
      onSetSleepTimer(minutes)
    },
  )
}
