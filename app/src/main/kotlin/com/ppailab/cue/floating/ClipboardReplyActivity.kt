package com.ppailab.cue.floating

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppailab.cue.api.ConversationScenario
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.persona.PersonaStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ── 디자인 토큰 ────────────────────────────────────────────────────────────────
private val Blue       = Color(0xFF246BFD)
private val BlueLight  = Color(0xFFEBF1FF)
private val FieldBg    = Color(0xFFEEEFF3)
private val TextMain   = Color(0xFF171719)
private val TextSub    = Color(0xFF8D909A)
private val TextFaint  = Color(0xFFBCC0CC)
private val BubblePeer = Color(0xFFEEEFF3)
private val Divider    = Color(0xFFF0F1F5)
private val SegBg      = Color(0xFFE8E9EE)

@AndroidEntryPoint
class ClipboardReplyActivity : ComponentActivity() {

    @Inject lateinit var repo: PeopleSimRepository
    @Inject lateinit var store: PersonaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clipboard = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
        setContent { MaterialTheme { ScenarioSheet(clipboard) { finish() } } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ScenarioSheet(clipboardText: String, onDismiss: () -> Unit) {
        val personas = remember { store.loadAll() }
        var selectedIdx by remember { mutableIntStateOf(0) }
        var mode by remember { mutableStateOf("partner") }
        var inputText by remember { mutableStateOf("") }
        var generateTick by remember { mutableIntStateOf(0) }
        var scenarios by remember { mutableStateOf<List<ConversationScenario>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        val snackState = remember { SnackbarHostState() }
        var snack by remember { mutableStateOf("") }

        LaunchedEffect(snack) {
            if (snack.isNotEmpty()) { snackState.showSnackbar(snack); snack = "" }
        }

        // partner 모드: 클립보드/페르소나 변경 시 자동 실행
        LaunchedEffect(clipboardText, selectedIdx) {
            if (mode != "partner") return@LaunchedEffect
            if (clipboardText.isBlank()) return@LaunchedEffect
            val p = personas.getOrNull(selectedIdx)
            loading = true; error = ""; scenarios = emptyList()
            repo.generateScenarios(clipboardText, p?.persona ?: "", p?.name ?: "상대방", "partner")
                .onSuccess { scenarios = it; loading = false }
                .onFailure { error = it.message ?: "오류"; loading = false }
        }

        // mine 모드: 예측 버튼 클릭 시 실행
        LaunchedEffect(generateTick) {
            if (generateTick == 0) return@LaunchedEffect
            if (inputText.isBlank()) return@LaunchedEffect
            val p = personas.getOrNull(selectedIdx)
            loading = true; error = ""; scenarios = emptyList()
            repo.generateScenarios(inputText, p?.persona ?: "", p?.name ?: "상대방", "mine")
                .onSuccess { scenarios = it; loading = false }
                .onFailure { error = it.message ?: "오류"; loading = false }
        }

        Scaffold(
            containerColor = Color.White,
            snackbarHost = { SnackbarHost(snackState) },
        ) { _ ->
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 헤더
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "대화 시나리오",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextMain,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismiss) {
                            Text("닫기", fontSize = 14.sp, color = TextSub)
                        }
                    }

                    // 세그먼트 모드 토글
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
                                        .clickable {
                                            mode = value
                                            inputText = ""
                                            scenarios = emptyList()
                                            error = ""
                                        }
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

                    if (mode == "partner") {
                        if (clipboardText.isBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF8E7), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("💡", fontSize = 14.sp)
                                Text("카톡 메시지를 먼저 복사하고 다시 탭해주세요", fontSize = 13.sp, color = Color(0xFF7A5200))
                            }
                            return@ModalBottomSheet
                        }
                        // 클립보드 미리보기 (tint 배경, 테두리 없음)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BlueLight, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text("💬", fontSize = 13.sp)
                            Text(
                                clipboardText,
                                fontSize = 13.sp,
                                color = Blue,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        // 목표 입력 필드 (filled 스타일)
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it; scenarios = emptyList(); error = "" },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("원하는 것", fontSize = 13.sp) },
                            placeholder = { Text("예: 용돈 올려줘", color = TextFaint) },
                            minLines = 2,
                            maxLines = 3,
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
                        if (inputText.isNotBlank()) {
                            Button(
                                onClick = { generateTick++ },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue,
                                    disabledContainerColor = FieldBg,
                                    disabledContentColor = TextSub,
                                ),
                                shape = RoundedCornerShape(50),
                                elevation = ButtonDefaults.buttonElevation(0.dp),
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("시나리오 예측", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // 페르소나 선택
                    if (personas.size > 1) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            TextField(
                                value = personas[selectedIdx].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("상대방", fontSize = 13.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = FieldBg,
                                    unfocusedContainerColor = FieldBg,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextMain,
                                    unfocusedTextColor = TextMain,
                                ),
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                personas.forEachIndexed { i, p ->
                                    DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedIdx = i; expanded = false })
                                }
                            }
                        }
                    } else if (personas.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FieldBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("👤", fontSize = 13.sp)
                            Text(personas[0].name, fontSize = 14.sp, color = TextMain, fontWeight = FontWeight.Medium)
                        }
                    }

                    // 결과 영역
                    when {
                        loading -> Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(26.dp), color = Blue, strokeWidth = 2.5.dp)
                                Text("시나리오 예측 중...", fontSize = 13.sp, color = TextSub)
                            }
                        }
                        error.isNotEmpty() -> Text(error, color = Color(0xFFE03C3C), fontSize = 13.sp)
                        scenarios.isNotEmpty() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(scenarios) { s ->
                                SheetScenarioCard(s, personas.getOrNull(selectedIdx)?.name ?: "상대방") {
                                    val myLine = s.exchanges.firstOrNull { it.sender == "나" }?.message ?: ""
                                    if (myLine.isNotBlank()) {
                                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(android.content.ClipData.newPlainText("cue", myLine))
                                        snack = "복사됐어! 카톡에 붙여넣어봐"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetScenarioCard(scenario: ConversationScenario, partnerName: String, onCopy: () -> Unit) {
    Card(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            modifier = Modifier.widthIn(max = 240.dp),
                        ) {
                            Text(partnerName, fontSize = 11.sp, color = TextFaint, fontWeight = FontWeight.Medium)
                            Surface(color = Color.White, shape = RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)) {
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
                            modifier = Modifier.widthIn(max = 240.dp),
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
