package me.akoot.bldl

sealed class Screen {
    object Home : Screen()
    object Settings : Screen()
}