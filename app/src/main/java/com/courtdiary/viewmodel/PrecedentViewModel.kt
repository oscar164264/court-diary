package com.courtdiary.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.courtdiary.database.CaseDatabase
import com.courtdiary.model.Precedent
import com.courtdiary.repository.PrecedentRepository
import com.courtdiary.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(FlowPreview::class)
class PrecedentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PrecedentRepository(CaseDatabase.getDatabase(application).precedentDao())

    // ── Search ────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val precedents: StateFlow<List<Precedent>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.getAllPrecedents() else repo.searchPrecedents(q)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onSearchQueryChanged(q: String) { _searchQuery.value = q }

    // ── One-shot events ───────────────────────
    private val _result = MutableSharedFlow<PrecedentResult>()
    val result: SharedFlow<PrecedentResult> = _result

    // ── CRUD ─────────────────────────────────

    fun savePrecedent(
        id: Int,
        title: String,
        textContent: String,
        fileUri: Uri?,
        existingFilePath: String,
        existingFileName: String,
        existingMimeType: String,
        existingFileSize: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            var filePath = existingFilePath
            var fileName = existingFileName
            var mimeType = existingMimeType
            var fileSize = existingFileSize

            // Copy new file if one was picked
            if (fileUri != null) {
                val copied = FileUtils.copyToAppStorage(getApplication(), fileUri, PRECEDENTS_DIR_ID)
                if (copied != null) {
                    // Delete old file if replacing
                    if (existingFilePath.isNotBlank()) File(existingFilePath).delete()
                    filePath = copied.filePath
                    fileName = copied.fileName
                    mimeType = copied.mimeType
                    fileSize = copied.fileSize
                }
            }

            val precedent = Precedent(
                id = id,
                title = title.trim(),
                textContent = textContent.trim(),
                filePath = filePath,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize,
                createdAt = if (id == 0) now else 0L,   // 0 = will be ignored by update
                updatedAt = now
            )

            if (id == 0) {
                repo.insertPrecedent(precedent)
            } else {
                // Preserve original createdAt
                val existing = repo.getPrecedentById(id)
                repo.updatePrecedent(precedent.copy(createdAt = existing?.createdAt ?: now))
            }
            _result.emit(PrecedentResult.Success)
        }
    }

    fun deletePrecedent(precedent: Precedent) {
        viewModelScope.launch(Dispatchers.IO) {
            if (precedent.filePath.isNotBlank()) File(precedent.filePath).delete()
            repo.deletePrecedent(precedent)
        }
    }

    suspend fun getPrecedentById(id: Int): Precedent? = repo.getPrecedentById(id)

    companion object {
        // Use a fixed "virtual caseId" for precedent files so they go in their own folder
        private const val PRECEDENTS_DIR_ID = -1
    }
}

sealed class PrecedentResult {
    object Success : PrecedentResult()
    data class Error(val message: String) : PrecedentResult()
}
