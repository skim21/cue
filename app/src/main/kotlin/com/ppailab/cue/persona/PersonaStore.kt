package com.ppailab.cue.persona

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SavedPersona(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val persona: String,
    val messageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Singleton
class PersonaStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val file = context.filesDir.resolve("personas.json")
    private val gson = Gson()
    private val type = object : TypeToken<List<SavedPersona>>() {}.type

    fun loadAll(): List<SavedPersona> {
        if (!file.exists()) return emptyList()
        return try { gson.fromJson<List<SavedPersona>>(file.readText(), type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }

    fun save(persona: SavedPersona) {
        val list = loadAll().toMutableList()
        val i = list.indexOfFirst { it.name == persona.name }
        if (i >= 0) list[i] = persona else list.add(0, persona)
        file.writeText(gson.toJson(list))
    }

    fun delete(id: String) {
        file.writeText(gson.toJson(loadAll().filter { it.id != id }))
    }

    fun findById(id: String): SavedPersona? = loadAll().firstOrNull { it.id == id }
}
