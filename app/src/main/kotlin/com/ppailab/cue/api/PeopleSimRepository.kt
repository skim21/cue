package com.ppailab.cue.api

/**
 * 답장 후보 생성 Repository 인터페이스.
 * 구현체: [MockPeopleSimRepository] (테스트), [PeopleSimRepositoryImpl] (프로덕션)
 */
interface PeopleSimRepository {
    /**
     * 주어진 메시지 컨텍스트에 대해 3개 스타일(공손/유머/단답)의 답장 후보를 생성한다.
     *
     * @param context 상대방 메시지 원문
     * @param persona 페르소나 지시문 (비어있으면 기본 프롬프트 사용)
     * @return 성공 시 [ReplyCandidate] 리스트(3개), 실패 시 에러 포함 Result
     */
    suspend fun generateReplies(
        context: String,
        persona: String = ""
    ): Result<List<ReplyCandidate>>
}

/**
 * 답장 후보 단일 항목.
 *
 * @param style 스타일 레이블 (예: "공손", "유머", "단답")
 * @param text  실제 답장 텍스트
 */
data class ReplyCandidate(
    val style: String,
    val text: String
)
