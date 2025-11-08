package com.example.andromeda.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.andromeda.ui.theme.AndromedaTheme

data class SettingItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)


enum class ScreenState {
    Main, Account, Preferences, Accessibility, QuestionManagement // New
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onSetTheme: (Boolean) -> Unit = {},
    useBiggerText: Boolean = false,
    onSetTextSize: (Boolean) -> Unit = {},
    selectedQuestions: Set<String> = setOf(), // Add this
    onSetQuestions: (Set<String>) -> Unit = {} // Add this
) {
    var screenState by remember { mutableStateOf(ScreenState.Main) }

    // FIX: Added targetState and transitionSpec to AnimatedContent
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
                                leadingContent = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                modifier = Modifier.clickable { item.onClick() }
                            )
                            if (index < settingsItems.size - 1) {
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
            ScreenState.Account -> AccountSettings(onBackClicked = { screenState = ScreenState.Main })
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f)) // Pushes the title to be more centered
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
                        .clickable {
                            onSetTheme(theme == "Dark")
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = {
                            onSetTheme(theme == "Dark")
                        }
                    )
                    Text(text = theme, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun AccountSettings(onBackClicked: () -> Unit) {
    // These values would typically be loaded from a ViewModel or repository.
    var name by remember { mutableStateOf("John") }
    var lastName by remember { mutableStateOf("Doe") }
    var age by remember { mutableStateOf("30") }
    var targetWeight by remember { mutableStateOf("180") }

    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with back button, title, and edit button
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Account Details", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { isEditing = !isEditing }) {
                Text(if (isEditing) "Save" else "Edit")
            }
        }

        Spacer(modifier = Modifier.height(32.dp)) // Add space below the top bar

        // Content area
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
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
                    label = { Text("Target Weight (lbs)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp) // Increased spacing
            ) {
                InfoRow(label = "Name:", value = name)
                InfoRow(label = "Last Name:", value = lastName)
                InfoRow(label = "Age:", value = age)
                InfoRow(label = "Target Weight:", value = targetWeight)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 20.sp) // Increased size
        Text(text = value, fontSize = 20.sp) // Increased size
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Accessibility", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AndromedaTheme(darkTheme = false) {
        Surface {
            SettingsScreen()
        }
    }
}
