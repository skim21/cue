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

    override suspend fun analyzePersona(
        name: String,
        messages: List<String>,
    ): Result<String> {
        delay(1000L)
        return Result.success("친근한 말투를 써요. 이모티콘을 자주 사용하고 감정 표현이 풍부해요.")
    }
}
