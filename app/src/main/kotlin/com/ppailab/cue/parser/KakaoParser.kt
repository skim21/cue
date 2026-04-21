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

    // 날짜/시스템 줄 패턴 (건너뜀)
    private val DATE_LINE = Regex("""^\d{4}년 \d+월 \d+일.{0,20}$""")

    fun parse(text: String): KakaoConversation? {
        val lines = text.lines()

        // 헤더에서 대화상대 추출
        val partnerName = lines.firstOrNull { it.startsWith("대화상대:") }
            ?.removePrefix("대화상대:")?.trim()
            ?: inferPartnerName(lines)

        // 형식 파싱 시도
        if (partnerName != null) {
            val partnerMessages = mutableListOf<String>()
            var totalMessages = 0

            for (line in lines) {
                val trimmed = line.trim()
                val matched = NEW_FMT.matchEntire(trimmed)?.destructured?.let { (sender, msg) ->
                    totalMessages++
                    if (sender == partnerName) partnerMessages.add(msg)
                    true
                } ?: OLD_FMT.matchEntire(trimmed)?.destructured?.let { (sender, msg) ->
                    totalMessages++
                    if (sender == partnerName) partnerMessages.add(msg)
                    true
                }
            }

            if (partnerMessages.isNotEmpty()) {
                return KakaoConversation(partnerName, partnerMessages, totalMessages)
            }
        }

        // 형식 파싱 실패 → 모든 텍스트 줄을 그대로 전달 (서버 Claude가 분석)
        val allLines = lines
            .map { it.trim() }
            .filter { it.isNotBlank() && !DATE_LINE.matches(it) }
        if (allLines.isEmpty()) return null

        // 이름을 못 찾으면 null 이름으로 반환 (ImportActivity에서 이름 입력 처리)
        return KakaoConversation(
            partnerName = partnerName ?: "",
            partnerMessages = allLines,
            totalMessages = allLines.size,
        )
    }

    private fun inferPartnerName(lines: List<String>): String? =
        lines.mapNotNull { NEW_FMT.find(it)?.groupValues?.get(1) }
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key
}
