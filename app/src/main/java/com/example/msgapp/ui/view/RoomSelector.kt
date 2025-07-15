package com.example.msgapp.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoomSelector(onRoomSelected: (String) -> Unit) {
    var roomName by remember { mutableStateOf("") }
    Surface(
        shadowElevation = 6.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                placeholder = { Text("Nome da sala") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { if (roomName.isNotBlank()) onRoomSelected(roomName) },
                shape = MaterialTheme.shapes.medium,
                enabled = roomName.isNotBlank()
            ) {
                Text("Entrar")
            }
        }
    }
}
