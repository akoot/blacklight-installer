package me.akoot.bldl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blacklightinstaller.composeapp.generated.resources.Res
import blacklightinstaller.composeapp.generated.resources.compose_multiplatform
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import kotlin.math.roundToInt

@Composable
@Preview
fun App() {
    var expandedServer by remember { mutableStateOf(false) }
    val menuItemData = mapOf(
        "ddd-cloud.com (EU)" to "https://getsamplefiles.com/download/zip/sample-3.zip",
        "cloud.akoot.co (US)" to
                "https://akoot.xyz/dl/blr.zip",
        "archive.org (Slow AF)" to ""
    )
    var selectedServer = menuItemData.keys.first()
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Create a coroutine scope
    val zipFile = File(System.getenv("temp"), "blr.zip")
    var installPath  by remember { mutableStateOf("C:\\Program Files (x86)") }
    var buttonText = "Download"
    var installed by remember { mutableStateOf(false) }
    var size by remember { mutableLongStateOf(0) }
    var downloaded by remember { mutableStateOf(false) }
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                    Row(modifier = Modifier.safeContentPadding()) {
                        Text("Provider")
                        Text(selectedServer)
                        IconButton(onClick = { expandedServer = !expandedServer }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = expandedServer,
                            onDismissRequest = { expandedServer = false }
                        ) {
                            menuItemData.keys.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        expandedServer = !expandedServer
                                        selectedServer = option
                                    },
                                    trailingIcon = {
                                        Icon(
                                            if (option == selectedServer) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                            contentDescription = ""
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.safeContentPadding()) {
                        Text("Install Location")
                        Text(installPath)
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                val directory =
                                    FileKit.openDirectoryPicker("Select folder to install Blacklight: Retribution")
                                scope.launch(Dispatchers.Main) {
                                    println(directory)
                                    directory?.file?.absolutePath?.let { installPath = it }
                                }
                            }
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
                    Button(onClick = {
                        if (installed) {
                            return@Button
                        }
                        loading = true
                        scope.launch(Dispatchers.IO) {
                            buttonText = "Downloading..."
                            download(menuItemData[selectedServer]!!, zipFile, {
                                scope.launch(Dispatchers.Main) {
                                    size = it
                                }
                                                                              }, {
                                scope.launch(Dispatchers.Main) {
                                    currentProgress = it
                                }
                            })
                            downloaded = true
                            buttonText = "Installing..."
                            val installDir = File(installPath)
                            installDir.mkdirs()
                            unzip(zipFile, installDir, {
                                scope.launch(Dispatchers.Main) {
                                    size = it
                                }
                                                       }, {
                                scope.launch(Dispatchers.Main) {
                                    currentProgress = it
                                }
                            })
                            currentProgress = 1f
                            buttonText = "Re-Install"
                            loading = false // Reset loading when the coroutine finishes
                            installed = true
                        }
                    }, enabled = !loading) {
                        Text(buttonText)
                    }
                    LinearProgressIndicator(
                        progress = { currentProgress }
                    )
                    Text("${((currentProgress * size) / 1000000).toInt()} / ${size / 1000000} MB (${String.format("%.2f", currentProgress * 100f)}%)")
                    Button(onClick = {
                        val exeFile = File(installPath).resolve("blacklightretribution").resolve("Binaries").resolve("Win32").resolve("FoxGame-win32-Shipping.exe")
                        println(exeFile.absolutePath)
                        if(!exeFile.exists()) {
                            return@Button
                        }
                        Runtime.getRuntime().exec(arrayOf(exeFile.absolutePath, "-zcureurl=blrrevive.ddd-game.de", "-zcureport=80", "-presenceurl=blrrevive.ddd-game.de", "-presenceport=9004",))
                    }, enabled = installed) {
                        Text("Launch")
                    }
                }
            }
        }
    }
}
