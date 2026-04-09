package com.tyxu4459.expovinyl.platform.file

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.tyxu4459.expovinyl.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val UNKNOWN_ALBUM = "Unknown Album"

@Singleton
class DeviceAudioScanner @Inject constructor(
  @ApplicationContext private val context: Context,
) {
  suspend fun scanAlbums(): Map<String, List<Track>> = withContext(Dispatchers.IO) {
    val projection =
      arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.DATE_ADDED,
      )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC}!=0"
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val rows = mutableListOf<ScannedAudioRow>()
    context.contentResolver.query(
      collection,
      projection,
      selection,
      null,
      null,
    )?.use { cursor ->
      val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
      val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
      val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
      val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
      val trackIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
      val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

      while (cursor.moveToNext()) {
        val id = cursor.getLong(idIndex)
        val displayName = cursor.getString(displayNameIndex).orEmpty().ifBlank { "Unknown" }
        val title = stripExtension(displayName)
        val artist = cursor.getString(artistIndex)?.normalizeMetadata()
        val album = cursor.getString(albumIndex)?.normalizeMetadata().ifNullOrBlank(UNKNOWN_ALBUM)
        val durationMs = cursor.getLongOrNullCompat(durationIndex)?.takeIf { it > 0L }
        val rawTrackNumber = cursor.getIntOrNullCompat(trackIndex) ?: 0
        val normalizedTrackNumber =
          when {
            rawTrackNumber <= 0 -> Int.MAX_VALUE
            rawTrackNumber >= 1000 -> rawTrackNumber % 1000
            else -> rawTrackNumber
          }
        val dateAddedSeconds = cursor.getLongOrNullCompat(dateAddedIndex) ?: 0L
        val uri = ContentUris.withAppendedId(collection, id).toString()

        rows +=
          ScannedAudioRow(
            album = album,
            trackNumber = normalizedTrackNumber,
            track =
              Track(
                id = 0L,
                uri = uri,
                displayName = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                importedAt = dateAddedSeconds * 1000L,
              ),
          )
      }
    }

    rows
      .groupBy { it.album }
      .mapValues { (_, tracks) ->
        tracks
          .sortedWith(
            compareBy<ScannedAudioRow>({ it.trackNumber }, { it.track.displayName.lowercase() }),
          )
          .map { it.track }
      }
      .toSortedMap(String.CASE_INSENSITIVE_ORDER)
  }

  private fun String?.ifNullOrBlank(defaultValue: String): String =
    this?.takeUnless { it.isBlank() } ?: defaultValue

  private fun String.normalizeMetadata(): String =
    trim().takeUnless { it.isBlank() || it.equals("<unknown>", ignoreCase = true) } ?: ""

  private fun stripExtension(value: String): String =
    value.substringBeforeLast('.', value).ifBlank { value }
}

private data class ScannedAudioRow(
  val album: String,
  val trackNumber: Int,
  val track: Track,
)

private fun android.database.Cursor.getLongOrNullCompat(index: Int): Long? =
  if (isNull(index)) {
    null
  } else {
    getLong(index)
  }

private fun android.database.Cursor.getIntOrNullCompat(index: Int): Int? =
  if (isNull(index)) {
    null
  } else {
    getInt(index)
  }
