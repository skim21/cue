package com.ppailab.cue.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val Purple = Color(0xFF7C3AED)

@Composable
fun SetupScreen(
    onDone: () -> Unit,
    vm: SetupViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("💬", fontSize = 72.sp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cue", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Purple)
            Text(
                "카카오톡 대화를 분석해서\n상대방 말투에 맞는 답장을 제안해줘",
                fontSize = 16.sp, color = Color(0xFF6B7280),
                textAlign = TextAlign.Center, lineHeight = 24.sp,
            )
        }

        HorizontalDivider(color = Color(0xFFE5E7EB))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("이렇게 써봐", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                listOf(
                    "1️⃣" to "카카오톡 채팅방 열기",
                    "2️⃣" to "오른쪽 상단 메뉴 → 대화 내보내기",
                    "3️⃣" to "공유 → Cue 선택",
                    "4️⃣" to "상대방 말투 분석 완료!",
                    "5️⃣" to "받은 메시지 입력 → 맞춤 답장 3개",
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

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
        ) {
            Text("시작하기", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}
