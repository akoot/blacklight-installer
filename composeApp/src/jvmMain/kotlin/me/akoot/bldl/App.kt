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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import blacklightinstaller.composeapp.generated.resources.Res
import blacklightinstaller.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
@Preview
fun App() {
    var expanded by remember { mutableStateOf(false) }
    val menuItemData = mapOf("ddd-cloud.com (EU)" to "https://getsamplefiles.com/download/zip/sample-1.zip", "cloud.akoot.co (US)" to
            "https://akoot.xyz/dl/blr.zip"
        , "archive.org (Slow AF)" to "")
    var selectedServer = menuItemData.keys.first()
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Create a coroutine scope
    val zipFile = File(System.getenv("temp"),"blr.zip")
    val installDir = File("C:\\Program Files (x86)\\blacklightretribution")
    var buttonText = "Download"
    installDir.mkdirs()
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
                    Row {
                        Button(onClick = {
                            loading = true
                            scope.launch(Dispatchers.IO) {
                                buttonText = "Downloading..."
                                download(menuItemData[selectedServer]!!, zipFile) { progress ->
                                    scope.launch(Dispatchers.Main) {
                                        currentProgress = progress
                                    }
                                }
                                buttonText = "Extracting..."
                                unzip(zipFile, installDir) {
                                    scope.launch(Dispatchers.Main) {
                                        currentProgress = it
                                    }
                                }
                                currentProgress = 1f
                                buttonText = "Re-Install"
                                loading = false // Reset loading when the coroutine finishes
                            }
                        },enabled = !loading) {
                            Text(buttonText)
                        }

                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            menuItemData.keys.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        expanded = !expanded
                                        selectedServer = option
                                              },
                                    trailingIcon = {Icon(if(option == selectedServer) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline, contentDescription = "")}
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { currentProgress },
                    )
                    Text("${String.format("%.2f", currentProgress * 100f)}%")
                }
            }
        }
    }
}