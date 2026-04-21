package com.ppailab.cue.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ppailab.cue.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cue_settings")

/**
 * DataStore 기반 앱 설정 저장소.
 *
 * 저장 항목:
 *  - apiKey    : Anthropic API 키 (기본값: BuildConfig.API_KEY)
 *  - baseUrl   : API 베이스 URL (기본값: BuildConfig.API_BASE_URL)
 *  - model     : 사용할 모델 ID (기본값: claude-haiku-4-5)
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val KEY_API_KEY   = stringPreferencesKey("api_key")
    private val KEY_BASE_URL  = stringPreferencesKey("base_url")
    private val KEY_MODEL     = stringPreferencesKey("model")
    private val KEY_SETUP_DONE = androidx.datastore.preferences.core.booleanPreferencesKey("setup_done")

    // ── Flows ──────────────────────────────────────────────────────────────

    val apiKey: Flow<String> = ctx.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: BuildConfig.API_KEY
    }

    val baseUrl: Flow<String> = ctx.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: BuildConfig.API_BASE_URL
    }

    val model: Flow<String> = ctx.dataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: "claude-haiku-4-5"
    }

    val setupDone: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[KEY_SETUP_DONE] ?: false
    }

    // ── Writers ────────────────────────────────────────────────────────────

    suspend fun setApiKey(value: String) {
        Timber.d("AppSettings: apiKey 업데이트")
        ctx.dataStore.edit { it[KEY_API_KEY] = value }
    }

    suspend fun setBaseUrl(value: String) {
        Timber.d("AppSettings: baseUrl 업데이트 → %s", value)
        ctx.dataStore.edit { it[KEY_BASE_URL] = value }
    }

    suspend fun setModel(value: String) {
        Timber.d("AppSettings: model 업데이트 → %s", value)
        ctx.dataStore.edit { it[KEY_MODEL] = value }
    }

    suspend fun markSetupDone() {
        ctx.dataStore.edit { it[KEY_SETUP_DONE] = true }
    }

    suspend fun clearAll() {
        Timber.w("AppSettings: 전체 초기화")
        ctx.dataStore.edit { it.clear() }
    }
}
