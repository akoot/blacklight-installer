package me.akoot.bldl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun Home(onGoToSettings: () -> Unit) {
    val scope = rememberCoroutineScope()

    var progress by remember { mutableFloatStateOf(0f) }

    var buttonText by remember { mutableStateOf("Install") }

    var working by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(false) }

    MaterialTheme {
        Box {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = verticalPadding,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(onClick = {
                        working = true
                        scope.launch(Dispatchers.IO) {
                            if (installed || !zipFile.exists()) {
                                buttonText = "Downloading..."
                                download {
                                    scope.launch { progress = it }
                                }
                                if (!zipFile.exists()) {
                                    println("Download failed!")
                                    buttonText = "Install"
                                    working = false
                                    progress = 0f
                                    installed = false
                                    return@launch
                                }
                            }
                            if (Preferences.checkSha1) {
                                buttonText = "Validating..."
                                val sha1 = computeSha1(zipFile) {
                                    scope.launch { progress = it }
                                }
                                if (sha1 != Preferences.sha1) {
                                    println("SHA1 does not match!")
                                    println("Expected:  ${Preferences.sha1}")
                                    println("Actual:    $sha1")
                                    buttonText = "Install"
                                    working = false
                                    progress = 0f
                                    installed = false
                                    return@launch
                                }
                            }
                            if (installed || !exeFile.exists()) {
                                buttonText = "Installing..."
                                unzip {
                                    scope.launch { progress = it }
                                }
                                if (!exeFile.exists()) {
                                    println("exe file not found!")
                                    buttonText = "Install"
                                    working = false
                                    progress = 0f
                                    installed = false
                                    return@launch
                                }
                            }
                            if (Preferences.createShortcuts) {
                                desktopShortcut.delete()
                                startMenuShortcut.delete()
                                createWindowsShortcut(desktopShortcut)
                                createWindowsShortcut(startMenuShortcut)
                            }
                            buttonText = "Re-Install"
                            progress = 1f
                            working = false
                            installed = true
                        }
                    }, enabled = !working, modifier = verticalPadding) {
                        Text(buttonText)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = verticalPadding
                    )
                    Text("${String.format("%.1f", progress * 100f)}%")
                    ElevatedButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                // loading thing
                                launchGame {
                                    println("exit code: $it")
                                    //loaded
                                }
                            }
                        },
                        enabled = !working && installed,
                        modifier = verticalPadding.alpha(if (!working && installed) 1f else 0f)
                    ) {
                        Text("Launch")
                    }
                }
            }
            IconButton(
                onClick = {
                    onGoToSettings()
                },
                Modifier.background(MaterialTheme.colorScheme.secondaryContainer),
                enabled = !working
            ) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}