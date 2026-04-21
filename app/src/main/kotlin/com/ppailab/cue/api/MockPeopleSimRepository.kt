package com.ppailab.cue.api

import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

/**
 * 테스트·UI 개발용 Mock Repository.
 * 실제 API 호출 없이 800ms 딜레이 후 하드코딩된 3개 후보를 반환한다.
 */
class MockPeopleSimRepository @Inject constructor() : PeopleSimRepository {

    override suspend fun generateReplies(
        context: String,
        persona: String,
    ): Result<List<ReplyCandidate>> {
        delay(800L)
        return Result.success(listOf(
            ReplyCandidate("공손", "네, 알겠습니다. 감사합니다."),
            ReplyCandidate("유머", "ㅋㅋ 진짜요? 대박이다"),
            ReplyCandidate("단답", "ㅇㅇ"),
        ))
    }

    override suspend fun generateScenarios(
        context: String, persona: String, name: String,
    ): Result<List<ConversationScenario>> {
        delay(1000L)
        return Result.success(listOf(
            ConversationScenario(50, "약속 잡기", listOf(
                ChatExchange(name, context),
                ChatExchange("나", "집에 있어! 너는?"),
                ChatExchange(name, "나도~ 그럼 저녁에 밥 먹을래?"),
            )),
            ConversationScenario(30, "짧게 끝나는 대화", listOf(
                ChatExchange(name, context),
                ChatExchange("나", "바빠서 ㅠ 나중에 얘기해"),
                ChatExchange(name, "ㅇㅇ 알겠어!"),
            )),
            ConversationScenario(20, "근황 공유", listOf(
                ChatExchange(name, context),
                ChatExchange("나", "그냥 쉬고 있어~ 요즘 어때?"),
                ChatExchange(name, "나 요즘 일이 많아서 ㅠㅠ"),
                ChatExchange("나", "힘들겠다ㅠ 화이팅!"),
            )),
        ))
    }

    override suspend fun analyzePersona(
        name: String,
        messages: List<String>,
    ): Result<String> {
        delay(1000L)
        return Result.success("친근한 말투를 써요. 이모티콘을 자주 사용하고 감정 표현이 풍부해요.")
    }
}
