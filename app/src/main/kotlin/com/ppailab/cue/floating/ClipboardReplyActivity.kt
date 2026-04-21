package com.ppailab.cue.floating

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.ppailab.cue.api.ConversationScenario
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.persona.PersonaStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

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

        // partner 모드: clipboardText/selectedIdx 변경 시 자동 실행
        LaunchedEffect(clipboardText, selectedIdx) {
            if (mode != "partner") return@LaunchedEffect
            if (clipboardText.isBlank()) return@LaunchedEffect
            val p = personas.getOrNull(selectedIdx)
            loading = true; error = ""; scenarios = emptyList()
            repo.generateScenarios(clipboardText, p?.persona ?: "", p?.name ?: "상대방", "partner")
                .onSuccess { scenarios = it; loading = false }
                .onFailure { error = it.message ?: "오류"; loading = false }
        }

        // mine 모드: generateTick 변경 시 실행
        LaunchedEffect(generateTick) {
            if (generateTick == 0) return@LaunchedEffect
            if (inputText.isBlank()) return@LaunchedEffect
            val p = personas.getOrNull(selectedIdx)
            loading = true; error = ""; scenarios = emptyList()
            repo.generateScenarios(inputText, p?.persona ?: "", p?.name ?: "상대방", "mine")
                .onSuccess { scenarios = it; loading = false }
                .onFailure { error = it.message ?: "오류"; loading = false }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackState) }) { _ ->
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFFF9FAFB),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ 대화 시나리오", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("닫기", color = Color(0xFF9CA3AF)) }
                    }

                    // 모드 토글
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("partner" to "상대방 먼저", "mine" to "내가 먼저").forEach { (value, label) ->
                            val selected = mode == value
                            Button(
                                onClick = { mode = value; inputText = ""; scenarios = emptyList(); error = "" },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFF7C3AED) else Color(0xFFEDE9FE),
                                    contentColor = if (selected) Color.White else Color(0xFF7C3AED),
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }

                    if (mode == "partner") {
                        if (clipboardText.isBlank()) {
                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("카톡 메시지를 먼저 복사하고 다시 탭해주세요", modifier = Modifier.padding(12.dp), fontSize = 14.sp, color = Color(0xFF92400E))
                            }
                            return@ModalBottomSheet
                        }
                        // 클립보드 미리보기
                        Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(clipboardText, modifier = Modifier.padding(10.dp), fontSize = 13.sp, color = Color(0xFF5B21B6), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it; scenarios = emptyList(); error = "" },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("원하는 것") },
                            placeholder = { Text("예: 용돈 올려줘") },
                            minLines = 2, maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                        )
                        if (inputText.isNotBlank()) {
                            Button(
                                onClick = { generateTick++ },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("시나리오 예측", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // 페르소나 선택
                    if (personas.size > 1) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = personas[selectedIdx].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("상대방") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                personas.forEachIndexed { i, p ->
                                    DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedIdx = i; expanded = false })
                                }
                            }
                        }
                    } else if (personas.isNotEmpty()) {
                        Text("👤 ${personas[0].name}", fontSize = 13.sp, color = Color(0xFF5B21B6), fontWeight = FontWeight.Medium)
                    }

                    when {
                        loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF7C3AED), strokeWidth = 2.dp)
                                Text("시나리오 예측 중...", fontSize = 13.sp, color = Color(0xFF6B7280))
                            }
                        }
                        error.isNotEmpty() -> Text(error, color = Color(0xFFEF4444), fontSize = 13.sp)
                        scenarios.isNotEmpty() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(scenarios) { s ->
                                ScenarioCard(s, personas.getOrNull(selectedIdx)?.name ?: "상대방") {
                                    // 내 첫 대사 복사
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
private fun ScenarioCard(scenario: ConversationScenario, partnerName: String, onCopy: () -> Unit) {
    Card(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 헤더: 확률 + 제목
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(20.dp)) {
                    Text("${scenario.probability}%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(scenario.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937), modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = Color(0xFFF3F4F6))

            // 대화 버블들
            scenario.exchanges.forEach { ex ->
                val isMe = ex.sender == "나"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                ) {
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

            Text("탭하면 내 첫 답변 복사", fontSize = 11.sp, color = Color(0xFFD1D5DB), modifier = Modifier.align(Alignment.End))
        }
    }
}
