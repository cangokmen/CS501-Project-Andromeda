package com.example.andromeda.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.R
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.viewmodels.AccountViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.math.RoundingMode

// Defines the individual screens within the settings menu
private data class SettingItem(val title: String, val icon: ImageVector, val onClick: () -> Unit)

// Enum to manage the current visible screen state
private enum class ScreenState {
    Main, Account, QuestionManagement, Preferences, Accessibility
}

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit,
    useBiggerText: Boolean,
    onSetTextSize: (Boolean) -> Unit,
    selectedQuestions: Set<String>,
    onSetQuestions: (Set<String>) -> Unit,
    onLogout: () -> Unit
) {
    var screenState by remember { mutableStateOf(ScreenState.Main) }

    AnimatedContent(
        targetState = screenState,
        label = "Settings Animation",
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { fullWidth -> fullWidth } + fadeIn() togetherWith
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()
            } else {
                slideInHorizontally { fullWidth -> -fullWidth } + fadeIn() togetherWith
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
            }
        }
    ) { targetState ->
        when (targetState) {
            ScreenState.Main -> {
                val settingsItems = listOf(
                    SettingItem("Account", Icons.Default.AccountCircle) { screenState = ScreenState.Account },
                    SettingItem("Question Management", Icons.Default.Build) { screenState = ScreenState.QuestionManagement },
                    SettingItem("Preferences", Icons.Default.Face) { screenState = ScreenState.Preferences },
                    SettingItem("Accessibility", Icons.Default.Info) { screenState = ScreenState.Accessibility }
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(settingsItems.size) { index ->
                            val item = settingsItems[index]
                            ListItem(
                                headlineContent = { Text(item.title) },
                                leadingContent = { Icon(item.icon, contentDescription = item.title) },
                                modifier = Modifier.clickable { item.onClick() }
                            )
                            if (index < settingsItems.size - 1) {
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
            ScreenState.Account -> AccountSettings(
                onBackClicked = { screenState = ScreenState.Main },
                onLogoutClicked = onLogout
            )
            ScreenState.QuestionManagement -> QuestionManagementScreen(
                onBackClicked = { screenState = ScreenState.Main },
                selectedQuestions = selectedQuestions,
                onSetQuestions = onSetQuestions
            )
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
        }
    }
}


@Composable
fun AccountSettings(
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    viewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wellnessRepo = remember { WellnessDataRepository(context) }
    var showResetDialog by remember { mutableStateOf(false) }

    val seedData: () -> Unit = {
        coroutineScope.launch {
            uiState.profile?.email?.let { email ->
                val inputStream = context.resources.openRawResource(R.raw.sample_wellness_data)
                val reader = InputStreamReader(inputStream)
                val listType = object : TypeToken<List<com.example.andromeda.data.WellnessData>>() {}.type
                val dataToSeed: List<com.example.andromeda.data.WellnessData> = Gson().fromJson(reader, listType)

                dataToSeed.forEach { data ->
                    val roundedWeight = data.weight.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
                    wellnessRepo.addWellnessData(data.copy(weight = roundedWeight, userEmail = email))
                }
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
                        coroutineScope.launch { wellnessRepo.clearAllData() }
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = { Button(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Account Details", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = viewModel::toggleEditMode) {
                Text(if (uiState.isEditing) "Save" else "Edit")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (profile != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(label = "Email:", value = profile.email)
                EditableInfoRow(
                    label = "First Name:",
                    value = profile.firstName,
                    isEditing = uiState.isEditing,
                    onValueChange = { viewModel.onProfileValueChange(firstName = it) }
                )
                EditableInfoRow(
                    label = "Last Name:",
                    value = profile.lastName,
                    isEditing = uiState.isEditing,
                    onValueChange = { viewModel.onProfileValueChange(lastName = it) }
                )
                EditableInfoRow(
                    label = "Age:",
                    value = profile.age.toString(),
                    isEditing = uiState.isEditing,
                    onValueChange = { viewModel.onProfileValueChange(age = it) },
                    keyboardType = KeyboardType.Number
                )
                EditableInfoRow(
                    label = "Target Weight:",
                    value = profile.targetWeight.toString(),
                    isEditing = uiState.isEditing,
                    onValueChange = { viewModel.onProfileValueChange(targetWeight = it) },
                    keyboardType = KeyboardType.Decimal
                )
            }
        } else {
            Text("Could not load user profile.", modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = seedData) { Text("Seed Sample Data") }
            Button(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) { Text("Reset All Wellness Data", color = MaterialTheme.colorScheme.onErrorContainer) }
            Button(
                onClick = onLogoutClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) { Text("Logout", color = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
    }
}

@Composable
fun EditableInfoRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
            )
        } else {
            Text(value, fontSize = 16.sp)
        }
    }
}

@Composable
fun PreferencesSettings(
    onBackClicked: () -> Unit,
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit
) {
    val themes = listOf("Light", "Dark")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Theme", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            themes.forEach { theme ->
                val selected = (theme == "Dark" && isDarkTheme) || (theme == "Light" && !isDarkTheme)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSetTheme(theme == "Dark") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSetTheme(theme == "Dark") }
                    )
                    Text(text = theme, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
        Text(text = value, fontSize = 16.sp)
    }
}

@Composable
fun AccessibilitySettings(
    onBackClicked: () -> Unit,
    useBiggerText: Boolean,
    onSetTextSize: (Boolean) -> Unit
) {
    val textSizes = listOf("Normal", "Bigger")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Accessibility", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Text Size", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            textSizes.forEach { size ->
                val selected = (size == "Bigger" && useBiggerText) || (size == "Normal" && !useBiggerText)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSetTextSize(size == "Bigger") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSetTextSize(size == "Bigger") }
                    )
                    Text(text = size, modifier = Modifier.padding(start = 8.dp))
                }
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
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Manage Questions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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

            allQuestions.forEach { (key, nameLabel) ->
                val isChecked = key in selectedQuestions
                val isEnabled = isChecked || selectedQuestions.size < 3

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEnabled) {
                            val newSelection = selectedQuestions.toMutableSet()
                            if (isChecked) {
                                newSelection.remove(key)
                            } else if (newSelection.size < 3) {
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
                                if (newSelection.size < 3) newSelection.add(key)
                            } else {
                                newSelection.remove(key)
                            }
                            onSetQuestions(newSelection)
                        },
                        enabled = isEnabled
                    )
                    Text(text = nameLabel, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
