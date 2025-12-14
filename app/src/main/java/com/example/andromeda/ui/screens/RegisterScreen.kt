package com.example.andromeda.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.viewmodels.AuthViewModel
import com.example.andromeda.viewmodels.AuthState
import androidx.compose.ui.platform.LocalContext
import android.app.Application

@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    // This is the line to change
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
){
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    val registrationState by authViewModel.authState.collectAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            // Use the correct, locally defined AuthState types
            is AuthState.Authenticated -> {
                isLoading = false
                onRegistrationSuccess()
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = state.message
            }
            is AuthState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            else -> {
                isLoading = false
            }
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
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text("Target Weight (kg)") },
                // Corrected KeyboardType from NumberDecimal to Decimal
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
                            authViewModel.createProfile(firstName, lastName, ageInt, weightDouble)
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
