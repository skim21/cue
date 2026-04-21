package com.ppailab.cue.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ── ppai-lab 서버 DTO ────────────────────────────────────────────────────────

data class CueReplyRequest(
    @SerializedName("context") val context: String,
    @SerializedName("style")   val style: String = "all",
)

data class CueReplyResponse(
    @SerializedName("ok")      val ok: Boolean,
    @SerializedName("replies") val replies: List<CueReply> = emptyList(),
    @SerializedName("error")   val error: String? = null,
)

data class CueReply(
    @SerializedName("style") val style: String,
    @SerializedName("text")  val text: String,
)

// ── Retrofit 서비스 ──────────────────────────────────────────────────────────

interface CueApiService {
    @POST("api/cue/reply")
    suspend fun getReply(@Body req: CueReplyRequest): CueReplyResponse
}

// ── Repository 구현 ──────────────────────────────────────────────────────────

@Singleton
class PeopleSimRepositoryImpl @Inject constructor(
    private val api: CueApiService,
) : PeopleSimRepository {

    override suspend fun generateReplies(
        context: String,
        persona: String,
    ): Result<List<ReplyCandidate>> = runCatching {
        Timber.d("generateReplies: context='%s'", context.take(80))
        val resp = api.getReply(CueReplyRequest(context = context))
        if (!resp.ok) error(resp.error ?: "서버 오류")
        resp.replies.map { ReplyCandidate(style = it.style, text = it.text) }
    }
}
