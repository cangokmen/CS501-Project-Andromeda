package com.example.andromeda.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.andromeda.ui.screens.AddScreen
import com.example.andromeda.ui.screens.ChatbotScreen
import com.example.andromeda.ui.screens.HistoryScreen
import com.example.andromeda.ui.screens.HomeScreen
import com.example.andromeda.ui.screens.RegisterScreen
import com.example.andromeda.ui.screens.SettingsScreen
import com.example.andromeda.viewmodels.RegisterViewModel

sealed class Screen(val route: String) {
    object Register : Screen("register")
    object Home : Screen("home")
    object History : Screen("history")
    object Add : Screen("add")
    object EditEntry : Screen("edit/{wellnessDataId}") {
        fun createRoute(wellnessDataId: String) = "edit/$wellnessDataId"
    }
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
    hasProfile: Boolean,
    registerViewModel: RegisterViewModel // <-- ADDED: Pass the shared ViewModel
) {
    NavHost(
        navController = navController,
        startDestination = if (hasProfile) Screen.Home.route else Screen.Register.route,
        modifier = modifier
    ) {
        // REGISTER SCREEN
        composable(Screen.Register.route) {
            RegisterScreen(
                registerViewModel = registerViewModel, // <-- PASS: Use the shared ViewModel
                onRegistrationSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // HOME
        composable(Screen.Home.route) {
            HomeScreen()
        }

        // HISTORY
        composable(Screen.History.route) {
            HistoryScreen(
                selectedQuestions = selectedQuestions,
                onEditEntry = { wellnessDataId ->
                    navController.navigate(Screen.EditEntry.createRoute(wellnessDataId))
                }
            )
        }

        // ADD (For new entries)
        composable(Screen.Add.route) {
            AddScreen(
                selectedQuestions = selectedQuestions,
                wellnessDataId = null, // Explicitly null for a new entry
                onSaveComplete = { navController.popBackStack() }
            )
        }

        // EDIT (For existing entries)
        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(navArgument("wellnessDataId") { type = NavType.StringType })
        ) { backStackEntry ->
            val wellnessDataId = backStackEntry.arguments?.getString("wellnessDataId")
            AddScreen(
                selectedQuestions = selectedQuestions,
                wellnessDataId = wellnessDataId,
                onSaveComplete = { navController.popBackStack() }
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
