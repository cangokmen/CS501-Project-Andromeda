package com.example.andromeda

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
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

// A simple ViewModel to manage theme state
class MainViewModel(private val userPreferencesRepository: UserPreferencesRepository) : ViewModel() {
    val isDarkTheme: StateFlow<Boolean> = userPreferencesRepository.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Default to light theme initially
        )

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemePreference(isDark)
        }
    }

    // ViewModel Factory
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
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavItem("History", Icons.Default.DateRange, Screen.History.route),
        BottomNavItem("Add", Icons.Default.AddCircle, Screen.Add.route),
        BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
    )

    AndromedaTheme(darkTheme = useDarkTheme) {
        Scaffold(
            bottomBar = {
                BottomAppBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
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
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                isDarkTheme = useDarkTheme, // Pass the theme state
                onSetTheme = { isDark -> viewModel.setTheme(isDark) } // Pass the function to change it
            )
        }
    }
}
