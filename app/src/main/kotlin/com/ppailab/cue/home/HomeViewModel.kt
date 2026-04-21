package com.ppailab.cue.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppailab.cue.persona.PersonaStore
import com.ppailab.cue.persona.SavedPersona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val store: PersonaStore,
) : ViewModel() {

    private val _personas = MutableStateFlow<List<SavedPersona>>(emptyList())
    val personas: StateFlow<List<SavedPersona>> = _personas

    init { reload() }

    fun reload() {
        viewModelScope.launch { _personas.value = store.loadAll() }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            store.delete(id)
            reload()
        }
    }
}
