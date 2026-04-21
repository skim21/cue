package com.ppailab.cue.reply

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ppailab.cue.api.ReplyCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyScreen(
    personaId: String,
    onBack: () -> Unit,
    vm: ReplyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val persona by vm.persona.collectAsState()
    var message by remember { mutableStateOf("") }
    var snackMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(personaId) { vm.loadPersona(personaId) }
    LaunchedEffect(snackMessage) {
        if (snackMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMessage)
            snackMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(persona?.name ?: "답장 생성", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (persona != null) {
                            Text("맞춤 답장", fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF7C3AED),
                    titleContentColor = Color.White,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            persona?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        it.persona,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = Color(0xFF5B21B6),
                        lineHeight = 19.sp,
                    )
                }
            }

            OutlinedTextField(
                value = message,
                onValueChange = { message = it; if (state is ReplyUiState.Success) vm.reset() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("상대방 메시지") },
                placeholder = { Text("여기에 받은 메시지를 입력하세요") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = { if (message.isNotBlank()) vm.generate(message) },
                enabled = message.isNotBlank() && state !is ReplyUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state is ReplyUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("답장 생성", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            when (val s = state) {
                is ReplyUiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(s.replies) { reply ->
                            ReplyCard(reply) {
                                copyToClipboard(context, reply.text)
                                snackMessage = "복사됐어!"
                            }
                        }
                    }
                }
                is ReplyUiState.Error -> {
                    Text(s.message, color = Color(0xFFEF4444), fontSize = 14.sp)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ReplyCard(reply: ReplyCandidate, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onCopy,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = Color(0xFFEDE9FE),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    reply.style,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                )
            }
            Text(
                reply.text,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = Color(0xFF1F2937),
                lineHeight = 22.sp,
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("cue_reply", text))
}
