package com.ppailab.cue.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val Purple      = Color(0xFF7C3AED)
private val PurpleLight = Color(0xFFEDE9FE)
private val Green       = Color(0xFF059669)
private val Red         = Color(0xFFDC2626)

@Composable
fun SetupScreen(
    onDone: () -> Unit,
    vm: SetupViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.setupDone) {
        if (ui.setupDone) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── 로고 & 타이틀 ──────────────────────────────────────
        Text("⚡", fontSize = 64.sp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cue", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Purple)
            Text(
                "카톡 메시지 선택 → AI 답장 후보 3개\n설치만 하면 바로 사용 가능해",
                fontSize = 15.sp, color = Color(0xFF6B7280),
                textAlign = TextAlign.Center, lineHeight = 22.sp,
            )
        }

        HorizontalDivider(color = Color(0xFFE5E7EB))

        // ── 사용 방법 카드 ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("이렇게 써봐", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                listOf(
                    "1️⃣" to "카카오톡 열기",
                    "2️⃣" to "상대방 메시지 꾹 누르기",
                    "3️⃣" to "텍스트 선택 → 더보기",
                    "4️⃣" to "\"Cue 답장\" 탭",
                    "5️⃣" to "3가지 답장 중 탭 → 클립보드 복사!",
                ).forEach { (emoji, text) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(emoji, fontSize = 20.sp)
                        Text(text, fontSize = 14.sp, color = Color(0xFF374151))
                    }
                }
            }
        }

        // ── 서버 연결 상태 ──────────────────────────────────────
        AnimatedVisibility(
            visible = ui.testStatus !is TestStatus.Idle,
            enter = fadeIn(tween(200)), exit = fadeOut(tween(200))
        ) {
            when (val s = ui.testStatus) {
                is TestStatus.Success -> StatusBanner(
                    emoji = "✅", text = "서버 연결 성공!",
                    bg = Color(0xFFECFDF5), fg = Green
                )
                is TestStatus.Error -> StatusBanner(
                    emoji = "⚠️", text = s.message,
                    bg = Color(0xFFFEF2F2), fg = Red
                )
                else -> {}
            }
        }

        // ── 버튼 ────────────────────────────────────────────────
        Button(
            onClick = vm::testConnection,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = ui.testStatus !is TestStatus.Loading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
        ) {
            AnimatedContent(targetState = ui.testStatus is TestStatus.Loading) { loading ->
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("연결 확인 중...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("시작하기", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        TextButton(onClick = vm::skipTest) {
            Text("바로 시작 (연결 테스트 건너뛰기)", fontSize = 13.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
private fun StatusBanner(emoji: String, text: String, bg: Color, fg: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(text, fontSize = 14.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}
