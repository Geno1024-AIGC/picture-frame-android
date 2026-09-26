package com.geno1024.pictureframe.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Available(val info: CanaryInfo) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

sealed interface DownloadResult {
    data class Success(val file: File) : DownloadResult
    data class Failed(val message: String) : DownloadResult
}

object Updater {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    /**
     * Fetches the canary manifest.
     *
     * This always goes to GitHub directly on purpose. The manifest is a few
     * hundred bytes, so even a slow link is fine, and reading the expected
     * sha256 from a channel the user chose would defeat the point of verifying
     * the download.
     */
    suspend fun check(currentRunNumber: Int): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val connection = open(CanaryManifest.DIRECT_URL)
            if (connection.responseCode !in 200..299) {
                return@withContext UpdateResult.Failed("检查更新失败：HTTP ${connection.responseCode}")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val info = CanaryManifest.parse(text)
                ?: return@withContext UpdateResult.Failed("无法解析更新信息")
            if (info.runNumber <= currentRunNumber) {
                UpdateResult.UpToDate
            } else {
                UpdateResult.Available(info)
            }
        } catch (e: IOException) {
            UpdateResult.Failed("检查更新失败：${e.message ?: "网络错误"}")
        }
    }

    /**
     * Downloads the APK through [mirror], verifying its sha256 against the
     * digest that was fetched directly from GitHub in [check].
     *
     * Returns the first mirror that yields a file whose digest matches; the
     * caller can retry with the next preset.
     */
    suspend fun download(
        info: CanaryInfo,
        mirror: Mirror,
        cacheDir: File,
        onProgress: (Float) -> Unit = {},
    ): DownloadResult = withContext(Dispatchers.IO) {
        val target = File(cacheDir, "canary-${info.runNumber}.apk")
        val url = mirror.wrap(CanaryManifest.apkUrl())
        try {
            val connection = open(url)
            if (connection.responseCode !in 200..299) {
                return@withContext DownloadResult.Failed("HTTP ${connection.responseCode}")
            }
            val expected = info.sizeBytes
            val total = contentLength(connection, expected)
            var received = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        received += read
                        onProgress(if (total > 0) received.toFloat() / total else -1f)
                    }
                }
            }
            if (expected > 0 && received != expected) {
                target.delete()
                return@withContext DownloadResult.Failed("文件不完整（$received/$expected 字节）")
            }
            val actual = sha256(target)
            if (actual != info.sha256) {
                target.delete()
                return@withContext DownloadResult.Failed("校验失败，镜像可能已被篡改")
            }
            DownloadResult.Success(target)
        } catch (e: IOException) {
            target.delete()
            DownloadResult.Failed(e.message ?: "下载失败")
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "*/*")
        }

    private fun contentLength(connection: HttpURLConnection, fallback: Long): Long {
        val declared = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
        return if (declared > 0) declared else fallback
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
