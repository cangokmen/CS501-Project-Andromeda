package com.example.andromeda.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.data.RegisterRepository
import com.example.andromeda.data.UserProfile
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.viewmodels.RegisterViewModel
import com.example.andromeda.viewmodels.RegisterState
import kotlinx.coroutines.launch
import java.math.RoundingMode

// --- Main Settings Navigation ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit,
    useBiggerText: Boolean,
    onSetTextSize: (Boolean) -> Unit,
    selectedQuestions: Set<String>,
    onSetQuestions: (Set<String>) -> Unit,
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    var screenState by remember { mutableStateOf(ScreenState.Main) }
    val authState by registerViewModel.registerState.collectAsState()

    // Create repository instances here to pass down
    val context = LocalContext.current
    val wellnessDataRepository = remember { WellnessDataRepository(context) }
    val registerRepository = remember { RegisterRepository(context.applicationContext as Application) }
    val coroutineScope = rememberCoroutineScope()


    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screenState,
            label = "Settings Animation",
            transitionSpec = {
                // Define the slide animation
                if (targetState.ordinal > initialState.ordinal) {
                    // Navigating forward (e.g., Main -> Account)
                    slideInHorizontally { fullWidth -> fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> -fullWidth }
                } else {
                    // Navigating back (e.g., Account -> Main)
                    slideInHorizontally { fullWidth -> -fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> fullWidth }
                }
            }
        ) { state ->
            when (state) {
                ScreenState.Main -> SettingsMainScreen(
                    onAccountClick = { screenState = ScreenState.Account },
                    onQuestionManagementClick = { screenState = ScreenState.QuestionManagement },
                    onPreferencesClick = { screenState = ScreenState.Preferences },
                    onAccessibilityClick = { screenState = ScreenState.Accessibility },
                    onHelpCenterClick = { screenState = ScreenState.HelpCenter },
                    onPrivacyPolicyClick = { screenState = ScreenState.PrivacyPolicy }
                )
                ScreenState.Account -> {
                    val profile = (authState as? RegisterState.Authenticated)?.userProfile
                    if (profile != null) {
                        AccountSettings(
                            profile = profile,
                            onUpdateProfile = { fn, ln, age, w, unit ->
                                // This logic handles both profile updates and unit conversions
                                coroutineScope.launch {
                                    // First, perform data conversion if the unit has changed
                                    if (unit != profile.weightUnit) {
                                        wellnessDataRepository.convertAllWeightData(unit, registerRepository)
                                        // The conversion function already updates the profile,
                                        // but we need to refresh the local state.
                                        registerViewModel.checkUserStatus()
                                    } else {
                                        // If the unit is the same, just update the profile with the new values.
                                        // The conversion function handles rounding, so we can pass the Double directly.
                                        registerViewModel.createProfile(fn, ln, age.toIntOrNull() ?: 0, w.toDoubleOrNull() ?: 0.0, unit)
                                    }
                                }
                            },
                            onBackClicked = {
                                registerViewModel.checkUserStatus() // Refresh profile on exit
                                screenState = ScreenState.Main
                            },
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
                ScreenState.HelpCenter -> HelpCenterScreen (
                    onBackClicked = { screenState = ScreenState.Main }
                )
                ScreenState.PrivacyPolicy -> PrivacyPolicyScreen (
                    onBackClicked = { screenState = ScreenState.Main }
                )
            }
        }
    }
}


private enum class ScreenState {
    Main, Account, QuestionManagement, Preferences, Accessibility, HelpCenter, PrivacyPolicy
}

@Composable
private fun SettingsMainScreen(
    onAccountClick: () -> Unit,
    onQuestionManagementClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onHelpCenterClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val context = LocalContext.current
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
                        SettingsRow(title = "FAQ", onClick = onHelpCenterClick)
                        Divider()
                        SettingsRow(title = "Send Feedback", onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/JgnX7qSxA8MrMwjd9"))
                            context.startActivity(browserIntent)
                        })
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
                        SettingsRow(title = "Privacy Policy", onClick = onPrivacyPolicyClick)
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

@Composable
private fun HelpCenterScreen(onBackClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Frequently Asked Questions",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                FaqItem(
                    question = "How does watch sync work?",
                    answer = "Data you enter on your watch is sent to the phone. Your average weight is sent back to the watch to be used as the default for your next entry."
                )
            }
            item {
                FaqItem(
                    question = "How do I change my tracked questions?",
                    answer = "You can choose which three metrics to track by going to Settings > Question Management."
                )
            }
            item {
                FaqItem(
                    question = "How do I edit my previous entries?",
                    answer = "In the History screen, you can click the edit icon on the entry you want to edit."
                )
            }
            item {
                FaqItem(
                    question = "I accidentally entered an entry. Can I delete it?",
                    answer = "In the edit screen, you can click the delete icon to delete an entry."
                )
            }
            item {
                FaqItem(
                    question = "Which AI model is used?",
                    answer = "The gemini-2.5-flash-lite model is used for both suggestion generation and chatbot."
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicyScreen(onBackClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                PrivacyPolicyItem(
                    title = "Data Storage and HIPAA",
                    content = "Your privacy is our top priority. All personal and health-related data you enter into this application is stored locally on your device. This data is not transmitted to or stored on any external servers, ensuring it remains under your control and compliant with HIPAA privacy principles."
                )
            }
            item {
                PrivacyPolicyItem(
                    title = "AI Features and Data Usage",
                    content = "For AI-powered features like suggestion generation and the chatbot, we utilize Google Cloud's Vertex AI services. We have a signed Business Associate Agreement (BAA) with Google, which is a requirement for HIPAA compliance. This agreement legally ensures that any data processed by these AI models is handled securely, is not used to train Google's public models, and is protected under the strict standards of HIPAA."
                )
            }
            item {
                PrivacyPolicyItem(
                    title = "Data Deletion",
                    content = "You are in full control of your data. You can delete individual entries from the 'Edit' screen or clear your entire wellness history from the 'Account' settings. Logging out of your profile will permanently delete your profile and all associated data from the device."
                )
            }
            item {
                PrivacyPolicyItem(
                    title = "Feedback",
                    content = "When you choose to 'Send Feedback', you will be directed to a Google Form. The information you submit is used solely for the purpose of improving this application and is not linked to your personal data within the app."
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicyItem(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun FaqItem(question: String, answer: String) {
    Column {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// Detail Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    profile: UserProfile,
    onUpdateProfile: (firstName: String, lastName: String, age: String, targetWeight: String, weightUnit: String) -> Unit,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wellnessDataRepository = remember { WellnessDataRepository(context) }

    // Local state for editing form
    var isEditing by remember { mutableStateOf(false) }

    /* AI Suggested this: Using `remember` with `profile` and `isEditing` as keys ensures
     * that the form state resets automatically if the profile changes or the user
     * cancels an edit. This prevents stale data from persisting in the UI.
     */
    var firstName by remember(profile, isEditing) { mutableStateOf(profile.firstName) }
    var lastName by remember(profile, isEditing) { mutableStateOf(profile.lastName) }
    var age by remember(profile, isEditing) { mutableStateOf(profile.age.toString()) }
    var targetWeight by remember(profile, isEditing) { mutableStateOf(profile.targetWeight.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()) }

    // State for weight unit
    var selectedUnit by remember(profile, isEditing) { mutableStateOf(profile.weightUnit) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out and delete your profile? This will erase all your data.") },
            confirmButton = {
                Button(
                    onClick = {
                        onLogoutClicked()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Logout & Delete") }
            },
            dismissButton = {
                Button(onClick = { showLogoutDialog = false }) { Text("Cancel") }
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
                    onUpdateProfile(firstName, lastName, age, targetWeight, selectedUnit)
                }
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
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    label = { Text("Target Weight ($selectedUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // DISPLAY MODE UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow("First Name", profile.firstName)
                InfoRow("Last Name", profile.lastName)
                InfoRow("Age", profile.age.toString())
                InfoRow("Target Weight", "${profile.targetWeight.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} ${profile.weightUnit}")
                InfoRow("Unit", profile.weightUnit)
            }
        }

        Spacer(Modifier.weight(1f)) // Pushes buttons to the bottom

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("DEV: Reset Wellness History")
            }
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DEV: Logout & Delete Profile")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun PreferencesSettings(
    onBackClicked: () -> Unit,
    isDarkTheme: Boolean,
    onSetTheme: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode", fontSize = 18.sp)
            Switch(checked = isDarkTheme, onCheckedChange = onSetTheme)
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Accessibility", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Use Bigger Text", fontSize = 18.sp)
            Switch(checked = useBiggerText, onCheckedChange = onSetTextSize)
        }
    }
}


@Composable
fun QuestionManagementScreen(
    onBackClicked: () -> Unit,
    selectedQuestions: Set<String>,
    onSetQuestions: (Set<String>) -> Unit
) {
    val allQuestions = remember {
        listOf(
            "DIET" to "Diet",
            "ACTIVITY" to "Activity",
            "SLEEP" to "Sleep",
            "WATER" to "Water Intake",
            "PROTEIN" to "Protein Intake"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Manage Questions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Select up to 3 metrics you want to track daily.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        allQuestions.forEach { (key, title) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newSelection = selectedQuestions.toMutableSet()
                        if (key in newSelection) {
                            if (newSelection.size > 1) { // Prevent removing the last item
                                newSelection.remove(key)
                            }
                        } else {
                            if (newSelection.size < 3) {
                                newSelection.add(key)
                            }
                        }
                        onSetQuestions(newSelection)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = key in selectedQuestions,
                    onCheckedChange = null, // Handled by the row click
                    enabled = (key in selectedQuestions && selectedQuestions.size > 1) || (key !in selectedQuestions && selectedQuestions.size < 3)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
