package com.example.msgapp.viewmodel // Adicionar esta linha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.msgapp.model.Message // Importar a classe correta
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class MsgViewModel(application: Application) : AndroidViewModel(application) {

    private var currentRoom: String = "geral"
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var listener: ValueEventListener? = null

    private fun messagesRef(roomId: String) =
        FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("messages")

    fun switchRoom(roomId: String) {
        listener?.let { messagesRef(currentRoom).removeEventListener(it) }

        currentRoom = roomId

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = mutableListOf<Message>()
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null) msgs.add(msg)
                }
                _messages.value = msgs.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                // Idealmente, deverias tratar este erro (ex: log)
            }
        }

        messagesRef(currentRoom).orderByChild("timestamp").addValueEventListener(listener!!)
    }

    fun sendMessage(senderId: String, senderName: String, text: String) {
        val ref = messagesRef(currentRoom)
        val key = ref.push().key ?: UUID.randomUUID().toString()

        val msg = Message(
            id = key,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        ref.child(key).setValue(msg)
    }
}