package com.ppailab.cue

import com.ppailab.cue.api.MockPeopleSimRepository
import com.ppailab.cue.api.ReplyCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockRepositoryTest {

    private lateinit var repository: MockPeopleSimRepository

    @Before
    fun setUp() {
        repository = MockPeopleSimRepository()
    }

    // ── 기본 반환 검증 ──────────────────────────────────────────

    @Test
    fun `정확히 3개 후보 반환`() = runTest {
        val result = repository.generateReplies("안녕하세요")
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `공손 스타일 포함`() = runTest {
        val styles = repository.generateReplies("테스트").getOrThrow().map { it.style }
        assertTrue(styles.contains("공손"))
    }

    @Test
    fun `유머 스타일 포함`() = runTest {
        val styles = repository.generateReplies("테스트").getOrThrow().map { it.style }
        assertTrue(styles.contains("유머"))
    }

    @Test
    fun `단답 스타일 포함`() = runTest {
        val styles = repository.generateReplies("테스트").getOrThrow().map { it.style }
        assertTrue(styles.contains("단답"))
    }

    @Test
    fun `모든 후보 텍스트 비어있지 않음`() = runTest {
        repository.generateReplies("오늘 뭐 해?").getOrThrow().forEach { c ->
            assertFalse("'${c.style}' 텍스트가 비어있으면 안 됨", c.text.isBlank())
        }
    }

    // ── 하드코딩 값 검증 ───────────────────────────────────────

    @Test
    fun `공손 텍스트 하드코딩 일치`() = runTest {
        val c = repository.generateReplies("감사합니다").getOrThrow().first { it.style == "공손" }
        assertEquals("네, 알겠습니다. 감사합니다.", c.text)
    }

    @Test
    fun `유머 텍스트 하드코딩 일치`() = runTest {
        val c = repository.generateReplies("대박").getOrThrow().first { it.style == "유머" }
        assertEquals("ㅋㅋ 진짜요? 대박이다", c.text)
    }

    @Test
    fun `단답 텍스트 하드코딩 일치`() = runTest {
        val c = repository.generateReplies("ㅇㅋ").getOrThrow().first { it.style == "단답" }
        assertEquals("ㅇㅇ", c.text)
    }

    // ── 엣지 케이스 ────────────────────────────────────────────

    @Test
    fun `빈 persona 허용`() = runTest {
        val result = repository.generateReplies("밥 먹었어?", persona = "")
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `커스텀 persona 허용`() = runTest {
        val result = repository.generateReplies("주말에 뭐 해?", persona = "친한 친구처럼 답장해")
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `빈 context도 결과 반환`() = runTest {
        val result = repository.generateReplies("")
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `긴 context도 결과 반환`() = runTest {
        val long = "오늘 학교에서 진짜 힘든 일이 있었어. 친구랑 싸웠는데 어떻게 화해해야 할지 모르겠어. 네 생각은 어때? 나는 진짜 화가 많이 났거든."
        val result = repository.generateReplies(long)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `스타일 순서는 공손-유머-단답`() = runTest {
        val candidates = repository.generateReplies("테스트").getOrThrow()
        assertEquals("공손", candidates[0].style)
        assertEquals("유머", candidates[1].style)
        assertEquals("단답", candidates[2].style)
    }

    @Test
    fun `ReplyCandidate data class 동등성 확인`() {
        val a = ReplyCandidate("공손", "안녕하세요")
        val b = ReplyCandidate("공손", "안녕하세요")
        assertEquals(a, b)
    }
}
