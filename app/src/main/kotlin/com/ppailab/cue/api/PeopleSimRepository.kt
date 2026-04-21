package com.ppailab.cue.api

interface PeopleSimRepository {
    suspend fun generateReplies(
        context: String,
        persona: String = "",
    ): Result<List<ReplyCandidate>>

    suspend fun generateScenarios(
        context: String,
        persona: String = "",
        name: String = "상대방",
    ): Result<List<ConversationScenario>>

    suspend fun analyzePersona(
        name: String,
        messages: List<String>,
    ): Result<String>
}

data class ReplyCandidate(val style: String, val text: String)

data class ChatExchange(val sender: String, val message: String)

data class ConversationScenario(
    val probability: Int,
    val title: String,
    val exchanges: List<ChatExchange>,
)
