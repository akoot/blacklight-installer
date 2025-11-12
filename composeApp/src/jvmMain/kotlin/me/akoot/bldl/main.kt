package me.akoot.bldl

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BlacklightInstaller",
    ) {
        App()
    }
}

suspend fun download(
    url: String,
    dest: File,
    updateProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Bad status: ${response.code}")

        val body = response.body

        val contentLen = body.contentLength()
        val input = body.byteStream()
        val output = dest.outputStream()

        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var total = 0L

        input.use { inp ->
            output.use { out ->
                while (true) {
                    bytesRead = inp.read(buffer)
                    if (bytesRead == -1) break
                    out.write(buffer, 0, bytesRead)
                    total += bytesRead

                    if (contentLen > 0) {
                        val progress = total.toFloat() / contentLen.toFloat()
                        updateProgress(progress)
                    }
                }
            }
        }
    }
}

suspend fun unzip(
    zipFile: File,
    dest: File,
    updateProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    if (!dest.exists()) dest.mkdirs()

    val zis = ZipInputStream(zipFile.inputStream())
    var entry: ZipEntry?

    // First count total bytes for progress (not always perfect, but close enough)
    val totalBytes = zipFile.length()
    var processedBytes = 0L

    zis.use { zip ->
        while (true) {
            entry = zip.nextEntry ?: break

            val newFile = File(dest, entry.name)

            // Handle dirs
            if (entry.isDirectory) {
                newFile.mkdirs()
            } else {
                // Ensure parent dirs exist
                newFile.parentFile?.mkdirs()

                FileOutputStream(newFile).use { fos ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val len = zip.read(buffer)
                        if (len == -1) break
                        fos.write(buffer, 0, len)

                        processedBytes += len
                        val progress = processedBytes.toFloat() / totalBytes.toFloat()
                        updateProgress(progress)
                    }
                }
            }

            zip.closeEntry()
        }
    }
}
