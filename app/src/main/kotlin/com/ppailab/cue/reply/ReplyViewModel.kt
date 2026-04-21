package com.ppailab.cue.reply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppailab.cue.api.ConversationScenario
import com.ppailab.cue.api.PeopleSimRepository
import com.ppailab.cue.persona.PersonaStore
import com.ppailab.cue.persona.SavedPersona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReplyUiState {
    object Idle : ReplyUiState()
    object Loading : ReplyUiState()
    data class Success(val scenarios: List<ConversationScenario>) : ReplyUiState()
    data class Error(val message: String) : ReplyUiState()
}

@HiltViewModel
class ReplyViewModel @Inject constructor(
    private val repo: PeopleSimRepository,
    private val store: PersonaStore,
) : ViewModel() {

    private val _state = MutableStateFlow<ReplyUiState>(ReplyUiState.Idle)
    val state: StateFlow<ReplyUiState> = _state

    private val _persona = MutableStateFlow<SavedPersona?>(null)
    val persona: StateFlow<SavedPersona?> = _persona

    fun loadPersona(id: String) { _persona.value = store.findById(id) }

    fun generate(message: String, mode: String = "partner") {
        val p = _persona.value
        viewModelScope.launch {
            _state.value = ReplyUiState.Loading
            repo.generateScenarios(message, p?.persona ?: "", p?.name ?: "상대방", mode)
                .onSuccess { _state.value = ReplyUiState.Success(it) }
                .onFailure { _state.value = ReplyUiState.Error(it.message ?: "오류") }
        }
    }

    fun reset() { _state.value = ReplyUiState.Idle }
}
