package com.blockpuzzle.rotate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.recordsDataStore by preferencesDataStore(name = "records")

/**
 * Persists one high score per level (keyed by [com.blockpuzzle.rotate.domain.LevelDefinition.tag])
 * — no history, no averages. DataStore, so no INTERNET permission is ever needed. [keyFor] takes
 * a plain string rather than the level itself, which also lets the one-time legacy migration
 * (see `LegacyMigration.kt`) reuse it to read scores stored under the pre-constructor key format.
 */
class RecordsRepository(private val context: Context) {

    private fun keyFor(tag: String) = intPreferencesKey("best_score_$tag")

    fun bestScore(tag: String): Flow<Int> =
        context.recordsDataStore.data.map { it[keyFor(tag)] ?: 0 }

    fun allBestScores(tags: List<String>): Flow<Map<String, Int>> =
        context.recordsDataStore.data.map { prefs ->
            tags.associateWith { prefs[keyFor(it)] ?: 0 }
        }

    /** One-shot read, for the legacy migration — reads whatever is currently stored under [tag] without collecting a Flow. */
    suspend fun scoreOnce(tag: String): Int =
        context.recordsDataStore.data.map { it[keyFor(tag)] ?: 0 }.first()

    /** Stores [score] as the new record for [tag] if it beats the current one. Returns true if it did. */
    suspend fun submitScore(tag: String, score: Int): Boolean {
        var isNewRecord = false
        context.recordsDataStore.edit { prefs ->
            val key = keyFor(tag)
            val current = prefs[key] ?: 0
            if (score > current) {
                prefs[key] = score
                isNewRecord = true
            }
        }
        return isNewRecord
    }

    /** Resets [tag]'s record to 0 — used when a level's rules change enough that its old record no longer applies fairly. */
    suspend fun resetScore(tag: String) {
        context.recordsDataStore.edit { prefs -> prefs[keyFor(tag)] = 0 }
    }
}
