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
    //if (!isRunningAsAdmin()) relaunchAsAdmin()
    Window(
        onCloseRequest = ::exitApplication,
        title = "BlacklightInstaller",
    ) {
        Settings()
    }
}

object Defaults {
    const val SHA1 = "1C4E002FBB1F106387038E3848F56E21E14E766F"
    const val INSTALL_LOCATION = "C:\\Program Files (x86)"
    const val ZCURE_SERVER = "blrrevive.ddd-game.de"
    const val PRESENCE_SERVER = "blrrevive.ddd-game.de"
    const val ZCURE_PORT = 80
    const val PRESENCE_PORT = 9004
}

suspend fun download(
    url: String,
    dest: File,
    updateLength: (Long) -> Unit,
    updateProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Bad status: ${response.code}")

        val body = response.body

        val contentLen = body.contentLength()
        updateLength(contentLen)
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
                        updateProgress(total / contentLen.toFloat())
                    }
                }
            }
        }
    }
}

suspend fun unzip(
    zipFile: File,
    dest: File,
    updateLength: (Long) -> Unit,
    updateProgress: (Float, String) -> Unit
) = withContext(Dispatchers.IO) {
    if (!dest.exists()) dest.mkdirs()

    val zis = ZipInputStream(zipFile.inputStream())
    var entry: ZipEntry?

    // First count total bytes for progress (not always perfect, but close enough)
    val totalBytes = zipFile.length()
    updateLength(totalBytes)
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
                        updateProgress(processedBytes / totalBytes.toFloat(), newFile.name)
                    }
                }
            }

            zip.closeEntry()
        }
    }
}

suspend fun launchGame(installLocation: File, zcureServer: String = "blrrevive.ddd-game.de", presenceServer: String = zcureServer, zcurePort: Int = 80, presencePort: Int = 9004, onExit: (Int) -> Unit) = withContext(Dispatchers.IO) {
    val exeFile = installLocation.resolve("blacklightretribution\\Binaries\\Win32\\FoxGame-win32-Shipping.exe")
    println("exe: ${exeFile.absolutePath}")
    if (!exeFile.exists()) {
        onExit(-1)
        return@withContext
    }
    onExit(Runtime.getRuntime().exec(
        arrayOf(
            exeFile.absolutePath,
            "-zcureurl=$zcureServer",
            "-zcureport=$zcurePort",
            "-presenceurl=$presenceServer",
            "-presenceport=$presencePort",
        )
    ).exitValue())
}
