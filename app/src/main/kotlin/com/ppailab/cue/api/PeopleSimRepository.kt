package com.ppailab.cue.api

interface PeopleSimRepository {
    suspend fun generateReplies(
        context: String,
        persona: String = "",
    ): Result<List<ReplyCandidate>>

    suspend fun analyzePersona(
        name: String,
        messages: List<String>,
    ): Result<String>
}

data class ReplyCandidate(
    val style: String,
    val text: String,
)
