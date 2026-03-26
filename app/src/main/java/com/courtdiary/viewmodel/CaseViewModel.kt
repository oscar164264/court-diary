package com.courtdiary.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.courtdiary.database.CaseDatabase
import com.courtdiary.model.CourtCase
import com.courtdiary.notification.NotificationScheduler
import com.courtdiary.repository.CaseRepository
import com.courtdiary.utils.toEndOfDayMillis
import com.courtdiary.utils.toStartOfDayMillis
import java.time.ZoneId
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

// ──────────────────────────────────────────────────────────────
// DataStore extension on Context
// ──────────────────────────────────────────────────────────────
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "court_diary_settings")

object PreferencesKeys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
}

// ──────────────────────────────────────────────────────────────
// Sealed result type for UI feedback
// ──────────────────────────────────────────────────────────────
sealed class CaseResult {
    object Success : CaseResult()
    data class Error(val message: String) : CaseResult()
}

// ──────────────────────────────────────────────────────────────
// Filter enum
// ──────────────────────────────────────────────────────────────
enum class CaseFilter { ALL, TODAY, UPCOMING, PAST }

/**
 * ViewModel shared by all screens.
 * All DB access is done through [CaseRepository] on the viewModelScope.
 */
class CaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: CaseRepository

    init {
        val dao = CaseDatabase.getDatabase(application).caseDao()
        repo = CaseRepository(dao)
    }

    // ──────────────────────────────────────────
    // All cases
    // ──────────────────────────────────────────
    val allCases: StateFlow<List<CourtCase>> = repo.getAllCases()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ──────────────────────────────────────────
    // Today's cases
    // ──────────────────────────────────────────
    val todayCases: StateFlow<List<CourtCase>> = run {
        val today = LocalDate.now()
        repo.getCasesByDate(today.toStartOfDayMillis(), today.toEndOfDayMillis())
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    // ──────────────────────────────────────────
    // Upcoming cases (today + 7 days)
    // ──────────────────────────────────────────
    val upcomingCases: StateFlow<List<CourtCase>> = run {
        val today = LocalDate.now()
        val end = today.plusDays(7)
        repo.getUpcomingCases(today.toStartOfDayMillis(), end.toEndOfDayMillis())
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    // ──────────────────────────────────────────
    // Past cases
    // ──────────────────────────────────────────
    val pastCases: StateFlow<List<CourtCase>> = run {
        val todayStart = LocalDate.now().toStartOfDayMillis()
        repo.getPastCases(todayStart)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    // ──────────────────────────────────────────
    // Hearing dates (for calendar highlights)
    // ──────────────────────────────────────────
    val hearingDates: StateFlow<Set<LocalDate>> =
        repo.getAllHearingDates()
            .map { milliList ->
                milliList.map { millis ->
                    java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }.toSet()
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // ──────────────────────────────────────────
    // Search / Filter
    // ──────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeFilter = MutableStateFlow(CaseFilter.ALL)
    val activeFilter: StateFlow<CaseFilter> = _activeFilter

    /** Reactive search results from DB */
    val searchResults: StateFlow<List<CourtCase>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.getAllCases() else repo.searchCases(q)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onFilterChanged(filter: CaseFilter) { _activeFilter.value = filter }

    // ──────────────────────────────────────────
    // Selected calendar date
    // ──────────────────────────────────────────
    private val _selectedCalendarDate = MutableStateFlow<LocalDate?>(null)
    val selectedCalendarDate: StateFlow<LocalDate?> = _selectedCalendarDate

    val casesForSelectedDate: StateFlow<List<CourtCase>> = _selectedCalendarDate
        .flatMapLatest { date ->
            if (date == null) flowOf(emptyList())
            else repo.getCasesByDate(date.toStartOfDayMillis(), date.toEndOfDayMillis())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectCalendarDate(date: LocalDate?) { _selectedCalendarDate.value = date }

    // ──────────────────────────────────────────
    // Operation results (one-shot events)
    // ──────────────────────────────────────────
    private val _operationResult = MutableSharedFlow<CaseResult>()
    val operationResult: SharedFlow<CaseResult> = _operationResult

    // ──────────────────────────────────────────
    // CRUD
    // ──────────────────────────────────────────

    fun insertCase(case: CourtCase) {
        viewModelScope.launch {
            try {
                // Validate unique case number
                val existing = repo.getCaseByCaseNumber(case.caseNumber)
                if (existing != null) {
                    _operationResult.emit(CaseResult.Error("CourtCase number '${case.caseNumber}' already exists."))
                    return@launch
                }
                repo.insertCase(case)
                _operationResult.emit(CaseResult.Success)
                // Re-schedule notifications after adding a case
                NotificationScheduler.schedule(getApplication())
            } catch (e: Exception) {
                _operationResult.emit(CaseResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateCase(case: CourtCase) {
        viewModelScope.launch {
            try {
                // Check uniqueness excluding the current case id
                val existing = repo.getCaseByCaseNumber(case.caseNumber)
                if (existing != null && existing.id != case.id) {
                    _operationResult.emit(CaseResult.Error("CourtCase number '${case.caseNumber}' already in use."))
                    return@launch
                }
                repo.updateCase(case)
                _operationResult.emit(CaseResult.Success)
                NotificationScheduler.schedule(getApplication())
            } catch (e: Exception) {
                _operationResult.emit(CaseResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteCase(case: CourtCase) {
        viewModelScope.launch {
            try {
                repo.deleteCase(case)
                _operationResult.emit(CaseResult.Success)
            } catch (e: Exception) {
                _operationResult.emit(CaseResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    suspend fun getCaseById(id: Int): CourtCase? = repo.getCaseById(id)

    // ──────────────────────────────────────────
    // Settings (DataStore)
    // ──────────────────────────────────────────
    val notificationsEnabled: StateFlow<Boolean> =
        getApplication<Application>().dataStore.data
            .map { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val darkModeEnabled: StateFlow<Boolean> =
        getApplication<Application>().dataStore.data
            .map { prefs -> prefs[PreferencesKeys.DARK_MODE_ENABLED] ?: false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            }
            if (enabled) {
                NotificationScheduler.schedule(getApplication())
            } else {
                NotificationScheduler.cancel(getApplication())
            }
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[PreferencesKeys.DARK_MODE_ENABLED] = enabled
            }
        }
    }

    // ──────────────────────────────────────────
    // Backup & Restore
    // ──────────────────────────────────────────
    private val gson = Gson()

    /** Exports all cases to a JSON file in the app's files directory. Returns the File. */
    suspend fun exportToJson(): File {
        val cases = repo.getAllCasesOnce()
        val json = gson.toJson(cases)
        val file = File(getApplication<Application>().filesDir, "court_diary_backup.json")
        file.writeText(json)
        return file
    }

    /** Parses the JSON string and inserts all cases (skips duplicates). */
    fun importFromJson(json: String) {
        viewModelScope.launch {
            try {
                val type = object : TypeToken<List<CourtCase>>() {}.type
                val cases: List<CourtCase> = gson.fromJson(json, type)
                var imported = 0
                cases.forEach { c ->
                    val existing = repo.getCaseByCaseNumber(c.caseNumber)
                    if (existing == null) {
                        repo.insertCase(c.copy(id = 0)) // reset id so Room auto-generates
                        imported++
                    }
                }
                _operationResult.emit(CaseResult.Success)
                NotificationScheduler.schedule(getApplication())
            } catch (e: Exception) {
                _operationResult.emit(CaseResult.Error("Import failed: ${e.message}"))
            }
        }
    }

    // ──────────────────────────────────────────
    // Sample data (first-launch seed)
    // ──────────────────────────────────────────
    fun seedSampleData() {
        viewModelScope.launch {
            if (repo.getAllCasesOnce().isNotEmpty()) return@launch // already has data

            val today = LocalDate.now()
            val sampleCases = listOf(
                CourtCase(
                    caseNumber = "CV-2024-001",
                    courtName = "Civil Court – Branch 3",
                    clientName = "Ahmed Al Mansouri",
                    clientPhone = "+971501234567",
                    lastHearingDate = today.minusDays(30).toStartOfDayMillis(),
                    nextHearingDate = today.plusDays(1).toStartOfDayMillis(),
                    lastStage = "Evidence Submission",
                    nextStage = "Final Arguments",
                    notes = "Client needs to prepare all original documents."
                ),
                CourtCase(
                    caseNumber = "CR-2024-078",
                    courtName = "Criminal Court",
                    clientName = "Sara Khalid",
                    clientPhone = "+971509876543",
                    nextHearingDate = today.toStartOfDayMillis(),
                    lastStage = "Initial Hearing",
                    nextStage = "Witness Examination",
                    notes = "Bail application pending."
                ),
                CourtCase(
                    caseNumber = "FM-2023-215",
                    courtName = "Family Court",
                    clientName = "Hassan Ibrahim",
                    clientPhone = "+971551112233",
                    lastHearingDate = today.minusDays(60).toStartOfDayMillis(),
                    nextHearingDate = today.plusDays(10).toStartOfDayMillis(),
                    lastStage = "Mediation",
                    nextStage = "Settlement Discussion",
                    notes = "Both parties agreed to mediation."
                ),
                CourtCase(
                    caseNumber = "CC-2024-042",
                    courtName = "Commercial Court",
                    clientName = "Al Noor Trading LLC",
                    clientPhone = "+97142223344",
                    lastHearingDate = today.minusDays(14).toStartOfDayMillis(),
                    nextHearingDate = today.plusDays(3).toStartOfDayMillis(),
                    lastStage = "Document Review",
                    nextStage = "Expert Witness Testimony",
                    notes = "Contract dispute – claim value AED 2.5M"
                )
            )
            sampleCases.forEach { repo.insertCase(it) }
        }
    }

}
