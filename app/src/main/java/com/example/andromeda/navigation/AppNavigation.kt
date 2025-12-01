package com.example.andromeda.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.andromeda.ui.screens.AddScreen
import com.example.andromeda.ui.screens.ChatbotScreen
import com.example.andromeda.ui.screens.HistoryScreen
import com.example.andromeda.ui.screens.HomeScreen
import com.example.andromeda.ui.screens.SettingsScreen
import com.example.andromeda.ui.screens.LoginScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object History : Screen("history")
    object Add : Screen("add")
    object Settings : Screen("settings")
    object Chatbot : Screen("chatbot")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit,
    useBiggerText: Boolean,
    onSetTextSize: (Boolean) -> Unit,
    selectedQuestions: Set<String>,
    onSetQuestions: (Set<String>) -> Unit,
    onLogout: () -> Unit,
    isLoggedIn: Boolean,
    currentUserEmail: String?
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route,
        modifier = modifier
    ) {
        // LOGIN
        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // HOME
        composable(Screen.Home.route) {
            HomeScreen(currentUserEmail = currentUserEmail)
        }

        // HISTORY
        composable(Screen.History.route) {
            HistoryScreen(
                selectedQuestions = selectedQuestions,
                currentUserEmail = currentUserEmail
            )
        }

        // ADD
        composable(Screen.Add.route) {
            AddScreen(
                selectedQuestions = selectedQuestions,
                currentUserEmail = currentUserEmail
            )
        }

        // SETTINGS
        composable(Screen.Settings.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onSetTheme = onSetTheme,
                useBiggerText = useBiggerText,
                onSetTextSize = onSetTextSize,
                selectedQuestions = selectedQuestions,
                onSetQuestions = onSetQuestions,
                onLogout = onLogout
            )
        }

        // CHATBOT
        composable(Screen.Chatbot.route) {
            ChatbotScreen()
        }
    }
}
