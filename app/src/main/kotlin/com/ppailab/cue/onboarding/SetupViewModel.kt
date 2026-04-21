package com.ppailab.cue.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SetupUiState(
    val testStatus: TestStatus = TestStatus.Idle,
    val setupDone: Boolean = false,
)

sealed interface TestStatus {
    data object Idle    : TestStatus
    data object Loading : TestStatus
    data object Success : TestStatus
    data class Error(val message: String) : TestStatus
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settings: AppSettings,
    private val repository: PeopleSimRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(SetupUiState())
    val ui: StateFlow<SetupUiState> = _ui

    val isSetupDone: StateFlow<Boolean> = settings.setupDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun testConnection() {
        _ui.update { it.copy(testStatus = TestStatus.Loading) }
        viewModelScope.launch {
            val result = repository.generateReplies("안녕 잘 지내?")
            if (result.isSuccess) {
                settings.markSetupDone()
                _ui.update { it.copy(testStatus = TestStatus.Success, setupDone = true) }
                Timber.i("Setup: 연결 테스트 성공")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
                _ui.update { it.copy(testStatus = TestStatus.Error("연결 실패: $msg")) }
                Timber.w("Setup: 연결 실패 — $msg")
            }
        }
    }

    fun skipTest() {
        viewModelScope.launch {
            settings.markSetupDone()
            _ui.update { it.copy(setupDone = true) }
        }
    }
}
