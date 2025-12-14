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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.andromeda.R
import com.example.andromeda.data.UserPreferencesRepository
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.ui.theme.AndromedaTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.math.RoundingMode

data class SettingItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

enum class ScreenState {
    Main, Account, Preferences, Accessibility, QuestionManagement
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onSetTheme: (Boolean) -> Unit = {},
    useBiggerText: Boolean = false,
    onSetTextSize: (Boolean) -> Unit = {},
    selectedQuestions: Set<String> = setOf(),
    onSetQuestions: (Set<String>) -> Unit = {},
    onLogout: () -> Unit = {}
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
            ScreenState.Main -> SettingsMainScreen(
                onAccountClick = { screenState = ScreenState.Account },
                onQuestionManagementClick = { screenState = ScreenState.QuestionManagement },
                onPreferencesClick = { screenState = ScreenState.Preferences },
                onAccessibilityClick = { screenState = ScreenState.Accessibility }
            )

            ScreenState.Account -> AccountSettings(
                onBackClicked = { screenState = ScreenState.Main },
                onLogoutClicked = onLogout
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

            ScreenState.QuestionManagement -> QuestionManagementScreen(
                onBackClicked = { screenState = ScreenState.Main },
                selectedQuestions = selectedQuestions,
                onSetQuestions = onSetQuestions
            )
        }
    }
}

/**
 * MAIN SETTINGS PAGE – “Profile” header, Help & Feedback, Settings card.
 * (Calendar removed)
 */
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

            // Help & Feedback
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

            // Settings section
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

/* ------------------------- EXISTING DETAIL SCREENS ------------------------- */

@Composable
fun AccountSettings(
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { WellnessDataRepository(context) }
    val userPrefs = remember { UserPreferencesRepository(context) }

    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val email = userPrefs.userEmail.first()
        val storedName = userPrefs.userName.first()
        if (storedName != null) name = storedName
        if (email != null) {
            val profile = userPrefs.getAccountProfile(email)
            lastName = profile.lastName
            age = profile.age
            targetWeight = profile.targetWeight
        }
    }

    val seedData: () -> Unit = {
        coroutineScope.launch {
            val email = userPrefs.userEmail.first() ?: return@launch

            val inputStream = context.resources.openRawResource(R.raw.sample_wellness_data)
            val reader = InputStreamReader(inputStream)
            val listType = object : TypeToken<List<WellnessData>>() {}.type
            val dataToSeed: List<WellnessData> = Gson().fromJson(reader, listType)

            dataToSeed.forEach { data ->
                val roundedWeight = data.weight
                    .toBigDecimal()
                    .setScale(1, RoundingMode.HALF_UP)
                    .toDouble()

                repository.addWellnessData(
                    data.copy(
                        weight = roundedWeight,
                        userEmail = email
                    )
                )
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
                        coroutineScope.launch { repository.clearAllData() }
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
                    coroutineScope.launch {
                        val email = userPrefs.userEmail.first()
                        if (email != null) {
                            userPrefs.saveAccountProfile(
                                email = email,
                                lastName = lastName,
                                age = age,
                                targetWeight = targetWeight
                            )
                        }
                    }
                }
                isEditing = !isEditing
            }) { Text(if (isEditing) "Save" else "Edit") }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            InfoRow(label = "Name:", value = name)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoRow(label = "Last Name:", value = lastName)
                InfoRow(label = "Age:", value = age)
                InfoRow(label = "Target Weight:", value = if (targetWeight.isNotBlank()) "$targetWeight lbs" else "")
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

        Button(
            onClick = onLogoutClicked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("LOG OUT", color = MaterialTheme.colorScheme.onPrimary)
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
            IconButton(onClick = onBackClicked) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
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
                    RadioButton(selected = selected, onClick = { onSetTheme(theme == "Dark") })
                    Text(text = theme, modifier = Modifier.padding(start = 8.dp))
                }
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
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = value, fontSize = 20.sp)
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
            IconButton(onClick = onBackClicked) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
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
                    RadioButton(selected = selected, onClick = { onSetTextSize(size == "Bigger") })
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
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

            allQuestions.forEach { (key, nameLabel) ->
                val isChecked = key in selectedQuestions
                val isEnabled = isChecked || selectedQuestions.size < 3

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEnabled) {
                            val newSelection = selectedQuestions.toMutableSet()
                            if (isChecked) newSelection.remove(key) else newSelection.add(key)
                            onSetQuestions(newSelection)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            val newSelection = selectedQuestions.toMutableSet()
                            if (it) newSelection.add(key) else newSelection.remove(key)
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AndromedaTheme(darkTheme = false) {
        Surface { SettingsScreen() }
    }
}