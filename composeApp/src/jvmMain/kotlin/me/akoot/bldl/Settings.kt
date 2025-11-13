package me.akoot.bldl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

val digitsOnly = Regex("\\d*")
val validHost = Regex("[a-zA-Z.-]*")
val validSHA1 = Regex("[A-F0-9]*")
val hostValidator = InputTransformation.then {
    if (!asCharSequence().matches(validHost)) {
        revertAllChanges()
    }
}
val portValidator = InputTransformation.maxLength(5).then {
    if (!asCharSequence().matches(digitsOnly)) {
        revertAllChanges()
    }
}
val sha1Validator = InputTransformation.maxLength(40).then {
    if (!asCharSequence().matches(validSHA1)) {
        revertAllChanges()
    }
}

@Composable
@Preview
fun Settings() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Settings) }
    val scope = rememberCoroutineScope()

    var checkSha1 by remember { mutableStateOf(true) }
    val sha1 = rememberTextFieldState(initialText = Defaults.SHA1)

    var selectedServer by remember { mutableStateOf(installationSources.keys.first()) }

    val installLocation = rememberTextFieldState(Defaults.INSTALL_LOCATION)

    var createShortcuts by remember { mutableStateOf(true) }

    val zcureServer = rememberTextFieldState(initialText = Defaults.ZCURE_SERVER)
    val presenceServer = rememberTextFieldState(initialText = Defaults.PRESENCE_SERVER)

    val zcurePort = rememberTextFieldState(initialText = Defaults.ZCURE_PORT.toString())
    val presencePort = rememberTextFieldState(initialText = Defaults.PRESENCE_PORT.toString())

    var expandedServerSelector by remember { mutableStateOf(false) }

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
                Row(
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Download from", modifier = horizontalPadding)
                    TextButton(
                        onClick = { expandedServerSelector = !expandedServerSelector },
                        modifier = horizontalPadding
                    ) {
                        Text(selectedServer)
                    }
                    DropdownMenu(
                        expanded = expandedServerSelector,
                        onDismissRequest = { expandedServerSelector = false }
                    ) {
                        installationSources.keys.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    expandedServerSelector = !expandedServerSelector
                                    selectedServer = option
                                },
                                trailingIcon = {
                                    Icon(
                                        if (option == selectedServer) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                        contentDescription = "Selected"
                                    )
                                }
                            )
                        }
                    }
                }
                Row(
                    verticalPadding.background(MaterialTheme.colorScheme.secondaryContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Check SHA1")
                    Checkbox(checked = checkSha1, onCheckedChange = { checkSha1 = it })
                    Box {
                        TextField(
                            sha1,
                            readOnly = !checkSha1,
                            inputTransformation = sha1Validator,
                            modifier = Modifier.alpha(if (checkSha1) 1f else 0.25f).background(MaterialTheme.colorScheme.tertiaryContainer)
                        )
                        IconButton(
                            onClick = {
                                sha1.edit { replace(0, length, Defaults.SHA1) }
                            },
                            modifier = Modifier.alpha(if (checkSha1) 1f else 0.25f)
                                .align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.Refresh, "Reset SHA1")
                        }
                    }
                }
                Row(verticalPadding, verticalAlignment = Alignment.CenterVertically) {
                    Text("Install to", modifier = horizontalPadding)
                    Box {
                        TextField(installLocation, modifier = horizontalPadding)
                        IconButton(onClick = {
                            installLocation.edit { replace(0, length, Defaults.INSTALL_LOCATION) }
                        }, Modifier.align(Alignment.CenterEnd)) {
                            Icon(Icons.Default.Refresh, "Reset Install Location")
                        }
                    }
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val directory =
                                FileKit.openDirectoryPicker("Select folder to install Blacklight: Retribution")
                                    ?: return@launch
                            scope.launch(Dispatchers.Main) {
                                installLocation.edit {
                                    replace(0, length, directory.absolutePath())
                                }
                            }
                        }
                    }, modifier = horizontalPadding) {
                        Icon(Icons.Default.Folder, "Select Location")
                    }
                }
                Row(verticalPadding, verticalAlignment = Alignment.CenterVertically) {
                    Text("Create Shortcuts", modifier = horizontalPadding)
                    Checkbox(
                        checked = createShortcuts,
                        onCheckedChange = { createShortcuts = it },
                        modifier = horizontalPadding
                    )
                }
                Column(Modifier.alpha(if (createShortcuts) 1f else 0.25f)) {
                    Text("ZCure Server", verticalPadding)
                    Row(verticalPadding, verticalAlignment = Alignment.CenterVertically) {
                        Text("Host", modifier = horizontalPadding)
                        Box {
                            TextField(
                                zcureServer,
                                inputTransformation = hostValidator,
                                modifier = horizontalPadding
                            )
                            IconButton(onClick = {
                                zcureServer.edit { replace(0, length, Defaults.ZCURE_SERVER) }
                            }, Modifier.align(Alignment.CenterEnd)) {
                                Icon(Icons.Default.Refresh, "Reset ZCure Host")
                            }
                        }
                        Text("Port", modifier = horizontalPadding)
                        Box {
                            TextField(
                                zcurePort,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                inputTransformation = portValidator,
                                modifier = horizontalPadding
                            )
                            IconButton(onClick = {
                                zcurePort.edit {
                                    replace(
                                        0,
                                        length,
                                        Defaults.ZCURE_PORT.toString()
                                    )
                                }
                            }, Modifier.align(Alignment.CenterEnd)) {
                                Icon(Icons.Default.Refresh, "Reset ZCure Port")
                            }
                        }
                    }
                    Text("Persistence Server", verticalPadding)
                    Row(verticalPadding, verticalAlignment = Alignment.CenterVertically) {
                        Text("Host", modifier = horizontalPadding)
                        Box {
                            TextField(
                                presenceServer,
                                inputTransformation = hostValidator,
                                modifier = horizontalPadding
                            )
                            IconButton(onClick = {
                                presenceServer.edit { replace(0, length, Defaults.PRESENCE_SERVER) }
                            }, Modifier.align(Alignment.CenterEnd)) {
                                Icon(Icons.Default.Refresh, "Reset ZCure Host")
                            }
                        }
                        Text("Port", modifier = horizontalPadding)
                        Box {
                            TextField(
                                presencePort,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                inputTransformation = portValidator,
                                modifier = horizontalPadding
                            )
                            IconButton(onClick = {
                                presencePort.edit {
                                    replace(
                                        0,
                                        length,
                                        Defaults.PRESENCE_PORT.toString()
                                    )
                                }
                            }, Modifier.align(Alignment.CenterEnd)) {
                                Icon(Icons.Default.Refresh, "Reset Presence Port")
                            }
                        }
                    }
                }
            }
        }
        SmallFloatingActionButton(onClick = {
            Preferences.installationSource = selectedServer
            Preferences.installLocation = installLocation.text.toString()
            Preferences.createShortcuts = createShortcuts
            Preferences.checkSha1 = checkSha1
            Preferences.sha1 = sha1.text.toString()
            Preferences.presenceServer = presenceServer.text.toString()
            Preferences.presencePort = presencePort.text.toString().toInt()
            Preferences.zcureServer = zcureServer.text.toString()
            Preferences.zcurePort = zcurePort.text.toString().toInt()
        }) {
            Icon(Icons.Default.ArrowBackIosNew, "Go Back")
        }
    }
}