package me.akoot.bldl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class Screen {
    object HomeScreen : Screen()
    object SettingsScreen : Screen()
}

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.HomeScreen) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        }
    ) { screen ->
        when (screen) {
            Screen.HomeScreen -> Home { currentScreen = Screen.SettingsScreen }
            Screen.SettingsScreen -> Settings { currentScreen = Screen.HomeScreen }
        }
    }
}
