package com.jedick.langcalc.data

import android.content.Context
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

sealed interface DownloadState {
  data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState {
    val percent: Int
      get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
  }
  data object Success : DownloadState
  data class Error(val message: String) : DownloadState
}

sealed interface UpdateCheckResult {
  data object UpToDate : UpdateCheckResult
  data object UpdateAvailable : UpdateCheckResult
  data class Error(val message: String) : UpdateCheckResult
}

/**
 * Downloads, deletes, and checks for updates to the one model file this app uses, straight from
 * the Hugging Face Hub (see ModelConstants). There's no Gallery model manager here, so this is a
 * deliberately small stand-in: one file, one repo, no resumable downloads or checksum verification
 * beyond what HTTP already gives us. See the "Remaining questions" in the project README for what
 * a production version would want to add.
 */
class ModelRepository(context: Context, private val settings: AppSettings) {
  private val modelDir = File(context.filesDir, "models")
  private val client = OkHttpClient()

  val modelFile: File
    get() = File(modelDir, ModelConstants.FILENAME)

  fun isDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 0

  fun modelSizeBytes(): Long = if (modelFile.exists()) modelFile.length() else 0L

  fun delete(): Boolean {
    settings.downloadedModelETag = null
    return !modelFile.exists() || modelFile.delete()
  }

  /** Downloads the model to a temp file and atomically renames it into place on success. */
  fun download(): Flow<DownloadState> =
    flow {
        modelDir.mkdirs()
        val tempFile = File(modelDir, "${ModelConstants.FILENAME}.part")
        val request = Request.Builder().url(ModelConstants.downloadUrl()).build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            emit(DownloadState.Error("HTTP ${response.code}"))
            return@flow
          }
          val body = response.body ?: run {
            emit(DownloadState.Error("Empty response body"))
            return@flow
          }
          val totalBytes = body.contentLength()
          var bytesDownloaded = 0L

          tempFile.outputStream().use { out ->
            body.byteStream().use { input ->
              val buffer = ByteArray(64 * 1024)
              var lastEmitPercent = -1
              while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
                bytesDownloaded += read
                val progress = DownloadState.Progress(bytesDownloaded, totalBytes)
                if (progress.percent != lastEmitPercent) {
                  lastEmitPercent = progress.percent
                  emit(progress)
                }
              }
            }
          }

          if (!tempFile.renameTo(modelFile)) {
            emit(DownloadState.Error("Couldn't finalize the downloaded file"))
            return@flow
          }
          settings.downloadedModelETag = eTagFrom(response)
          emit(DownloadState.Success)
        }
      }
      .flowOn(Dispatchers.IO)

  /**
   * HEADs the download URL (without following redirects) to read the file's current ETag and
   * compares it to the one saved after the last successful download.
   */
  suspend fun checkForUpdate(): UpdateCheckResult {
    if (!isDownloaded()) return UpdateCheckResult.Error("No model downloaded yet")
    val savedETag = settings.downloadedModelETag ?: return UpdateCheckResult.UpdateAvailable

    val noRedirectClient = client.newBuilder().followRedirects(false).build()
    val request = Request.Builder().url(ModelConstants.downloadUrl()).head().build()

    return try {
      val response = noRedirectClient.executeSuspend(request)
      response.use {
        val currentETag = eTagFrom(it)
        if (currentETag == null) {
          UpdateCheckResult.Error("Couldn't read the model's version info")
        } else if (currentETag == savedETag) {
          UpdateCheckResult.UpToDate
        } else {
          UpdateCheckResult.UpdateAvailable
        }
      }
    } catch (e: IOException) {
      UpdateCheckResult.Error(e.message ?: "Network error")
    }
  }

  /**
   * Hugging Face serves large (LFS-backed) files as a redirect to a CDN URL, with the file's real
   * content hash in "X-Linked-Etag" on the redirect response itself. Small, non-LFS files skip the
   * redirect and carry a normal "ETag" instead, so this checks both.
   */
  private fun eTagFrom(response: Response): String? =
    response.header("X-Linked-Etag") ?: response.header("ETag")
}

private suspend fun OkHttpClient.executeSuspend(request: Request): Response =
  suspendCancellableCoroutine { continuation ->
    val call = newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(
      object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
          continuation.resume(response)
        }
      }
    )
  }
