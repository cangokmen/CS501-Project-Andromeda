package com.example.andromeda.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.viewmodels.RegisterViewModel
import com.example.andromeda.viewmodels.RegisterState
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.example.andromeda.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }

    // --- NEW STATE ---
    var selectedUnit by remember { mutableStateOf("kg") }
    val unitOptions = listOf("kg", "lbs")
    // --- END NEW STATE ---

    val registrationState by registerViewModel.registerState.collectAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegisterState.Authenticated -> {
                isLoading = false
                onRegistrationSuccess()
            }
            is RegisterState.Error -> {
                isLoading = false
                errorMessage = state.message
            }
            is RegisterState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            else -> isLoading = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO ADDED HERE ---
            Icon(
                painter = painterResource(id = R.drawable.galaxy),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(alpha = 0.8f),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            // --- END LOGO ---

            Text("Create Your Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- NEW UNIT SELECTOR ---
            Text("Units for Weight", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                unitOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = unitOptions.size),
                        onClick = { selectedUnit = label },
                        selected = label == selectedUnit
                    ) {
                        Text(label)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // --- END NEW UNIT SELECTOR ---


            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text("Target Weight ($selectedUnit)") }, // Dynamic label
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val ageInt = age.toIntOrNull()
                        val weightDouble = targetWeight.toDoubleOrNull()
                        if (firstName.isNotBlank() && lastName.isNotBlank() && ageInt != null && weightDouble != null) {
                            registerViewModel.createProfile(firstName, lastName, ageInt, weightDouble, selectedUnit) // Pass unit
                        } else {
                            errorMessage = "Please fill all fields correctly."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Profile")
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
