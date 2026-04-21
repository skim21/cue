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
import com.ppailab.cue.api.ConversationScenario

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
    var mode by remember { mutableStateOf("partner") }
    val snackState = remember { SnackbarHostState() }
    var snack by remember { mutableStateOf("") }

    LaunchedEffect(personaId) { vm.loadPersona(personaId) }
    LaunchedEffect(snack) { if (snack.isNotEmpty()) { snackState.showSnackbar(snack); snack = "" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(persona?.name ?: "시나리오 예측", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("대화 흐름 예측", fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF7C3AED), titleContentColor = Color.White),
            )
        },
        snackbarHost = { SnackbarHost(snackState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            persona?.let {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)), shape = RoundedCornerShape(12.dp)) {
                    Text(it.persona, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = Color(0xFF5B21B6), lineHeight = 19.sp)
                }
            }

            // 모드 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("partner" to "상대방 먼저", "mine" to "내가 먼저").forEach { (value, label) ->
                    val selected = mode == value
                    Button(
                        onClick = { mode = value; message = ""; if (state is ReplyUiState.Success) vm.reset() },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF7C3AED) else Color(0xFFEDE9FE),
                            contentColor = if (selected) Color.White else Color(0xFF7C3AED),
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            OutlinedTextField(
                value = message,
                onValueChange = { message = it; if (state is ReplyUiState.Success) vm.reset() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (mode == "mine") "원하는 것" else "상대방 메시지") },
                placeholder = { Text(if (mode == "mine") "예: 용돈 올려줘" else "받은 메시지를 입력하세요") },
                minLines = 2, maxLines = 4,
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = { if (message.isNotBlank()) vm.generate(message, mode) },
                enabled = message.isNotBlank() && state !is ReplyUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state is ReplyUiState.Loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Text("시나리오 예측 중...", fontSize = 15.sp)
                    }
                } else {
                    Text("시나리오 예측", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            when (val s = state) {
                is ReplyUiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.scenarios) { scenario ->
                        ScenarioCard(
                            scenario = scenario,
                            partnerName = persona?.name ?: "상대방",
                            onCopy = {
                                val myLine = scenario.exchanges.firstOrNull { it.sender == "나" }?.message ?: ""
                                if (myLine.isNotBlank()) {
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                        .setPrimaryClip(ClipData.newPlainText("cue", myLine))
                                    snack = "복사됐어!"
                                }
                            },
                        )
                    }
                }
                is ReplyUiState.Error -> Text(s.message, color = Color(0xFFEF4444), fontSize = 14.sp)
                else -> {}
            }
        }
    }
}

@Composable
fun ScenarioCard(scenario: ConversationScenario, partnerName: String, onCopy: () -> Unit) {
    Card(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(20.dp)) {
                    Text("${scenario.probability}%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(scenario.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937), modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = Color(0xFFF3F4F6))
            scenario.exchanges.forEach { ex ->
                val isMe = ex.sender == "나"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                    if (!isMe) {
                        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(partnerName, fontSize = 10.sp, color = Color(0xFF9CA3AF))
                            Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp)) {
                                Text(ex.message, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 13.sp, color = Color(0xFF1F2937))
                            }
                        }
                    } else {
                        Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp)) {
                            Text(ex.message, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
            Text("탭 → 내 첫 답변 복사", fontSize = 11.sp, color = Color(0xFFD1D5DB), modifier = Modifier.align(Alignment.End))
        }
    }
}
