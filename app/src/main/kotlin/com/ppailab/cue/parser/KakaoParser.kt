package com.ppailab.cue.parser

data class KakaoConversation(
    val partnerName: String,
    val partnerMessages: List<String>,
    val totalMessages: Int,
)

object KakaoParser {

    // 새 형식:  [홍길동] [오전 10:00] 안녕!
    private val NEW_FMT = Regex("""^\[([^\]]+)] \[(?:오전|오후) \d+:\d+] (.+)$""")

    // 구 형식:  2024년 1월 1일 오전 10:00, 홍길동 : 안녕!
    private val OLD_FMT = Regex("""\d{4}년 \d+월 \d+일 (?:오전|오후) \d+:\d+, (.+?) : (.+)""")

    fun parse(text: String): KakaoConversation? {
        val lines = text.lines()

        // 헤더에서 대화상대 추출
        val partnerName = lines.firstOrNull { it.startsWith("대화상대:") }
            ?.removePrefix("대화상대:")?.trim()
            ?: inferPartnerName(lines)
            ?: return null

        val partnerMessages = mutableListOf<String>()
        var totalMessages = 0

        for (line in lines) {
            val trimmed = line.trim()
            NEW_FMT.matchEntire(trimmed)?.destructured?.let { (sender, msg) ->
                totalMessages++
                if (sender == partnerName) partnerMessages.add(msg)
                return@let
            } ?: OLD_FMT.matchEntire(trimmed)?.destructured?.let { (sender, msg) ->
                totalMessages++
                if (sender == partnerName) partnerMessages.add(msg)
            }
        }

        if (partnerMessages.isEmpty()) return null
        return KakaoConversation(partnerName, partnerMessages, totalMessages)
    }

    // 헤더가 없을 때: 가장 많이 등장하는 발신자를 상대방으로 추정
    private fun inferPartnerName(lines: List<String>): String? =
        lines.mapNotNull { NEW_FMT.find(it)?.groupValues?.get(1) }
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key
}
