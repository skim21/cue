package com.ppailab.cue.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ppailab.cue.floating.FloatingService
import com.ppailab.cue.persona.SavedPersona
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPersonaTap: (SavedPersona) -> Unit,
    onImportTap: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val personas by vm.personas.collectAsState()
    var deleteTarget by remember { mutableStateOf<SavedPersona?>(null) }
    var bubbleOn by remember { mutableStateOf(false) }

    // 오버레이 권한 확인 + 버블 상태 동기화
    LaunchedEffect(Unit) {
        bubbleOn = Settings.canDrawOverlays(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cue", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    // 버블 토글
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("⚡버블", fontSize = 12.sp, color = Color.White.copy(alpha = .85f))
                        Switch(
                            checked = bubbleOn,
                            onCheckedChange = { on ->
                                if (on) {
                                    if (Settings.canDrawOverlays(context)) {
                                        FloatingService.start(context)
                                        bubbleOn = true
                                    } else {
                                        // 오버레이 권한 요청
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                } else {
                                    FloatingService.stop(context)
                                    bubbleOn = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF5B21B6),
                                uncheckedThumbColor = Color.White.copy(alpha = .7f),
                                uncheckedTrackColor = Color.White.copy(alpha = .3f),
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF7C3AED),
                    titleContentColor = Color.White,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportTap,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("대화 가져오기") },
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
            )
        }
    ) { padding ->
        if (personas.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(personas, key = { it.id }) { p ->
                    PersonaCard(
                        persona = p,
                        onTap = { onPersonaTap(p) },
                        onDelete = { deleteTarget = p },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("${p.name} 삭제") },
            text = { Text("이 페르소나를 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { vm.delete(p.id); deleteTarget = null }) {
                    Text("삭제", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun PersonaCard(
    persona: SavedPersona,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = remember(persona.createdAt) {
        SimpleDateFormat("MM/dd", Locale.KOREA).format(Date(persona.createdAt))
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    persona.name.take(1),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(persona.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    persona.persona,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "메시지 ${persona.messageCount}개 · $date 분석",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFD1D5DB))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("💬", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("아직 분석한 대화가 없어요", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "카카오톡에서 대화를 내보내면\n상대방 말투를 분석해서 맞춤 답장을 제안해요",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            lineHeight = 21.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        HowToCard()
    }
}

@Composable
private fun HowToCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("가져오는 방법", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            listOf(
                "1️⃣" to "카카오톡 채팅방 열기",
                "2️⃣" to "오른쪽 상단 메뉴 → 대화 내보내기",
                "3️⃣" to "공유 → Cue 선택",
                "4️⃣" to "분석 완료 → 맞춤 답장 바로 사용!",
            ).forEach { (emoji, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(emoji, fontSize = 18.sp)
                    Text(text, fontSize = 14.sp, color = Color(0xFF374151))
                }
            }
        }
    }
}
