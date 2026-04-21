package com.ppailab.cue

import com.ppailab.cue.api.MockPeopleSimRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * MockPeopleSimRepository 단위 테스트.
 * 실제 API 호출 없이 정확히 3개의 후보를 반환하는지 검증한다.
 */
class MockRepositoryTest {

    private lateinit var repository: MockPeopleSimRepository

    @Before
    fun setUp() {
        repository = MockPeopleSimRepository()
    }

    @Test
    fun `generateReplies returns exactly 3 candidates`() = runTest {
        val result = repository.generateReplies(context = "안녕하세요")

        assertTrue("Result should be success", result.isSuccess)
        val candidates = result.getOrThrow()
        assertEquals("Should return exactly 3 candidates", 3, candidates.size)
    }

    @Test
    fun `generateReplies contains 공손 style`() = runTest {
        val candidates = repository.generateReplies("테스트").getOrThrow()
        val styles = candidates.map { it.style }
        assertTrue("Should contain 공손 style", styles.contains("공손"))
    }

    @Test
    fun `generateReplies contains 유머 style`() = runTest {
        val candidates = repository.generateReplies("테스트").getOrThrow()
        val styles = candidates.map { it.style }
        assertTrue("Should contain 유머 style", styles.contains("유머"))
    }

    @Test
    fun `generateReplies contains 단답 style`() = runTest {
        val candidates = repository.generateReplies("테스트").getOrThrow()
        val styles = candidates.map { it.style }
        assertTrue("Should contain 단답 style", styles.contains("단답"))
    }

    @Test
    fun `all candidates have non-empty text`() = runTest {
        val candidates = repository.generateReplies("오늘 뭐 해?").getOrThrow()
        candidates.forEach { candidate ->
            assertFalse(
                "Candidate '${candidate.style}' should have non-empty text",
                candidate.text.isBlank()
            )
        }
    }

    @Test
    fun `generateReplies works with empty persona`() = runTest {
        val result = repository.generateReplies(context = "밥 먹었어?", persona = "")
        assertTrue("Result should be success with empty persona", result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `generateReplies works with custom persona`() = runTest {
        val result = repository.generateReplies(
            context = "주말에 뭐 해?",
            persona = "친한 친구처럼 답장해"
        )
        assertTrue("Result should be success with custom persona", result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `공손 reply matches expected hardcoded text`() = runTest {
        val candidates = repository.generateReplies("감사합니다").getOrThrow()
        val gongson = candidates.first { it.style == "공손" }
        assertEquals("네, 알겠습니다. 감사합니다.", gongson.text)
    }

    @Test
    fun `유머 reply matches expected hardcoded text`() = runTest {
        val candidates = repository.generateReplies("대박").getOrThrow()
        val humor = candidates.first { it.style == "유머" }
        assertEquals("ㅋㅋ 진짜요? 대박이다", humor.text)
    }

    @Test
    fun `단답 reply matches expected hardcoded text`() = runTest {
        val candidates = repository.generateReplies("ㅇㅋ").getOrThrow()
        val short = candidates.first { it.style == "단답" }
        assertEquals("ㅇㅇ", short.text)
    }
}
