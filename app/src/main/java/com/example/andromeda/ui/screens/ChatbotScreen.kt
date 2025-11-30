package com.example.andromeda.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.viewmodels.ChatMessage
import com.example.andromeda.viewmodels.ChatbotViewModel

@Composable
fun ChatbotScreen(
    viewModel: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to the bottom when a new message appears
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        bottomBar = {
            UserInputBar(
                userInput = uiState.userInput,
                onUserInputChange = viewModel::onUserInputChange,
                onSendMessage = viewModel::sendMessage
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages) { message ->
                MessageBubble(message = message)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f) // Max width for a bubble
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isFromUser) 0.dp else 16.dp
                    )
                )
                // --- START: MODIFICATION FOR GREEN CHAT BUBBLES ---
                .background(
                    if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .padding(16.dp)
        ) {
            if (message.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = message.text,
                    // Use the correct "on" color to ensure text is readable
                    color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            // --- END: MODIFICATION FOR GREEN CHAT BUBBLES ---
        }
    }
}

@Composable
fun UserInputBar(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            shape = RoundedCornerShape(24.dp)
        )
        IconButton(onClick = onSendMessage, enabled = userInput.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message",
                // Make the send icon green to match
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
