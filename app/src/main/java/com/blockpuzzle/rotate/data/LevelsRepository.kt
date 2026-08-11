package com.blockpuzzle.rotate.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.blockpuzzle.rotate.domain.LevelDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.levelsDataStore by preferencesDataStore(name = "levels")

/**
 * Persists the full list of levels (legacy-seeded + user-created) as one JSON blob in
 * DataStore Preferences. The list is small (a handful to a few dozen levels), so a single
 * blob is simpler than one entry per level and keeps [save]/[delete] trivially transactional —
 * no INTERNET permission is ever needed for this, it's purely local like [RecordsRepository].
 */
class LevelsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val levelsKey = stringPreferencesKey("levels_json")
    private val defaultsSeededKey = booleanPreferencesKey("defaults_seeded")

    val levels: Flow<List<LevelDefinition>> = context.levelsDataStore.data.map { prefs -> decode(prefs[levelsKey]) }

    val defaultsSeeded: Flow<Boolean> = context.levelsDataStore.data.map { prefs -> prefs[defaultsSeededKey] ?: false }

    private fun decode(raw: String?): List<LevelDefinition> =
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString<List<LevelDefinition>>(raw)

    /** Inserts [level], or replaces the existing entry with the same tag. */
    suspend fun save(level: LevelDefinition) {
        context.levelsDataStore.edit { prefs ->
            val current = decode(prefs[levelsKey]).toMutableList()
            val index = current.indexOfFirst { it.tag == level.tag }
            if (index >= 0) current[index] = level else current.add(level)
            prefs[levelsKey] = json.encodeToString(current)
        }
    }

    suspend fun delete(tag: String) {
        context.levelsDataStore.edit { prefs ->
            val current = decode(prefs[levelsKey]).filterNot { it.tag == tag }
            prefs[levelsKey] = json.encodeToString(current)
        }
    }

    /** One-shot: seeds [initialLevels] and marks defaults as seeded, atomically. Used only by the legacy migration. */
    suspend fun seedDefaults(initialLevels: List<LevelDefinition>) {
        context.levelsDataStore.edit { prefs ->
            prefs[levelsKey] = json.encodeToString(initialLevels)
            prefs[defaultsSeededKey] = true
        }
    }
}
