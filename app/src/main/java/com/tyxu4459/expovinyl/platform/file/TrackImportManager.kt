package com.tyxu4459.expovinyl.platform.file

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.tyxu4459.expovinyl.model.ImportProgress
import com.tyxu4459.expovinyl.model.ImportStage
import com.tyxu4459.expovinyl.model.ImportTracksResult
import com.tyxu4459.expovinyl.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TrackImportManager @Inject constructor(
  @ApplicationContext private val context: Context,
) {
  suspend fun importTracks(
    uris: List<Uri>,
    shouldCancel: (() -> Boolean)? = null,
    onProgress: ((ImportProgress) -> Unit)? = null,
  ): ImportTracksResult = withContext(Dispatchers.IO) {
    val audioDir = File(context.filesDir, "vinyl-audios").apply { mkdirs() }
    val importedTracks = mutableListOf<Track>()
    val totalCount = uris.size
    var processedCount = 0
    var failedCount = 0
    var cancelled = false

    onProgress?.invoke(
      ImportProgress(
        totalCount = totalCount,
        processedCount = 0,
        copiedCount = 0,
        failedCount = 0,
        skippedCount = 0,
        stage = ImportStage.COPYING,
      ),
    )

    for (uri in uris) {
      if (shouldCancel?.invoke() == true) {
        cancelled = true
        break
      }

      val track =
        runCatching {
          val sourceName = resolveDisplayName(uri)
          val extension = sourceName.substringAfterLast('.', "").lowercase(Locale.US)
          val safeBaseName = sanitizeFileName(
          sourceName.substringBeforeLast('.', sourceName),
        )
        val targetFile = File(
          audioDir,
          buildString {
            append(System.currentTimeMillis())
            append('-')
            append((0..9999).random())
            append('-')
            append(safeBaseName)
            if (extension.isNotBlank()) {
              append('.')
              append(extension)
            }
          },
        )

        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
          }
        } ?: return@runCatching null

        val localUri = Uri.fromFile(targetFile)
        val metadata = resolveMetadata(localUri)
          Track(
            id = 0L,
            uri = localUri.toString(),
            displayName = metadata.title ?: stripExtension(sourceName),
          artist = metadata.artist,
          album = metadata.album,
          durationMs = metadata.durationMs,
            importedAt = System.currentTimeMillis(),
          )
        }.getOrNull()

      if (track != null) {
        importedTracks += track
      } else {
        failedCount += 1
      }
      processedCount += 1

      onProgress?.invoke(
        ImportProgress(
          totalCount = totalCount,
          processedCount = processedCount,
          copiedCount = importedTracks.size,
          failedCount = failedCount,
          skippedCount = 0,
          stage = ImportStage.COPYING,
        ),
      )
    }

    val result =
      ImportTracksResult(
        tracks = importedTracks,
        totalCandidateCount = totalCount,
        processedCount = processedCount,
        copiedCount = importedTracks.size,
        failedCount = failedCount,
        skippedCount = 0,
        cancelled = cancelled,
      )

    onProgress?.invoke(
      ImportProgress(
        totalCount = totalCount,
        processedCount = processedCount,
        copiedCount = importedTracks.size,
        failedCount = failedCount,
        skippedCount = 0,
        stage = if (cancelled) ImportStage.CANCELLED else ImportStage.DONE,
      ),
    )

    result
  }

  private fun resolveDisplayName(uri: Uri): String {
    context.contentResolver.query(
      uri,
      arrayOf(OpenableColumns.DISPLAY_NAME),
      null,
      null,
      null,
    )?.use { cursor ->
      val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (index >= 0 && cursor.moveToFirst()) {
        return cursor.getString(index) ?: "Unknown"
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
  }

  private fun resolveMetadata(uri: Uri): ImportedMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      ImportedMetadata(
        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
        album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
        durationMs =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull(),
      )
    } catch (_: Exception) {
      ImportedMetadata()
    } finally {
      retriever.release()
    }
  }

  private fun sanitizeFileName(value: String): String =
    value.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "track" }

  private fun stripExtension(value: String): String =
    value.substringBeforeLast('.', value).ifBlank { value }
}

private data class ImportedMetadata(
  val title: String? = null,
  val artist: String? = null,
  val album: String? = null,
  val durationMs: Long? = null,
)
