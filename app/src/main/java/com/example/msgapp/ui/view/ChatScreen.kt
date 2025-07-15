package com.example.msgapp.ui.view

import android.Manifest
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.msgapp.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    username: String,
    userId: String,
    messages: List<Message>,
    onSend: (String) -> Unit,
    currentRoom: String,
    lastNotifiedId: String?,
    onNotify: (Message) -> Unit,
    onLeaveRoom: (() -> Unit)? = null
) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // --- LÓGICA PARA PEDIR PERMISSÃO ---
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    // Pedimos a permissão assim que o ecrã é aberto, se necessário
    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(messages.size) {
        val lastMsg = messages.lastOrNull()
        // só notifica se tiver permissão
        if (hasNotificationPermission && lastMsg != null && lastMsg.senderId != userId && lastMsg.id != lastNotifiedId) {
            onNotify(lastMsg)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sala: $currentRoom", fontWeight = FontWeight.Bold) },
                actions = {
                    if (onLeaveRoom != null) {
                        TextButton(onClick = onLeaveRoom) {
                            Text("Trocar sala")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mensagem") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )
                Spacer(Modifier.width(8.dp))
                val isInputEmpty = input.isBlank()
                IconButton(
                    onClick = {
                        if (!isInputEmpty) {
                            onSend(input)
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isInputEmpty) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isInputEmpty) "Gravar" else "Enviar",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(
                    msg = msg,
                    isOwn = msg.senderId == userId
                )
            }
        }
    }
}


@Composable
fun MessageBubble(msg: Message, isOwn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwn) 48.dp else 8.dp,
                end = if (isOwn) 8.dp else 48.dp,
                top = 2.dp, bottom = 2.dp
            ),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOwn) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg.senderName.firstOrNull()?.uppercase() ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(2.dp))
        }
        Surface(
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = if (isOwn) 20.dp else 4.dp,
                topEnd = if (isOwn) 4.dp else 20.dp,
                bottomEnd = 20.dp,
                bottomStart = 20.dp
            ),
            shadowElevation = 1.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = android.text.format.DateFormat.format("HH:mm", msg.timestamp).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
        if (isOwn) {
            Spacer(Modifier.width(2.dp))
        }
    }
}

fun notifyNewMessage(context: Context, message: Message) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Se não tiver permissão, simplesmente não faz nada.
            return
        }
    }

    val channelId = "chat_messages"
    val notificationManager = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "Mensagens", android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Nova mensagem de ${message.senderName}")
        .setContentText(message.text)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    notificationManager.notify(message.id.hashCode(), notification)
}