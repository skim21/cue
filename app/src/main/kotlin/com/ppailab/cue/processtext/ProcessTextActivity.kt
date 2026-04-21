package com.ppailab.cue.processtext

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.api.ReplyCandidate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 텍스트 선택 공유 시트(PROCESS_TEXT)의 진입점.
 *
 * 흐름:
 *  1. Intent.EXTRA_PROCESS_TEXT 에서 선택 텍스트 수신
 *  2. PeopleSimRepository.generateReplies() 호출 (코루틴)
 *  3. 결과를 ReplyBottomSheet Compose UI로 표시
 *  4. 에러 시 Toast 알림 후 종료
 */
@AndroidEntryPoint
class ProcessTextActivity : ComponentActivity() {

    @Inject
    lateinit var repository: PeopleSimRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent
            .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            ?.trim()

        if (selectedText.isNullOrEmpty()) {
            Timber.w("ProcessTextActivity: 선택 텍스트 없음")
            Toast.makeText(this, "선택된 텍스트가 없어.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Timber.d("ProcessTextActivity: selectedText='%s'", selectedText.take(80))

        // 로딩 상태를 보여주는 초기 화면
        setContent {
            MaterialTheme {
                ReplyBottomSheet(
                    candidates = emptyList(),
                    isLoading = true,
                    onDismiss = { finish() },
                    onCopy = { /* 로딩 중 복사 불가 */ }
                )
            }
        }

        // 코루틴으로 API 호출
        lifecycleScope.launch {
            repository.generateReplies(context = selectedText)
                .onSuccess { candidates ->
                    Timber.d("답장 후보 %d개 수신", candidates.size)
                    showBottomSheet(candidates)
                }
                .onFailure { e ->
                    Timber.e(e, "답장 생성 실패")
                    Toast.makeText(
                        this@ProcessTextActivity,
                        "답장 생성에 실패했어. 다시 시도해줘.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
        }
    }

    private fun showBottomSheet(candidates: List<ReplyCandidate>) {
        setContent {
            MaterialTheme {
                ReplyBottomSheet(
                    candidates = candidates,
                    isLoading = false,
                    onDismiss = { finish() },
                    onCopy = { text ->
                        copyToClipboard(text)
                        Toast.makeText(this, "복사됐어!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("cue_reply", text)
        clipboard.setPrimaryClip(clip)
        Timber.d("클립보드 복사 완료: '%s'", text.take(40))
    }
}
