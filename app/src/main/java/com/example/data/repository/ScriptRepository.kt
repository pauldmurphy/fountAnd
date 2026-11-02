package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.ScriptDao
import com.example.data.local.ScriptEntity
import com.example.domain.FountainParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "fountain_studio_prefs")

data class StudioPreferences(
    val isDarkMode: Boolean = true,
    val showLineNumbers: Boolean = true,
    val lastActiveScriptId: Long = -1L
)

class ScriptRepository(
    private val scriptDao: ScriptDao,
    private val appContext: Context
) {
    private val keyDarkMode = booleanPreferencesKey("is_dark_mode")
    private val keyLineNumbers = booleanPreferencesKey("show_line_numbers")
    private val keyLastScriptId = longPreferencesKey("last_active_script_id")

    val allScripts: Flow<List<ScriptEntity>> = scriptDao.getAllScripts()

    val preferencesFlow: Flow<StudioPreferences> = appContext.studioDataStore.data.map { prefs ->
        StudioPreferences(
            isDarkMode = prefs[keyDarkMode] ?: true,
            showLineNumbers = prefs[keyLineNumbers] ?: true,
            lastActiveScriptId = prefs[keyLastScriptId] ?: -1L
        )
    }

    suspend fun ensureStarterScript(): ScriptEntity {
        val count = scriptDao.getScriptCount()
        if (count == 0) {
            val starter = ScriptEntity(
                title = "THE LAST PROJECTIONIST",
                content = FountainParser.SAMPLE_FOUNTAIN_SCRIPT,
                updatedAt = System.currentTimeMillis()
            )
            val id = scriptDao.insertScript(starter)
            setLastActiveScriptId(id)
            return starter.copy(id = id)
        }
        return scriptDao.getScriptById(1L) ?: ScriptEntity(
            title = "THE LAST PROJECTIONIST",
            content = FountainParser.SAMPLE_FOUNTAIN_SCRIPT
        )
    }

    suspend fun getScriptById(id: Long): ScriptEntity? = scriptDao.getScriptById(id)

    suspend fun insertScript(script: ScriptEntity): Long {
        val id = scriptDao.insertScript(script)
        setLastActiveScriptId(id)
        return id
    }

    suspend fun updateScript(script: ScriptEntity) {
        scriptDao.updateScript(script)
    }

    suspend fun deleteScript(id: Long) {
        scriptDao.deleteScriptById(id)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        appContext.studioDataStore.edit { prefs ->
            prefs[keyDarkMode] = enabled
        }
    }

    suspend fun setShowLineNumbers(enabled: Boolean) {
        appContext.studioDataStore.edit { prefs ->
            prefs[keyLineNumbers] = enabled
        }
    }

    suspend fun setLastActiveScriptId(id: Long) {
        appContext.studioDataStore.edit { prefs ->
            prefs[keyLastScriptId] = id
        }
    }
}
