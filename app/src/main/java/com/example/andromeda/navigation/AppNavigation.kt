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
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit,
    useBiggerText: Boolean, // Add this
    onSetTextSize: (Boolean) -> Unit // Add this
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
                isDarkTheme = isDarkTheme,
                onSetTheme = onSetTheme,
                useBiggerText = useBiggerText, // Pass down
                onSetTextSize = onSetTextSize  // Pass down
            )
        }
        composable(Screen.Chatbot.route) { ChatbotScreen() }
    }
}
