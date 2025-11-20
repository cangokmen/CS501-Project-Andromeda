package com.example.andromeda

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
// Add this import for the chat icon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
// Add this import for the FloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.andromeda.data.UserPreferencesRepository
import com.example.andromeda.navigation.AppNavHost
import com.example.andromeda.navigation.Screen
import com.example.andromeda.ui.theme.AndromedaTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }
}

// ViewModel and data class definitions remain unchanged...
class MainViewModel(private val userPreferencesRepository: UserPreferencesRepository) :
    ViewModel() {
    val isDarkTheme: StateFlow<Boolean> = userPreferencesRepository.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val useBiggerText: StateFlow<Boolean> = userPreferencesRepository.useBiggerText
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val selectedQuestions: StateFlow<Set<String>> = userPreferencesRepository.selectedQuestions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf("DIET", "ACTIVITY", "SLEEP") // Default initial value
        )

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemePreference(isDark)
        }
    }

    fun setTextSize(useBigger: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveTextSizePreference(useBigger)
        }
    }

    fun setQuestions(questions: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.saveSelectedQuestions(questions)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(UserPreferencesRepository(application)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val useDarkTheme by viewModel.isDarkTheme.collectAsState()
    val useBiggerText by viewModel.useBiggerText.collectAsState()
    val selectedQuestions by viewModel.selectedQuestions.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, Screen.Home.route),
        BottomNavItem("History", Icons.Default.DateRange, Screen.History.route),
        BottomNavItem("Add", Icons.Default.AddCircle, Screen.Add.route),
        BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
    )

    val fontScale = if (useBiggerText) 1.25f else 1.0f

    AndromedaTheme(
        darkTheme = useDarkTheme,
        fontScale = fontScale
    ) {
        Scaffold(
            bottomBar = {
                BottomAppBar {
                    val currentDestination = navBackStackEntry?.destination
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute != Screen.Chatbot.route) {
                    FloatingActionButton(
                        onClick = {
                            // --- MODIFICATION START ---
                            // Use the same navigation logic as the BottomAppBar
                            navController.navigate(Screen.Chatbot.route) {
                                // This ensures that if the user is on a main screen (like Home)
                                // and navigates to Chatbot, pressing back will correctly
                                // return them to Home with the navigation state preserved.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            // --- MODIFICATION END ---
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Open Chatbot"
                        )
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                isDarkTheme = useDarkTheme,
                onSetTheme = viewModel::setTheme,
                useBiggerText = useBiggerText,
                onSetTextSize = viewModel::setTextSize,
                selectedQuestions = selectedQuestions,
                onSetQuestions = viewModel::setQuestions
            )
        }
    }
}
