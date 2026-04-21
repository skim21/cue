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
        persona: String
    ): Result<List<ReplyCandidate>> {
        Timber.d("[Mock] generateReplies called. context='%s'", context.take(50))
        delay(800L)

        val candidates = listOf(
            ReplyCandidate(style = "공손", text = "네, 알겠습니다. 감사합니다."),
            ReplyCandidate(style = "유머", text = "ㅋㅋ 진짜요? 대박이다"),
            ReplyCandidate(style = "단답", text = "ㅇㅇ")
        )

        Timber.d("[Mock] returning %d candidates", candidates.size)
        return Result.success(candidates)
    }
}
