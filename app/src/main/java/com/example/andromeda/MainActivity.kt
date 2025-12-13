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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
            // The MainApp composable now correctly calls your new theme
            MainApp()
        }
    }
}

class MainViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    // Add the Application parameter
    application: Application
) : ViewModel() {

    // --- THIS IS NEW: Add the dataClient for sending data to the watch ---
    private val dataClient by lazy { Wearable.getDataClient(application) }

    init {
        // Send the current question configuration to the watch as soon as the app starts.
        // This ensures the watch is always in sync, even if it was offline
        // when the settings were last changed on the phone.
        viewModelScope.launch {
            // Fetch the most recent value from the Flow and send it.
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

    val userEmail: StateFlow<String?> = userPreferencesRepository.userEmail
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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

    // login state
    val isLoggedIn: StateFlow<Boolean> = userPreferencesRepository.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
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

    // --- THIS FUNCTION IS MODIFIED ---
    fun setQuestions(questions: Set<String>) {
        viewModelScope.launch {
            // 1. Save locally
            userPreferencesRepository.saveSelectedQuestions(questions)
            // 2. Send to the watch
            sendQuestionsToWatch(questions)
        }
    }

    // --- THIS IS THE NEW FUNCTION THAT SENDS THE DATA ---
    private fun sendQuestionsToWatch(questions: Set<String>) {
        viewModelScope.launch {
            try {
                val putDataMapRequest = PutDataMapRequest.create("/config_questions").apply {
                    val questionsJson = Gson().toJson(questions)
                    // --- ADD THIS LOG ---
                    // This will print the exact JSON string being sent.
                    println("PHONE: Sending questions to watch: $questionsJson")

                    dataMap.putString("KEY_QUESTIONS_LIST", questionsJson)
                    dataMap.putLong("KEY_TIMESTAMP", System.currentTimeMillis())
                }
                val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()

                dataClient.putDataItem(putDataRequest).apply {
                    addOnSuccessListener {
                        // Log success
                        println("PHONE: Successfully sent /config_questions data item.")
                    }
                    addOnFailureListener { e ->
                        // Log failure
                        println("PHONE: Failed to send /config_questions data item: $e")
                    }
                }
            } catch (e: Exception) {
                println("PHONE: Error creating data item for watch: $e")
            }
        }
    }


    fun logout() {
        viewModelScope.launch {
            userPreferencesRepository.logoutUser()
        }
    }

    // --- THIS FACTORY IS MODIFIED to pass the Application ---
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(
                    userPreferencesRepository = UserPreferencesRepository(application),
                    application = application // Pass it here
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
    viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val useDarkTheme by viewModel.isDarkTheme.collectAsState()
    val useBiggerText by viewModel.useBiggerText.collectAsState()
    val selectedQuestions by viewModel.selectedQuestions.collectAsState()
    val currentUserEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

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
                // --- MODIFIED: Conditionally show the BottomAppBar ---
                if (currentRoute != Screen.Login.route) {
                    BottomAppBar(
                        // Set the background color of the entire bar to our primary green color
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
                                // Define the colors for the items on the green background
                                colors = NavigationBarItemDefaults.colors(
                                    // Color when tab is selected (e.g., White)
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    // Color when tab is not selected (a muted version of the selected color)
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    // The ripple effect color when you tap the item
                                    indicatorColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Hide FAB on Login and Chatbot screens
                if (currentRoute != Screen.Chatbot.route &&
                    currentRoute != Screen.Login.route
                ) {
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
                        // Set the background color of the button to your theme's secondary color
                        containerColor = MaterialTheme.colorScheme.secondary,
                        // Set the icon color to be readable on the secondary color
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.galaxy),
                            contentDescription = "Chatbot"
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
                onSetQuestions = viewModel::setQuestions,
                onLogout = viewModel::logout,
                isLoggedIn = isLoggedIn,
                currentUserEmail = currentUserEmail
            )
        }
    }
}
