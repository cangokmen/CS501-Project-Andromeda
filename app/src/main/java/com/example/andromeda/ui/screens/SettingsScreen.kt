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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
    Main, Account, Preferences
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = isSystemInDarkTheme(), // Add isDarkTheme parameter
    onSetTheme: (Boolean) -> Unit = {}
) {
    var screenState by remember { mutableStateOf(ScreenState.Main) }

    AnimatedContent(
        targetState = screenState,
        label = "Settings Animation",
        transitionSpec = {
            if (targetState > initialState) {
                // Enter from right, exit to left
                slideInHorizontally { fullWidth -> fullWidth } + fadeIn() togetherWith
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()
            } else {
                // Enter from left, exit to right
                slideInHorizontally { fullWidth -> -fullWidth } + fadeIn() togetherWith
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
            }
        }
    ) { targetState ->
        when (targetState) {
            ScreenState.Main -> {
                val settingsItems = listOf(
                    SettingItem("Account", Icons.Default.AccountCircle) { screenState = ScreenState.Account },
                    SettingItem("Question Management", Icons.Default.Build) { /* Handle click */ },
                    SettingItem("Preferences", Icons.Default.Face) { screenState = ScreenState.Preferences },
                    SettingItem("Accessibility", Icons.Default.Info) { /* Handle click */ }
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
                isDarkTheme = isDarkTheme, // Pass it down
                onSetTheme = onSetTheme
            )
        }
    }
}

@Composable
fun PreferencesSettings(
    onBackClicked: () -> Unit,
    isDarkTheme: Boolean, // Receive the actual theme state
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
                // The selected state is now derived from the isDarkTheme parameter
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
                    label = { Text("Target Weight") },
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AndromedaTheme {
        Surface {
            SettingsScreen()
        }
    }
}
