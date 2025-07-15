package com.example.msgapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.msgapp.ui.theme.MsgAppTheme
import com.example.msgapp.ui.view.ChatScreen
import com.example.msgapp.ui.view.RoomSelector
import com.example.msgapp.ui.view.notifyNewMessage
import com.example.msgapp.viewmodel.MsgViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirebaseApp.initializeApp(this)
        setContent {
            MsgAppTheme {
                MsgAppRoot()
            }
        }
    }
}

// Um estado para representar o progresso da autenticação
sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val uid: String) : AuthState()
    object Error : AuthState()
}

@Composable
fun MsgAppRoot(vm: MsgViewModel = viewModel()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }

        // Este efeito corre uma vez para garantir que temos um utilizador autenticado
        LaunchedEffect(Unit) {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid
                        authState = if (uid != null) AuthState.Authenticated(uid) else AuthState.Error
                    } else {
                        authState = AuthState.Error
                    }
                }
            } else {
                authState = AuthState.Authenticated(auth.currentUser!!.uid)
            }
        }

        // O ecrã muda com base no estado da autenticação
        when (val state = authState) {
            is AuthState.Loading -> {
                // 1. Mostra um indicador de progresso enquanto espera pelo ID
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.Error -> {
                // 2. Mostra uma mensagem de erro se algo correr mal
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Falha na autenticação. Tente novamente.")
                }
            }
            is AuthState.Authenticated -> {
                // 3. Quando temos um ID, mostramos a aplicação
                val userId = state.uid // Agora temos a certeza que este ID é único
                ChatFlow(userId = userId, vm = vm)
            }
        }
    }
}

@Composable
fun ChatFlow(userId: String, vm: MsgViewModel) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("Usuário-${userId.takeLast(4)}") }
    var currentRoom by remember { mutableStateOf<String?>(null) }
    var lastNotifiedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoom) {
        currentRoom?.let { vm.switchRoom(it) }
    }

    if (currentRoom == null) {
        RoomSelector(onRoomSelected = { roomName ->
            if (roomName.isNotBlank()) {
                currentRoom = roomName
            }
        })
    } else {
        val roomName = currentRoom ?: "desconhecida"
        ChatScreen(
            username = userName,
            userId = userId,
            messages = vm.messages.collectAsState().value,
            onSend = { text -> vm.sendMessage(userId, userName, text) },
            currentRoom = roomName,
            lastNotifiedId = lastNotifiedId,
            onNotify = { msg ->
                notifyNewMessage(context, msg)
                lastNotifiedId = msg.id
            },
            onLeaveRoom = {
                currentRoom = null
            }
        )
    }
}