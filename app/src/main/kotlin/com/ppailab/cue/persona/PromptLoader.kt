package com.ppailab.cue.persona

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val PROMPT_PATH = "prompts/reply_v1.md"

/**
 * assets/prompts/reply_v1.md 를 읽어 플레이스홀더를 치환한다.
 *
 * 플레이스홀더:
 *  - {context}  → 상대방 메시지
 *  - {persona}  → 페르소나 지시문 (빈 문자열 허용)
 *  - {style}    → 답장 스타일 (공손 / 유머 / 단답)
 */
@Singleton
class PromptLoader @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val template: String by lazy {
        try {
            ctx.assets.open(PROMPT_PATH).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.e(e, "프롬프트 파일 로드 실패: %s", PROMPT_PATH)
            // 폴백 최소 프롬프트
            "상대방 메시지: {context}\n{style} 스타일로 한 문장 답장을 작성하세요."
        }
    }

    /**
     * 플레이스홀더를 치환한 최종 프롬프트 문자열을 반환한다.
     */
    fun load(context: String, persona: String, style: String): String {
        return template
            .replace("{context}", context)
            .replace("{persona}", persona)
            .replace("{style}", style)
    }
}
