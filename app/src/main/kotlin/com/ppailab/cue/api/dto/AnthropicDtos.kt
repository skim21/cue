package com.ppailab.cue.api.dto

import com.google.gson.annotations.SerializedName

// ─── Request ────────────────────────────────────────────────────────────────

data class AnthropicRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("max_tokens")
    val maxTokens: Int = 256,

    @SerializedName("messages")
    val messages: List<AnthropicMessage>
)

data class AnthropicMessage(
    @SerializedName("role")
    val role: String,          // "user" | "assistant"

    @SerializedName("content")
    val content: String
)

// ─── Response ───────────────────────────────────────────────────────────────

data class AnthropicResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: List<AnthropicContent>,

    @SerializedName("model")
    val model: String,

    @SerializedName("stop_reason")
    val stopReason: String?,

    @SerializedName("usage")
    val usage: AnthropicUsage?
)

data class AnthropicContent(
    @SerializedName("type")
    val type: String,          // "text"

    @SerializedName("text")
    val text: String
)

data class AnthropicUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,

    @SerializedName("output_tokens")
    val outputTokens: Int
)

// ─── Error ──────────────────────────────────────────────────────────────────

data class AnthropicError(
    @SerializedName("type")
    val type: String,

    @SerializedName("error")
    val error: AnthropicErrorDetail
)

data class AnthropicErrorDetail(
    @SerializedName("type")
    val type: String,

    @SerializedName("message")
    val message: String
)
