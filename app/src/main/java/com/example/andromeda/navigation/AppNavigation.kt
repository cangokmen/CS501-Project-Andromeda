package com.example.andromeda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.andromeda.ui.screens.AddScreen
import com.example.andromeda.ui.screens.ChatbotScreen
import com.example.andromeda.ui.screens.HistoryScreen
import com.example.andromeda.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object History : Screen("history")
    object Add : Screen("add")
    object Settings : Screen("settings")
    object Chatbot : Screen("chatbot")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean, // Add the missing parameter here
    onSetTheme: (Boolean) -> Unit // Also correcting this to non-nullable to match
) {
    NavHost(
        navController = navController,
        startDestination = Screen.History.route,
        modifier = modifier
    ) {
        composable(Screen.History.route) { HistoryScreen() }
        composable(Screen.Add.route) { AddScreen() }
        composable(Screen.Settings.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme, // Now this will resolve correctly
                onSetTheme = onSetTheme
            )
        }
        composable(Screen.Chatbot.route) { ChatbotScreen() }
    }
}
