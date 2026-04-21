package com.ppailab.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ppailab.cue.onboarding.SetupScreen
import com.ppailab.cue.onboarding.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CueTheme {
                CueApp()
            }
        }
    }
}

@Composable
fun CueTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun CueApp() {
    val vm: SetupViewModel = hiltViewModel()
    val isSetupDone by vm.isSetupDone.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    AnimatedContent(targetState = isSetupDone && !showSettings) { done ->
        if (done) {
            MainScreen(onSettingsClick = { showSettings = true })
        } else {
            SetupScreen(onDone = { showSettings = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSettingsClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cue", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("⚡", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("준비 완료!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "카카오톡에서 상대방 메시지를 꾹 눌러\n텍스트를 선택한 뒤\n\"Cue 답장\" 을 탭해봐.",
                fontSize = 15.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(32.dp))
            HowToCard()
        }
    }
}

@Composable
private fun HowToCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("사용 방법", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            listOf(
                "1️⃣" to "카톡 채팅방 열기",
                "2️⃣" to "상대방 메시지 꾹 누르기",
                "3️⃣" to "텍스트 선택 후 더보기 탭",
                "4️⃣" to "\"Cue 답장\" 선택",
                "5️⃣" to "3가지 답장 후보 중 탭 → 복사 완료!",
            ).forEach { (emoji, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 18.sp)
                    Text(text, fontSize = 14.sp, color = Color(0xFF374151))
                }
            }
        }
    }
}
