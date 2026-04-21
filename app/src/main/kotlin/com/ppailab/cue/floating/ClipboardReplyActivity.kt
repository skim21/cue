package com.ppailab.cue.floating

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.api.ReplyCandidate
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

        setContent {
            MaterialTheme {
                ReplySheet(
                    clipboardText = clipboard,
                    onDismiss = { finish() },
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ReplySheet(clipboardText: String, onDismiss: () -> Unit) {
        val personas = remember { store.loadAll() }
        var selectedPersonaIdx by remember { mutableIntStateOf(0) }
        var replies by remember { mutableStateOf<List<ReplyCandidate>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        var snack by remember { mutableStateOf("") }
        val snackState = remember { SnackbarHostState() }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(snack) {
            if (snack.isNotEmpty()) { snackState.showSnackbar(snack); snack = "" }
        }

        // 클립보드 있으면 자동 생성
        LaunchedEffect(clipboardText, selectedPersonaIdx) {
            if (clipboardText.isBlank()) return@LaunchedEffect
            generate(clipboardText, selectedPersonaIdx, personas.map { it.persona }) { r, e ->
                replies = r; error = e; loading = false
            }.also { loading = true }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackState) }) { _ ->
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 헤더
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ Cue 답장", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("닫기", color = Color(0xFF9CA3AF)) }
                    }

                    // 클립보드 미리보기
                    if (clipboardText.isBlank()) {
                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("카톡 메시지를 먼저 복사하고 다시 탭해주세요", modifier = Modifier.padding(12.dp), fontSize = 14.sp, color = Color(0xFF92400E))
                        }
                        return@ModalBottomSheet
                    }

                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(clipboardText, modifier = Modifier.padding(10.dp), fontSize = 13.sp, color = Color(0xFF374151), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    // 페르소나 선택 (여러 명 있을 때만)
                    if (personas.size > 1) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = personas[selectedPersonaIdx].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("상대방") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                personas.forEachIndexed { i, p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = { selectedPersonaIdx = i; expanded = false },
                                    )
                                }
                            }
                        }
                    } else if (personas.isNotEmpty()) {
                        Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "👤 ${personas[0].name}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 13.sp, color = Color(0xFF5B21B6), fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // 결과
                    when {
                        loading -> {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF7C3AED), strokeWidth = 2.dp)
                                    Text("답장 생성 중...", fontSize = 14.sp, color = Color(0xFF6B7280))
                                }
                            }
                        }
                        error.isNotEmpty() -> Text(error, color = Color(0xFFEF4444), fontSize = 13.sp)
                        replies.isNotEmpty() -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(replies) { reply ->
                                    ReplyCard(reply) {
                                        copyText(reply.text)
                                        snack = "복사됐어!"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generate(
        message: String,
        personaIdx: Int,
        personas: List<String>,
        onResult: (List<ReplyCandidate>, String) -> Unit,
    ) {
        lifecycleScope.launch {
            val persona = personas.getOrNull(personaIdx) ?: ""
            repo.generateReplies(message, persona)
                .onSuccess { onResult(it, "") }
                .onFailure { onResult(emptyList(), it.message ?: "오류") }
        }
    }

    private fun copyText(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("cue", text))
    }
}

@Composable
private fun ReplyCard(reply: ReplyCandidate, onCopy: () -> Unit) {
    Card(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(6.dp)) {
                Text(reply.style, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
            }
            Text(reply.text, modifier = Modifier.weight(1f), fontSize = 15.sp, color = Color(0xFF1F2937), lineHeight = 22.sp)
        }
    }
}
