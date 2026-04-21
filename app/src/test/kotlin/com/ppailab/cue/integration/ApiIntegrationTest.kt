package com.ppailab.cue.integration

import com.ppailab.cue.api.CueApiService
import com.ppailab.cue.api.CueReplyRequest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 실제 buddy.ppai-lab.com 서버 호출 통합 테스트.
 * 네트워크 없는 환경에서는 스킵됨.
 */
class ApiIntegrationTest {

    private lateinit var api: CueApiService

    @Before
    fun setUp() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        api = Retrofit.Builder()
            .baseUrl("https://buddy.ppai-lab.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CueApiService::class.java)
    }

    @Test
    fun `서버 연결 및 3개 답장 반환`() = runTest {
        val resp = api.getReply(CueReplyRequest(context = "오늘 뭐 해?"))

        assertTrue("ok가 true여야 함", resp.ok)
        assertEquals("3개 답장 반환해야 함", 3, resp.replies.size)
    }

    @Test
    fun `공손 유머 단답 스타일 모두 포함`() = runTest {
        val resp = api.getReply(CueReplyRequest(context = "밥 먹었어?"))
        val styles = resp.replies.map { it.style }

        assertTrue("공손 스타일 포함", styles.contains("공손"))
        assertTrue("유머 스타일 포함", styles.contains("유머"))
        assertTrue("단답 스타일 포함", styles.contains("단답"))
    }

    @Test
    fun `답장 텍스트가 비어있지 않음`() = runTest {
        val resp = api.getReply(CueReplyRequest(context = "주말에 뭐 할거야?"))

        resp.replies.forEach { reply ->
            assertFalse("'${reply.style}' 텍스트가 비어있으면 안 됨", reply.text.isBlank())
        }
    }

    @Test
    fun `긴 메시지도 처리 가능`() = runTest {
        val longMsg = "오늘 학교에서 진짜 힘든 일이 있었어. 친구랑 싸웠는데 어떻게 화해해야 할지 모르겠어. 네 생각은 어때?"
        val resp = api.getReply(CueReplyRequest(context = longMsg))

        assertTrue(resp.ok)
        assertEquals(3, resp.replies.size)
    }

    @Test
    fun `빈 context는 400 에러`() = runTest {
        try {
            val resp = api.getReply(CueReplyRequest(context = ""))
            // 서버가 ok=false 반환하거나 예외 발생
            assertFalse("빈 context는 ok=false여야 함", resp.ok)
        } catch (e: retrofit2.HttpException) {
            assertEquals("400이어야 함", 400, e.code())
        }
    }

    @Test
    fun `특정 스타일만 요청`() = runTest {
        val resp = api.getReply(CueReplyRequest(context = "잘 자", style = "공손"))
        assertTrue(resp.ok)
        // style=공손이면 replies 1개 또는 서버 구현에 따라 다를 수 있음
        assertTrue("최소 1개 반환", resp.replies.isNotEmpty())
    }
}
