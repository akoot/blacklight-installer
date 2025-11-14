package me.akoot.bldl

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Defaults {
    const val SHA1 = "B656B97DAB9A2ADA403D37556562D98ADD815CC9"
    const val INSTALL_LOCATION = "C:\\Program Files (x86)"
    const val ZCURE_SERVER = "blrrevive.ddd-game.de"
    const val PRESENCE_SERVER = "blrrevive.ddd-game.de"
    const val ZCURE_PORT = 80
    const val PRESENCE_PORT = 9004
}

object Preferences {
    var installationSource = installationSources.values.first()
    var checkSha1 = true
    var createShortcuts = true
    var sha1 = Defaults.SHA1
    var installLocation = Defaults.INSTALL_LOCATION
    var zcureServer = Defaults.ZCURE_SERVER
    var presenceServer = Defaults.PRESENCE_SERVER
    var zcurePort = Defaults.ZCURE_PORT
    var presencePort = Defaults.PRESENCE_PORT
}

val zipFile = File(System.getenv("temp"), "blacklightretribution.zip")
val installDir get() = File(Preferences.installLocation)
val exeFile get() = installDir.resolve("blacklightretribution\\Binaries\\Win32\\FoxGame-win32-Shipping.exe")
val installationSources = mapOf(
    "akoot.xyz (US)" to "https://akoot.xyz/dl/blr.zip",
    "ddd-cloud.com (EU)" to "https://ddd-cloud.com/blrzip",
)

val padding = 2.dp
val verticalPadding = Modifier.padding(vertical = padding)
val horizontalPadding = Modifier.padding(horizontal = padding)

const val VERSION = "1.0"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Blacklight Installer",
    ) {
        App()
    }
}

suspend fun download(updateProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder().url(Preferences.installationSource).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Bad status: ${response.code}")

        val body = response.body

        val contentLen = body.contentLength()
        val input = body.byteStream()
        val output = zipFile.outputStream()

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

suspend fun unzip(updateProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
    if (!installDir.exists()) installDir.mkdirs()

    val zis = ZipInputStream(zipFile.inputStream())
    var entry: ZipEntry?

    // First count total bytes for progress (not always perfect, but close enough)
    val totalBytes = zipFile.length()
    var processedBytes = 0L

    zis.use { zip ->
        while (true) {
            entry = zip.nextEntry ?: break

            val newFile = File(installDir, entry.name)

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
                        updateProgress(processedBytes / totalBytes.toFloat())
                    }
                }
            }

            zip.closeEntry()
        }
    }
}

val arguments
    get() = arrayOf(
        exeFile.absolutePath,
        "-zcureurl=${Preferences.zcureServer}",
        "-zcureport=${Preferences.zcurePort}",
        "-presenceurl=${Preferences.presenceServer}",
        "-presenceport=${Preferences.presencePort}",
    )

suspend fun launchGame(onExit: (Int) -> Unit) {
    println("exe: ${exeFile.absolutePath} (${if (exeFile.exists()) "exists" else "does not exist"})")
    if (!exeFile.exists()) {
        onExit(-1)
        return
    }
    onExit(
        Runtime.getRuntime().exec(
            arguments
        ).exitValue()
    )
}

suspend fun computeSha1(
    file: File,
    onProgress: (Float) -> Unit
): String {
    val digest = MessageDigest.getInstance("SHA-1")
    val total = file.length().toFloat()
    var processed = 0L

    FileInputStream(file).use { fis ->
        DigestInputStream(fis, digest).use { dis ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = dis.read(buffer)
                if (read == -1) break
                processed += read
                onProgress(processed / total)
            }
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }.uppercase()
}

val desktopShortcut =
    File("${System.getProperty("user.home")}\\Desktop\\Blacklight Retribution.lnk")
val startMenuShortcut =
    File("${System.getenv("APPDATA")}\\Microsoft\\Windows\\Start Menu\\Programs\\Blacklight Retribution.lnk")

fun createWindowsShortcut(shortcut: File) {
    val workingDirectory = exeFile.parentFile.absolutePath
    val script = buildString {
        append($$"$ws = New-Object -ComObject WScript.Shell; ")
        append($$"$s = $ws.CreateShortcut('$${shortcut.absolutePath}'); ")
        append($$"$s.TargetPath = '$${exeFile.absolutePath}'; ")

        if (arguments.isNotEmpty()) {
            append($$"$s.Arguments = '$${arguments.joinToString(" ")}'; ")
        }

        if (workingDirectory != null) {
            append($$"$s.WorkingDirectory = '$$workingDirectory'; ")
        }

        append($$"$s.Save()")
    }

    val process = ProcessBuilder(
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-Command", script
    )
        .redirectErrorStream(true)
        .start()

    process.waitFor()
}
