package com.tyxu4459.expovinyl.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tyxu4459.expovinyl.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@UnstableApi
class VinylMediaService : MediaSessionService() {
  private var player: ExoPlayer? = null
  private var mediaSession: MediaSession? = null

  override fun onCreate() {
    super.onCreate()

    player = ExoPlayer.Builder(this)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .setUsage(C.USAGE_MEDIA)
          .build(),
        true,
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

    val launchIntent =
      packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
        PendingIntent.getActivity(
          this,
          0,
          intent.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
          },
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
      }

    val sessionBuilder = MediaSession.Builder(this, checkNotNull(player))
      .setId("vinyl-main-session")
    if (launchIntent != null) {
      sessionBuilder.setSessionActivity(launchIntent)
    }
    val notificationProvider =
      DefaultMediaNotificationProvider.Builder(this).build().apply {
        setSmallIcon(R.drawable.ic_notification_small)
      }
    setMediaNotificationProvider(notificationProvider)
    mediaSession = sessionBuilder.build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession

  override fun onTaskRemoved(rootIntent: Intent?) {
    val currentPlayer = player
    if (currentPlayer == null || currentPlayer.mediaItemCount == 0) {
      stopSelf()
    }
    super.onTaskRemoved(rootIntent)
  }

  override fun onDestroy() {
    mediaSession?.player?.release()
    mediaSession?.release()
    mediaSession = null
    player = null
    super.onDestroy()
  }
}
