package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScriptEntity
import com.example.data.repository.ScriptRepository
import com.example.domain.FountainDocument
import com.example.domain.FountainParser
import com.example.domain.SceneOutlineItem
import com.example.domain.TitlePageData
import com.example.ui.editor.QuickInsertToken
import com.example.ui.editor.applyQuickInsert
import com.example.util.DocumentExportManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkspaceViewMode {
    EDITOR,
    SPLIT,
    PREVIEW
}

enum class SaveStatus {
    SAVED_LOCALLY,
    LINKED_TO_DRIVE,
    SAVING
}

data class FountainUiState(
    val activeScriptId: Long = 0L,
    val scriptTitle: String = "THE LAST PROJECTIONIST",
    val linkedUri: String? = null,
    val linkedFileName: String? = null,
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val parsedDocument: FountainDocument = FountainDocument(),
    val viewMode: WorkspaceViewMode = WorkspaceViewMode.EDITOR,
    val isDarkMode: Boolean = true,
    val showLineNumbers: Boolean = true,
    val saveStatus: SaveStatus = SaveStatus.SAVED_LOCALLY,
    val showTitlePageDialog: Boolean = false,
    val showNewScriptDialog: Boolean = false,
    val scrollToLineEvent: Int? = null,
    val snackbarMessage: String? = null
) {
    val suggestedFountainFileName: String
        get() {
            val base = scriptTitle.trim()
                .ifBlank { "Untitled_Screenplay" }
                .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                .trim()
                .replace(Regex("\\s+"), "_")
            return if (base.endsWith(".fountain", ignoreCase = true)) base else "$base.fountain"
        }

    val suggestedPdfFileName: String
        get() {
            val base = scriptTitle.trim()
                .ifBlank { "Untitled_Screenplay" }
                .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                .trim()
                .replace(Regex("\\s+"), "_")
            return if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
        }
}

