package com.ppailab.cue.reply

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ppailab.cue.api.ConversationScenario

// ── 디자인 토큰 ────────────────────────────────────────────────────────────────
private val Blue       = Color(0xFF246BFD)
private val BlueLight  = Color(0xFFEBF1FF)
private val PageBg     = Color(0xFFF5F6F8)
private val FieldBg    = Color(0xFFEEEFF3)
private val TextMain   = Color(0xFF171719)
private val TextSub    = Color(0xFF8D909A)
private val TextFaint  = Color(0xFFBCC0CC)
private val BubblePeer = Color(0xFFEEEFF3)
private val Divider    = Color(0xFFF0F1F5)
private val SegBg      = Color(0xFFE8E9EE)

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
        containerColor = PageBg,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                persona?.name ?: "시나리오 예측",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextMain,
                            )
                            Text("대화 흐름 예측", fontSize = 12.sp, color = TextSub)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextMain)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                )
                HorizontalDivider(color = Divider, thickness = 1.dp)
            }
        },
        snackbarHost = { SnackbarHost(snackState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 페르소나 요약 (테두리 없이 배경 tint만)
            persona?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BlueLight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("🧠", fontSize = 13.sp)
                    Text(it.persona, fontSize = 13.sp, color = Blue, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                }
            }

            // 세그먼트 모드 토글 (pill 스타일)
            ModeToggle(mode = mode) { selected ->
                mode = selected
                message = ""
                if (state is ReplyUiState.Success) vm.reset()
            }

            // 입력 필드 (filled, 테두리 없음)
            TextField(
                value = message,
                onValueChange = { message = it; if (state is ReplyUiState.Success) vm.reset() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (mode == "mine") "원하는 것" else "상대방 메시지", fontSize = 13.sp) },
                placeholder = { Text(if (mode == "mine") "예: 용돈 올려줘" else "받은 메시지를 입력하세요", color = TextFaint) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldBg,
                    unfocusedContainerColor = FieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    focusedLabelColor = Blue,
                    unfocusedLabelColor = TextSub,
                    cursorColor = Blue,
                ),
            )

            // 예측 버튼
            Button(
                onClick = { if (message.isNotBlank()) vm.generate(message, mode) },
                enabled = message.isNotBlank() && state !is ReplyUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue,
                    disabledContainerColor = FieldBg,
                    disabledContentColor = TextSub,
                ),
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                if (state is ReplyUiState.Loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                        Text("예측 중...", fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
                is ReplyUiState.Error -> Text(s.message, color = Color(0xFFE03C3C), fontSize = 14.sp)
                else -> {}
            }
        }
    }
}

// ── 세그먼트 컨트롤 ────────────────────────────────────────────────────────────
@Composable
private fun ModeToggle(mode: String, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SegBg, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        Row {
            listOf("partner" to "상대방 먼저", "mine" to "내가 먼저").forEach { (value, label) ->
                val selected = mode == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { onSelect(value) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) TextMain else TextSub,
                    )
                }
            }
        }
    }
}

// ── 시나리오 카드 ──────────────────────────────────────────────────────────────
@Composable
fun ScenarioCard(scenario: ConversationScenario, partnerName: String, onCopy: () -> Unit) {
    Card(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = BlueLight, shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "${scenario.probability}%",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue,
                    )
                }
                Text(
                    scenario.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = Divider)

            // 대화 버블
            scenario.exchanges.forEach { ex ->
                val isMe = ex.sender == "나"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                ) {
                    if (!isMe) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.widthIn(max = 260.dp),
                        ) {
                            Text(partnerName, fontSize = 11.sp, color = TextFaint, fontWeight = FontWeight.Medium)
                            Surface(color = BubblePeer, shape = RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)) {
                                Text(
                                    ex.message,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    color = TextMain,
                                    lineHeight = 20.sp,
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Blue,
                            shape = RoundedCornerShape(14.dp, 4.dp, 14.dp, 14.dp),
                            modifier = Modifier.widthIn(max = 260.dp),
                        ) {
                            Text(
                                ex.message,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
            }

            Text("탭하면 내 첫 답변 복사", fontSize = 11.sp, color = TextFaint, modifier = Modifier.align(Alignment.End))
        }
    }
}
