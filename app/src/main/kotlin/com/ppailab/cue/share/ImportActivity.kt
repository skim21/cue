package com.ppailab.cue.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.ppailab.cue.MainActivity
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.parser.KakaoParsed
import com.ppailab.cue.parser.KakaoParser
import com.ppailab.cue.persona.PersonaStore
import com.ppailab.cue.persona.SavedPersona
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val MAX_BYTES = 1_048_576 // 1MB

@AndroidEntryPoint
class ImportActivity : ComponentActivity() {

    @Inject lateinit var repo: PeopleSimRepository
    @Inject lateinit var store: PersonaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent?.clipData?.getItemAt(0)?.uri
            ?: intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            ?: run { finish(); return }

        setContent { MaterialTheme { ImportScreen(uri) } }
    }

    @Composable
    private fun ImportScreen(uri: Uri) {
        var uiState by remember { mutableStateOf<ImportState>(ImportState.Loading("파일 읽는 중...")) }

        LaunchedEffect(uri) { uiState = readFile(uri) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val s = uiState) {
                is ImportState.Loading   -> LoadingView(s.message)
                is ImportState.PickName  -> PickNameView(s) { name ->
                    uiState = ImportState.Loading("$name 분석 중...")
                    lifecycleScope.launch { uiState = analyze(s.parsed, name) }
                }
                is ImportState.Success   -> SuccessView(s) { openMainAndFinish() }
                is ImportState.Error     -> ErrorView(s.message) { finish() }
            }
        }
    }

    private suspend fun readFile(uri: Uri): ImportState = withContext(Dispatchers.IO) {
        try {
            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext ImportState.Error("파일을 열 수 없어요")

            // 1MB만 읽기
            val bytes = stream.use { it.readBytes() }
            val truncated = if (bytes.size > MAX_BYTES) bytes.copyOf(MAX_BYTES) else bytes
            val text = String(truncated, Charsets.UTF_8)

            if (text.isBlank()) return@withContext ImportState.Error("파일이 비어있어요")

            val parsed = KakaoParser.preparse(text)
            ImportState.PickName(parsed, truncated.size < bytes.size)
        } catch (e: Exception) {
            ImportState.Error("읽기 실패: ${e.message}")
        }
    }

    private suspend fun analyze(parsed: KakaoParsed, name: String): ImportState {
        return try {
            val conv = KakaoParser.extract(parsed, name)
            val result = repo.analyzePersona(name, conv.partnerMessages)
            result.fold(
                onSuccess = { personaText ->
                    val saved = SavedPersona(name = name, persona = personaText, messageCount = conv.totalMessages)
                    store.save(saved)
                    ImportState.Success(saved)
                },
                onFailure = { ImportState.Error("분석 실패: ${it.message}") }
            )
        } catch (e: Exception) {
            ImportState.Error("오류: ${e.message}")
        }
    }

    private fun openMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}

// ── States ────────────────────────────────────────────────────────────────────

sealed class ImportState {
    data class Loading(val message: String) : ImportState()
    data class PickName(val parsed: KakaoParsed, val wasTruncated: Boolean) : ImportState()
    data class Success(val persona: SavedPersona) : ImportState()
    data class Error(val message: String) : ImportState()
}

// ── Views ─────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingView(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Color(0xFF7C3AED), modifier = Modifier.size(48.dp))
        Text(message, fontSize = 16.sp, color = Color(0xFF6B7280))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickNameView(state: ImportState.PickName, onSelect: (String) -> Unit) {
    var customName by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(state.parsed.speakers.isEmpty()) }

    Column(
        modifier = Modifier.padding(28.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💬", fontSize = 48.sp)
        Text("누구를 분석할까요?", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        if (state.wasTruncated) {
            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp)) {
                Text(
                    "파일이 커서 앞부분 1MB만 분석해요",
                    modifier = Modifier.padding(10.dp),
                    fontSize = 12.sp, color = Color(0xFF92400E),
                )
            }
        }

        if (state.parsed.speakers.isNotEmpty() && !showCustomInput) {
            Text("등장인물을 선택하세요", fontSize = 14.sp, color = Color(0xFF6B7280))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.parsed.speakers) { name ->
                    OutlinedCard(
                        onClick = { onSelect(name) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                color = Color(0xFFEDE9FE),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    name.take(1),
                                    modifier = Modifier.padding(8.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED),
                                )
                            }
                            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            TextButton(onClick = { showCustomInput = true }) {
                Text("직접 입력하기", fontSize = 13.sp, color = Color(0xFF9CA3AF))
            }
        } else {
            // 등장인물 없거나 직접 입력 선택
            if (state.parsed.speakers.isNotEmpty()) {
                TextButton(onClick = { showCustomInput = false }) {
                    Text("← 목록으로", fontSize = 13.sp, color = Color(0xFF7C3AED))
                }
            } else {
                Text("이름을 직접 입력해주세요", fontSize = 14.sp, color = Color(0xFF6B7280))
            }

            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text("상대방 이름") },
                placeholder = { Text("예: 엄마, 홍길동") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Button(
                onClick = { if (customName.isNotBlank()) onSelect(customName.trim()) },
                enabled = customName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("분석 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SuccessView(state: ImportState.Success, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("✅", fontSize = 56.sp)
        Text("${state.persona.name} 분석 완료!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(state.persona.persona, modifier = Modifier.padding(16.dp), fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF5B21B6))
        }
        Text("${state.persona.messageCount}개 메시지 분석됨", fontSize = 13.sp, color = Color(0xFF9CA3AF))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("답장 생성하러 가기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorView(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("❌", fontSize = 48.sp)
        Text(message, fontSize = 15.sp, color = Color(0xFF374151), textAlign = TextAlign.Center, lineHeight = 22.sp)
        OutlinedButton(onClick = onDismiss) { Text("닫기") }
    }
}
