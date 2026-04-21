package com.ppailab.cue.parser

data class KakaoConversation(
    val partnerName: String,
    val partnerMessages: List<String>,
    val totalMessages: Int,
)

data class KakaoParsed(
    val speakers: List<String>,          // 등장인물 목록 (많이 말한 순)
    val allLines: List<String>,          // 형식 파싱 성공 시 전체 줄 (발신자 포함)
    val rawLines: List<String>,          // 형식 파싱 실패 시 fallback용 원본 줄
)

object KakaoParser {

    private val NEW_FMT = Regex("""^\[([^\]]+)] \[(?:오전|오후) \d+:\d+] (.+)$""")
    private val OLD_FMT = Regex("""\d{4}년 \d+월 \d+일 (?:오전|오후) \d+:\d+, (.+?) : (.+)""")
    private val DATE_LINE = Regex("""^\d{4}년 \d+월 \d+일.{0,20}$""")

    // 1MB로 잘라서 파싱
    fun preparse(text: String): KakaoParsed {
        val lines = text.lines()
        val senderCount = mutableMapOf<String, Int>()
        val structuredLines = mutableListOf<String>()
        var hasStructure = false

        for (line in lines) {
            val trimmed = line.trim()
            NEW_FMT.matchEntire(trimmed)?.destructured?.let { (sender, _) ->
                senderCount[sender] = (senderCount[sender] ?: 0) + 1
                structuredLines.add(trimmed)
                hasStructure = true
                return@let
            } ?: OLD_FMT.matchEntire(trimmed)?.destructured?.let { (sender, _) ->
                senderCount[sender] = (senderCount[sender] ?: 0) + 1
                structuredLines.add(trimmed)
                hasStructure = true
            }
        }

        // 많이 말한 순으로 정렬
        val speakers = senderCount.entries.sortedByDescending { it.value }.map { it.key }

        val rawLines = if (!hasStructure) {
            lines.map { it.trim() }.filter { it.isNotBlank() && !DATE_LINE.matches(it) }
        } else emptyList()

        return KakaoParsed(speakers = speakers, allLines = structuredLines, rawLines = rawLines)
    }

    fun extract(parsed: KakaoParsed, partnerName: String): KakaoConversation {
        if (parsed.allLines.isEmpty()) {
            // 형식 없음 → 전체 텍스트로 분석
            return KakaoConversation(partnerName, parsed.rawLines, parsed.rawLines.size)
        }

        val partnerMessages = mutableListOf<String>()
        var totalMessages = 0

        for (line in parsed.allLines) {
            NEW_FMT.matchEntire(line)?.destructured?.let { (sender, msg) ->
                totalMessages++
                if (sender == partnerName) partnerMessages.add(msg)
                return@let
            } ?: OLD_FMT.matchEntire(line)?.destructured?.let { (sender, msg) ->
                totalMessages++
                if (sender == partnerName) partnerMessages.add(msg)
            }
        }

        return KakaoConversation(partnerName, partnerMessages.ifEmpty { parsed.rawLines }, totalMessages)
    }
}
