package com.ppailab.cue.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ── DTOs ─────────────────────────────────────────────────────────────────────

data class CueReplyRequest(
    @SerializedName("context") val context: String,
    @SerializedName("style")   val style: String = "all",
    @SerializedName("persona") val persona: String = "",
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

data class AnalyzePersonaRequest(
    @SerializedName("name")     val name: String,
    @SerializedName("messages") val messages: List<String>,
)

data class AnalyzePersonaResponse(
    @SerializedName("ok")      val ok: Boolean,
    @SerializedName("persona") val persona: String = "",
    @SerializedName("error")   val error: String? = null,
)

data class ScenarioRequest(
    @SerializedName("context") val context: String,
    @SerializedName("persona") val persona: String = "",
    @SerializedName("name")    val name: String = "상대방",
    @SerializedName("mode")    val mode: String = "partner",
)

data class ScenarioExchange(
    @SerializedName("sender")  val sender: String,
    @SerializedName("message") val message: String,
)

data class ScenarioItem(
    @SerializedName("probability") val probability: Int,
    @SerializedName("title")       val title: String,
    @SerializedName("exchanges")   val exchanges: List<ScenarioExchange>,
)

data class ScenarioResponse(
    @SerializedName("ok")        val ok: Boolean,
    @SerializedName("scenarios") val scenarios: List<ScenarioItem> = emptyList(),
    @SerializedName("error")     val error: String? = null,
)

// ── Retrofit 서비스 ──────────────────────────────────────────────────────────

interface CueApiService {
    @POST("api/cue/reply")
    suspend fun getReply(@Body req: CueReplyRequest): CueReplyResponse

    @POST("api/cue/analyze-persona")
    suspend fun analyzePersona(@Body req: AnalyzePersonaRequest): AnalyzePersonaResponse

    @POST("api/cue/scenarios")
    suspend fun getScenarios(@Body req: ScenarioRequest): ScenarioResponse
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
        Timber.d("generateReplies context='%s'", context.take(80))
        val resp = api.getReply(CueReplyRequest(context = context, persona = persona))
        if (!resp.ok) error(resp.error ?: "서버 오류")
        resp.replies.map { ReplyCandidate(it.style, it.text) }
    }

    override suspend fun generateScenarios(
        context: String,
        persona: String,
        name: String,
        mode: String,
    ): Result<List<ConversationScenario>> = runCatching {
        val resp = api.getScenarios(ScenarioRequest(context, persona, name, mode))
        if (!resp.ok) error(resp.error ?: "서버 오류")
        resp.scenarios.map { s ->
            ConversationScenario(
                probability = s.probability,
                title = s.title,
                exchanges = s.exchanges.map { ChatExchange(it.sender, it.message) },
            )
        }
    }

    override suspend fun analyzePersona(
        name: String,
        messages: List<String>,
    ): Result<String> = runCatching {
        Timber.d("analyzePersona name='%s' messages=%d", name, messages.size)
        val resp = api.analyzePersona(AnalyzePersonaRequest(name, messages))
        if (!resp.ok) error(resp.error ?: "분석 실패")
        resp.persona
    }
}
