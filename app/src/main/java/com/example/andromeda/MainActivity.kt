package com.example.andromeda

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import com.example.andromeda.ui.theme.DarkGreen
import com.example.andromeda.viewmodels.RegisterViewModel
import com.example.andromeda.viewmodels.RegisterState
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }
}

class MainViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) : ViewModel() {

    private val dataClient by lazy { Wearable.getDataClient(application) }

    init {
        viewModelScope.launch {
            val currentQuestions = userPreferencesRepository.selectedQuestions.first()
            sendQuestionsToWatch(currentQuestions)
        }
    }

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
            initialValue = setOf("DIET", "ACTIVITY", "SLEEP")
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
            sendQuestionsToWatch(questions)
        }
    }

    private fun sendQuestionsToWatch(questions: Set<String>) {
        viewModelScope.launch {
            try {
                val putDataMapRequest = PutDataMapRequest.create("/config_questions").apply {
                    val questionsJson = Gson().toJson(questions)
                    dataMap.putString("KEY_QUESTIONS_LIST", questionsJson)
                    dataMap.putLong("KEY_TIMESTAMP", System.currentTimeMillis())
                }
                val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()

                dataClient.putDataItem(putDataRequest).apply {
                    addOnSuccessListener {
                        println("PHONE: Successfully sent /config_questions data item.")
                    }
                    addOnFailureListener { e ->
                        println("PHONE: Failed to send /config_questions data item: $e")
                    }
                }
            } catch (e: Exception) {
                println("PHONE: Error creating data item for watch: $e")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(
                    userPreferencesRepository = UserPreferencesRepository(application),
                    application = application
                ) as T
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainApp(
    mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    ),
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val useDarkTheme by mainViewModel.isDarkTheme.collectAsState()
    val useBiggerText by mainViewModel.useBiggerText.collectAsState()
    val selectedQuestions by mainViewModel.selectedQuestions.collectAsState()

    // Get auth state directly from AuthViewModel
    val authState by registerViewModel.registerState.collectAsState()

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
                // Only show bottom bar if a profile exists
                if (authState is RegisterState.Authenticated) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
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
                                label = { Text(item.label) },

                                /* AI Suggested this: To make the FAB truly transparent in Material 3,
                                 * the default elevation must be overridden to 0.dp. This removes the
                                 * underlying surface tint that shows through the transparent container.
                                 */
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    indicatorColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Only show FAB if a profile exists
                if (authState is RegisterState.Authenticated && currentRoute != Screen.Chatbot.route) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(Screen.Chatbot.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        containerColor = DarkGreen.copy(alpha = 0.2f),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.galaxy),
                            contentDescription = "Chatbot",
                            modifier = Modifier.graphicsLayer(alpha = 0.8f) // Correct way to set transparency
                        )

                    }
                }
            }
        ) { innerPadding ->
            // Show a loading indicator while checking auth status
            if (authState is RegisterState.Loading) {
                // You can add a centered CircularProgressIndicator here
            } else {
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    isDarkTheme = useDarkTheme,
                    onSetTheme = { mainViewModel.setTheme(it) },
                    useBiggerText = useBiggerText,
                    onSetTextSize = { mainViewModel.setTextSize(it) },
                    selectedQuestions = selectedQuestions,
                    onSetQuestions = { mainViewModel.setQuestions(it) },
                    // Pass the hasProfile state directly to the NavHost
                    hasProfile = authState is RegisterState.Authenticated,
                    onLogout = { registerViewModel.logout() },
                    registerViewModel = registerViewModel
                )
            }
        }
    }
}
