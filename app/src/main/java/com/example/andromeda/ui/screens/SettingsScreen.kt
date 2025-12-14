package com.example.andromeda.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.R
import com.example.andromeda.data.UserProfile
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.viewmodels.AuthViewModel
import com.example.andromeda.viewmodels.AuthState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.math.RoundingMode

// --- Main Settings Navigation ---
// (This part remains unchanged)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit,
    useBiggerText: Boolean,
    onSetTextSize: (Boolean) -> Unit,
    selectedQuestions: Set<String>,
    onSetQuestions: (Set<String>) -> Unit,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    var screenState by remember { mutableStateOf(ScreenState.Main) }
    val authState by authViewModel.authState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (screenState) {
            ScreenState.Main -> SettingsMainScreen(
                onAccountClick = { screenState = ScreenState.Account },
                onQuestionManagementClick = { screenState = ScreenState.QuestionManagement },
                onPreferencesClick = { screenState = ScreenState.Preferences },
                onAccessibilityClick = { screenState = ScreenState.Accessibility }
            )
            ScreenState.Account -> {
                val profile = (authState as? AuthState.Authenticated)?.userProfile
                if (profile != null) {
                    AccountSettings(
                        profile = profile,
                        onUpdateProfile = { fn, ln, age, w ->
                            authViewModel.createProfile(fn, ln, age.toIntOrNull() ?: 0, w.toDoubleOrNull() ?: 0.0)
                        },
                        onBackClicked = { screenState = ScreenState.Main },
                        onLogoutClicked = onLogout
                    )
                }
            }
            ScreenState.Preferences -> PreferencesSettings(
                onBackClicked = { screenState = ScreenState.Main },
                isDarkTheme = isDarkTheme,
                onSetTheme = onSetTheme
            )
            ScreenState.Accessibility -> AccessibilitySettings(
                onBackClicked = { screenState = ScreenState.Main },
                useBiggerText = useBiggerText,
                onSetTextSize = onSetTextSize
            )
            ScreenState.QuestionManagement -> QuestionManagementScreen(
                onBackClicked = { screenState = ScreenState.Main },
                selectedQuestions = selectedQuestions,
                onSetQuestions = onSetQuestions
            )
        }
    }
}

private enum class ScreenState {
    Main, Account, QuestionManagement, Preferences, Accessibility
}

// (SettingsMainScreen and SettingsRow remain unchanged)
@Composable
private fun SettingsMainScreen(
    onAccountClick: () -> Unit,
    onQuestionManagementClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onAccessibilityClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Text(
                    text = "Help & Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(title = "Help Center", onClick = { /* TODO */ })
                        Divider()
                        SettingsRow(title = "Feedback", onClick = { /* TODO */ })
                    }
                }
            }
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(title = "Account", onClick = onAccountClick)
                        Divider()
                        SettingsRow(title = "Question Management", onClick = onQuestionManagementClick)
                        Divider()
                        SettingsRow(title = "Preferences", onClick = onPreferencesClick)
                        Divider()
                        SettingsRow(title = "Accessibility", onClick = onAccessibilityClick)
                        Divider()
                        SettingsRow(title = "Privacy Policy", onClick = { /* TODO */ })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ------------------------- CORRECTED DETAIL SCREENS ------------------------- */

@Composable
fun AccountSettings(
    profile: UserProfile,
    onUpdateProfile: (firstName: String, lastName: String, age: String, targetWeight: String) -> Unit,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wellnessDataRepository = remember { WellnessDataRepository(context) }

    // Local state for editing form
    var isEditing by remember { mutableStateOf(false) }
    var firstName by remember(profile) { mutableStateOf(profile.firstName) }
    var lastName by remember(profile) { mutableStateOf(profile.lastName) }
    var age by remember(profile) { mutableStateOf(profile.age.toString()) }
    var targetWeight by remember(profile) { mutableStateOf(profile.targetWeight.toString()) }

    var showResetDialog by remember { mutableStateOf(false) }

    val seedData: () -> Unit = {
        coroutineScope.launch {
            val inputStream = context.resources.openRawResource(R.raw.sample_wellness_data)
            val reader = InputStreamReader(inputStream)
            val listType = object : TypeToken<List<WellnessData>>() {}.type
            val dataToSeed: List<WellnessData> = Gson().fromJson(reader, listType)

            dataToSeed.forEach { data ->
                val roundedWeight = data.weight
                    .toBigDecimal()
                    .setScale(1, RoundingMode.HALF_UP)
                    .toDouble()
                wellnessDataRepository.addWellnessData(data.copy(weight = roundedWeight))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Data") },
            text = { Text("Are you sure you want to delete all saved wellness history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch { wellnessDataRepository.clearAllData() }
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                Button(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Account Details", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = {
                if (isEditing) {
                    // When saving, call the update function
                    onUpdateProfile(firstName, lastName, age, targetWeight)
                }
                // Toggle edit mode
                isEditing = !isEditing
            }) { Text(if (isEditing) "Save" else "Edit") }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isEditing) {
            // EDITING MODE UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    label = { Text("Target Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // DISPLAY MODE UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoRow(label = "First Name:", value = profile.firstName)
                InfoRow(label = "Last Name:", value = profile.lastName)
                InfoRow(label = "Age:", value = profile.age.toString())
                InfoRow(label = "Target Weight:", value = "${profile.targetWeight} kg")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { seedData() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("ADD MANUAL DATA", color = MaterialTheme.colorScheme.onSecondaryContainer)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("RESET APP DATA", color = MaterialTheme.colorScheme.onErrorContainer)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // **FIX**: Use the onLogout function passed from the NavHost
        Button(
            onClick = onLogoutClicked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("LOG OUT / RESET PROFILE", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

// (PreferencesSettings and other detail screens remain unchanged)
@Composable
fun PreferencesSettings(
    onBackClicked: () -> Unit,
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isDarkTheme, onCheckedChange = onSetTheme)
            }
        }
    }
}

@Composable
fun AccessibilitySettings(
    onBackClicked: () -> Unit,
    useBiggerText: Boolean,
    onSetTextSize: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Accessibility", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Bigger Text", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = useBiggerText, onCheckedChange = onSetTextSize)
            }
        }
    }
}

@Composable
fun QuestionManagementScreen(
    onBackClicked: () -> Unit,
    selectedQuestions: Set<String>,
    onSetQuestions: (Set<String>) -> Unit
) {
    // Map of question keys to their display names
    val allQuestions = mapOf(
        "DIET" to "Diet Rating",
        "ACTIVITY" to "Activity Rating",
        "SLEEP" to "Sleep Hours",
        "WATER" to "Water Intake",
        "PROTEIN" to "Protein Intake"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Manage Questions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Select up to 3 questions to track:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            allQuestions.forEach { (key, name) ->
                val isChecked = key in selectedQuestions
                val isEnabled = isChecked || selectedQuestions.size < 3

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEnabled) {
                            val newSelection = selectedQuestions.toMutableSet()
                            if (isChecked) {
                                newSelection.remove(key)
                            } else {
                                newSelection.add(key)
                            }
                            onSetQuestions(newSelection)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            val newSelection = selectedQuestions.toMutableSet()
                            if (it) {
                                newSelection.add(key)
                            } else {
                                newSelection.remove(key)
                            }
                            onSetQuestions(newSelection)
                        },
                        enabled = isEnabled
                    )
                    Text(text = name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}