class FountainViewModel(
    private val repository: ScriptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FountainUiState())
    val uiState: StateFlow<FountainUiState> = _uiState.asStateFlow()

    val scripts: StateFlow<List<ScriptEntity>> = repository.allScripts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            val prefs = repository.preferencesFlow.first()
            val starterOrExisting = if (prefs.lastActiveScriptId > 0) {
                repository.getScriptById(prefs.lastActiveScriptId) ?: repository.ensureStarterScript()
            } else {
                repository.ensureStarterScript()
            }

            val parsed = FountainParser.parse(starterOrExisting.content)
            val resolvedTitle = parsed.titlePage.title.ifBlank { starterOrExisting.title }

            _uiState.update { state ->
                state.copy(
                    activeScriptId = starterOrExisting.id,
                    scriptTitle = resolvedTitle,
                    linkedUri = starterOrExisting.linkedUri,
                    linkedFileName = starterOrExisting.linkedFileName,
                    textFieldValue = TextFieldValue(
                        text = starterOrExisting.content,
                        selection = TextRange(0)
                    ),
                    parsedDocument = parsed,
                    isDarkMode = prefs.isDarkMode,
                    showLineNumbers = prefs.showLineNumbers,
                    saveStatus = if (starterOrExisting.linkedUri != null) {
                        SaveStatus.LINKED_TO_DRIVE
                    } else {
                        SaveStatus.SAVED_LOCALLY
                    }
                )
            }
        }
    }

    fun onTextFieldValueChange(newValue: TextFieldValue) {
        val textChanged = newValue.text != _uiState.value.textFieldValue.text
        if (!textChanged) {
            _uiState.update { it.copy(textFieldValue = newValue) }
            return
        }

        val parsed = FountainParser.parse(newValue.text)
        val derivedTitle = parsed.titlePage.title.ifBlank {
            _uiState.value.scriptTitle.ifBlank { "UNTITLED SCREENPLAY" }
        }

        _uiState.update { state ->
            state.copy(
                textFieldValue = newValue,
                parsedDocument = parsed,
                scriptTitle = derivedTitle,
                saveStatus = SaveStatus.SAVING
            )
        }

        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(450L)
            persistCurrentScriptNow()
        }
    }

    suspend fun persistCurrentScriptNow() {
        val current = _uiState.value
        if (current.activeScriptId <= 0L) return
        val entity = ScriptEntity(
            id = current.activeScriptId,
            title = current.scriptTitle.ifBlank { "UNTITLED SCREENPLAY" },
            content = current.textFieldValue.text,
            linkedUri = current.linkedUri,
            linkedFileName = current.linkedFileName,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateScript(entity)
        _uiState.update { state ->
            state.copy(
                saveStatus = if (state.linkedUri != null) {
                    SaveStatus.LINKED_TO_DRIVE
                } else {
                    SaveStatus.SAVED_LOCALLY
                }
            )
        }
    }

    fun onQuickInsert(token: QuickInsertToken) {
        val updated = applyQuickInsert(_uiState.value.textFieldValue, token)
        onTextFieldValueChange(updated)
    }

    fun setViewMode(mode: WorkspaceViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun toggleDarkMode() {
        val next = !_uiState.value.isDarkMode
        _uiState.update { it.copy(isDarkMode = next) }
        viewModelScope.launch {
            repository.setDarkMode(next)
        }
    }

    fun toggleLineNumbers() {
        val next = !_uiState.value.showLineNumbers
        _uiState.update { it.copy(showLineNumbers = next) }
        viewModelScope.launch {
            repository.setShowLineNumbers(next)
        }
    }

    fun setShowTitlePageDialog(show: Boolean) {
        _uiState.update { it.copy(showTitlePageDialog = show) }
    }

    fun setShowNewScriptDialog(show: Boolean) {
        _uiState.update { it.copy(showNewScriptDialog = show) }
    }

    fun applyTitlePage(titleData: TitlePageData) {
        val updatedRaw = FountainParser.applyTitlePageMetadata(
            rawContent = _uiState.value.textFieldValue.text,
            titleData = titleData
        )
        val parsed = FountainParser.parse(updatedRaw)
        val resolvedTitle = titleData.title.ifBlank { _uiState.value.scriptTitle }

        _uiState.update { state ->
            state.copy(
                scriptTitle = resolvedTitle,
                textFieldValue = TextFieldValue(
                    text = updatedRaw,
                    selection = TextRange(0)
                ),
                parsedDocument = parsed,
                showTitlePageDialog = false,
                snackbarMessage = "Updated Fountain Title Page metadata"
            )
        }
        scheduleAutosave()
    }

    fun jumpToScene(scene: SceneOutlineItem) {
        val text = _uiState.value.textFieldValue.text
        val safeOffset = scene.charOffset.coerceIn(0, text.length)
        _uiState.update { state ->
            state.copy(
                textFieldValue = state.textFieldValue.copy(
                    selection = TextRange(safeOffset)
                ),
                scrollToLineEvent = scene.lineIndex
            )
        }
    }

    fun consumeScrollEvent() {
        _uiState.update { it.copy(scrollToLineEvent = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun selectScript(script: ScriptEntity) {
        viewModelScope.launch {
            persistCurrentScriptNow()
            repository.setLastActiveScriptId(script.id)
            val parsed = FountainParser.parse(script.content)
            _uiState.update { state ->
                state.copy(
                    activeScriptId = script.id,
                    scriptTitle = parsed.titlePage.title.ifBlank { script.title },
                    linkedUri = script.linkedUri,
                    linkedFileName = script.linkedFileName,
                    textFieldValue = TextFieldValue(
                        text = script.content,
                        selection = TextRange(0)
                    ),
                    parsedDocument = parsed,
                    saveStatus = if (script.linkedUri != null) {
                        SaveStatus.LINKED_TO_DRIVE
                    } else {
                        SaveStatus.SAVED_LOCALLY
                    }
                )
            }
        }
    }

    fun createNewScript(title: String, author: String) {
        viewModelScope.launch {
            persistCurrentScriptNow()
            val cleanTitle = title.trim().ifBlank { "UNTITLED SCREENPLAY" }
            val cleanAuthor = author.trim().ifBlank { "Screenwriter" }
            val templateContent = buildString {
                append("Title: ").append(cleanTitle).append("\n")
                append("Credit: Written by\n")
                append("Author: ").append(cleanAuthor).append("\n\n")
                append("FADE IN:\n\n")
                append("INT. LOCATION - NIGHT\n\n")
                append("Describe the opening image here.\n\n")
            }
            val newEntity = ScriptEntity(
                title = cleanTitle,
                content = templateContent,
                updatedAt = System.currentTimeMillis()
            )
            val id = repository.insertScript(newEntity)
            val parsed = FountainParser.parse(templateContent)
            _uiState.update { state ->
                state.copy(
                    activeScriptId = id,
                    scriptTitle = cleanTitle,
                    linkedUri = null,
                    linkedFileName = null,
                    textFieldValue = TextFieldValue(
                        text = templateContent,
                        selection = TextRange(templateContent.length)
                    ),
                    parsedDocument = parsed,
                    saveStatus = SaveStatus.SAVED_LOCALLY,
                    showNewScriptDialog = false,
                    snackbarMessage = "Created \"$cleanTitle\""
                )
            }
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            val all = scripts.value
            if (all.size <= 1) {
                _uiState.update {
                    it.copy(snackbarMessage = "Cannot delete the only screenplay in your library")
                }
                return@launch
            }
            repository.deleteScript(script.id)
            if (_uiState.value.activeScriptId == script.id) {
                val remaining = all.firstOrNull { it.id != script.id } ?: repository.ensureStarterScript()
                selectScript(remaining)
            }
            _uiState.update { it.copy(snackbarMessage = "Deleted \"${script.title}\"") }
        }
    }

    fun importFountainUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            persistCurrentScriptNow()
            val result = DocumentExportManager.readFountainFromUri(context, uri)
            result.onSuccess { (fileName, content) ->
                val parsed = FountainParser.parse(content)
                val fallbackTitle = fileName
                    .removeSuffix(".fountain")
                    .removeSuffix(".txt")
                    .replace("_", " ")
                    .ifBlank { "IMPORTED SCREENPLAY" }
                val resolvedTitle = parsed.titlePage.title.ifBlank { fallbackTitle }

                val newEntity = ScriptEntity(
                    title = resolvedTitle,
                    content = content,
                    linkedUri = uri.toString(),
                    linkedFileName = fileName,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = repository.insertScript(newEntity)
                _uiState.update { state ->
                    state.copy(
                        activeScriptId = newId,
                        scriptTitle = resolvedTitle,
                        linkedUri = uri.toString(),
                        linkedFileName = fileName,
                        textFieldValue = TextFieldValue(content, TextRange(0)),
                        parsedDocument = parsed,
                        saveStatus = SaveStatus.LINKED_TO_DRIVE,
                        snackbarMessage = "Opened \"$fileName\""
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(snackbarMessage = "Could not open file: ${err.localizedMessage ?: "Read error"}")
                }
            }
        }
    }

    fun saveToExternalUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val content = _uiState.value.textFieldValue.text
            val result = DocumentExportManager.writeFountainToUri(context, uri, content)
            result.onSuccess { fileName ->
                _uiState.update { state ->
                    state.copy(
                        linkedUri = uri.toString(),
                        linkedFileName = fileName,
                        saveStatus = SaveStatus.LINKED_TO_DRIVE,
                        snackbarMessage = "Saved to \"$fileName\" (Google Drive / Device)"
                    )
                }
                persistCurrentScriptNow()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(snackbarMessage = "Save failed: ${err.localizedMessage ?: "Write error"}")
                }
            }
        }
    }

    fun saveToAlreadyLinkedUri(context: Context): Boolean {
        val linked = _uiState.value.linkedUri ?: return false
        val uri = runCatching { Uri.parse(linked) }.getOrNull() ?: return false
        saveToExternalUri(context, uri)
        return true
    }

    fun exportPdfToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val doc = _uiState.value.parsedDocument
            val result = DocumentExportManager.exportFormattedPdfToUri(context, uri, doc)
            result.onSuccess { fileName ->
                _uiState.update {
                    it.copy(snackbarMessage = "Exported formatted screenplay PDF to \"$fileName\"")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(snackbarMessage = "PDF export failed: ${err.localizedMessage ?: "Write error"}")
                }
            }
        }
    }

    class Factory(
        private val repository: ScriptRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FountainViewModel(repository) as T
        }
    }
}
