package com.ppailab.cue.api

import com.ppailab.cue.api.dto.AnthropicRequest
import com.ppailab.cue.api.dto.AnthropicResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Anthropic Messages API Retrofit 인터페이스.
 * Authorization 헤더는 OkHttp Interceptor에서 동적으로 주입한다.
 */
interface AnthropicService {

    @Headers(
        "anthropic-version: 2023-06-01",
        "Content-Type: application/json"
    )
    @POST("messages")
    suspend fun createMessage(
        @Body request: AnthropicRequest
    ): Response<AnthropicResponse>
}
