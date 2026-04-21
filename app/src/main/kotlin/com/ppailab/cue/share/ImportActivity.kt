package com.ppailab.cue.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
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
import com.ppailab.cue.parser.KakaoParser
import com.ppailab.cue.persona.PersonaStore
import com.ppailab.cue.persona.SavedPersona
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ImportActivity : ComponentActivity() {

    @Inject lateinit var repo: PeopleSimRepository
    @Inject lateinit var store: PersonaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent?.clipData?.getItemAt(0)?.uri
            ?: (intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            ?: run { finish(); return }

        setContent {
            MaterialTheme {
                ImportScreen(uri)
            }
        }
    }

    @Composable
    private fun ImportScreen(uri: Uri) {
        var uiState by remember { mutableStateOf<ImportState>(ImportState.Loading) }

        LaunchedEffect(uri) {
            uiState = runImport(uri)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = uiState) {
                is ImportState.Loading -> LoadingView()
                is ImportState.Success -> SuccessView(s) { openMainAndFinish() }
                is ImportState.Error -> ErrorView(s.message) { finish() }
            }
        }
    }

    private suspend fun runImport(uri: Uri): ImportState {
        return withContext(Dispatchers.IO) {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: return@withContext ImportState.Error("파일을 읽을 수 없어요")

                val conv = KakaoParser.parse(text)
                    ?: return@withContext ImportState.Error("카카오톡 대화 파일이 아닌 것 같아요\n(대화 내보내기 → .txt 파일을 공유해주세요)")

                val personaResult = repo.analyzePersona(conv.partnerName, conv.partnerMessages)
                personaResult.fold(
                    onSuccess = { personaText ->
                        val saved = SavedPersona(
                            name = conv.partnerName,
                            persona = personaText,
                            messageCount = conv.totalMessages,
                        )
                        store.save(saved)
                        ImportState.Success(saved)
                    },
                    onFailure = { ImportState.Error("분석 실패: ${it.message}") }
                )
            } catch (e: Exception) {
                ImportState.Error("오류: ${e.message}")
            }
        }
    }

    private fun openMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}

sealed class ImportState {
    object Loading : ImportState()
    data class Success(val persona: SavedPersona) : ImportState()
    data class Error(val message: String) : ImportState()
}

@Composable
private fun LoadingView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Color(0xFF7C3AED), modifier = Modifier.size(48.dp))
        Text("대화 분석 중...", fontSize = 16.sp, color = Color(0xFF6B7280))
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
        Text(
            "${state.persona.name} 분석 완료!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                state.persona.persona,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color(0xFF5B21B6),
            )
        }
        Text(
            "메시지 ${state.persona.messageCount}개 분석됨",
            fontSize = 13.sp,
            color = Color(0xFF9CA3AF),
        )
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
