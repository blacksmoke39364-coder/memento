package com.example.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.MementoDatabase
import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryStatus
import com.example.data.model.MemoryTimelineEvent
import com.example.data.model.MemoryType
import com.example.data.model.SecurityAuditLog
import com.example.data.model.SourceType
import com.example.data.model.UserSession
import com.example.data.remote.GeminiService
import com.example.data.remote.MemoryRetrievalResult
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    AUTH,
    ONBOARDING,
    HOME,
    REMEMBER,
    ASK,
    MEMORY_HUB,
    TIMELINE,
    PEOPLE,
    PROMISES,
    WAITING,
    PRIVACY_CENTER,
    SETTINGS
}

class MementoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MementoDatabase.getDatabase(application)
    private val geminiService = GeminiService(
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    val repository = MemoryRepository(database.memoryDao(), geminiService)

    private val _currentScreen = MutableStateFlow(Screen.AUTH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeUserSession = MutableStateFlow<UserSession?>(null)
    val activeUserSession: StateFlow<UserSession?> = _activeUserSession.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryEntity>>(emptyList())
    val memories: StateFlow<List<MemoryEntity>> = _memories.asStateFlow()

    private val _selectedTimelineEntity = MutableStateFlow("Car Keys")
    val selectedTimelineEntity: StateFlow<String> = _selectedTimelineEntity.asStateFlow()

    private val _timelineEvents = MutableStateFlow<List<MemoryTimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<MemoryTimelineEvent>> = _timelineEvents.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<SecurityAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<SecurityAuditLog>> = _auditLogs.asStateFlow()

    private val _isMissionControl = MutableStateFlow(false)
    val isMissionControl: StateFlow<Boolean> = _isMissionControl.asStateFlow()

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            // Check existing user session
            val session = repository.currentUserSession.firstOrNull()
            if (session != null) {
                _activeUserSession.value = session
                _themeMode.value = session.themeMode
                _isMissionControl.value = !session.simpleMode
                loadUserData(session.userId)
            } else {
                val defaultSession = UserSession(
                    userId = "usr_local_vault",
                    displayName = "Alex",
                    emailOrPhone = "alex@memento.local",
                    authProvider = "LOCAL_VAULT",
                    isOnboarded = true
                )
                _activeUserSession.value = defaultSession
                repository.saveSession(defaultSession)
                repository.seedInitialMemoriesIfEmpty(defaultSession.userId)
                loadUserData(defaultSession.userId)
            }
        }
    }

    fun authenticateUser(session: UserSession) {
        viewModelScope.launch {
            _activeUserSession.value = session
            _themeMode.value = session.themeMode
            repository.saveSession(session)
            repository.seedInitialMemoriesIfEmpty(session.userId)
            loadUserData(session.userId)
            _currentScreen.value = if (session.isOnboarded) Screen.HOME else Screen.ONBOARDING
        }
    }

    fun completeOnboarding() {
        val user = _activeUserSession.value ?: return
        viewModelScope.launch {
            val updated = user.copy(isOnboarded = true)
            _activeUserSession.value = updated
            repository.saveSession(updated)
            _currentScreen.value = Screen.HOME
        }
    }

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            repository.getActiveMemories(userId).collect { list ->
                _memories.value = list
            }
        }
        viewModelScope.launch {
            repository.getAuditLogs(userId).collect { logs ->
                _auditLogs.value = logs
            }
        }
        loadTimelineForEntity(_selectedTimelineEntity.value)
    }

    fun loadTimelineForEntity(entityTitle: String, memoryId: String = "") {
        _selectedTimelineEntity.value = entityTitle
        val userId = _activeUserSession.value?.userId ?: return
        viewModelScope.launch {
            repository.getTimelineForEntity(userId, entityTitle, memoryId).collect { events ->
                _timelineEvents.value = events
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun saveMemory(rawInput: String, sourceType: SourceType, imageUri: String?) {
        val userId = _activeUserSession.value?.userId ?: return
        viewModelScope.launch {
            val saved = repository.remember(userId, rawInput, sourceType, imageUri)
            loadUserData(userId)
        }
    }

    suspend fun askMemento(question: String): MemoryRetrievalResult {
        val userId = _activeUserSession.value?.userId ?: return MemoryRetrievalResult(
            entity = "Unknown",
            answer = "Please sign in to access your memory.",
            howIKnow = "No authenticated session.",
            isKnown = false
        )
        return repository.askMemento(userId, question)
    }

    fun correctLocation(memoryId: String, newLocation: String) {
        val userId = _activeUserSession.value?.userId ?: return
        viewModelScope.launch {
            repository.correctMemoryLocation(userId, memoryId, newLocation)
            loadUserData(userId)
            loadTimelineForEntity(_selectedTimelineEntity.value, memoryId)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        val userId = _activeUserSession.value?.userId ?: return
        viewModelScope.launch {
            repository.deleteMemory(userId, memory.id)
            loadUserData(userId)
        }
    }

    fun deleteCategory(type: MemoryType) {
        val userId = _activeUserSession.value?.userId ?: return
        viewModelScope.launch {
            repository.deleteMemoriesByType(userId, type)
            loadUserData(userId)
        }
    }

    fun deleteAllMemories() {
        val userId = _activeUserSession.value?.userId ?: return
        viewModelScope.launch {
            repository.deleteAllMemories(userId)
            loadUserData(userId)
        }
    }

    suspend fun exportData(): String {
        val userId = _activeUserSession.value?.userId ?: return "{}"
        return repository.exportDataJson(userId)
    }

    fun toggleMissionControl(enabled: Boolean) {
        _isMissionControl.value = enabled
        val user = _activeUserSession.value ?: return
        viewModelScope.launch {
            val updated = user.copy(simpleMode = !enabled)
            _activeUserSession.value = updated
            repository.saveSession(updated)
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        val user = _activeUserSession.value ?: return
        viewModelScope.launch {
            val updated = user.copy(themeMode = mode)
            _activeUserSession.value = updated
            repository.saveSession(updated)
        }
    }

    fun logout() {
        val userId = _activeUserSession.value?.userId
        viewModelScope.launch {
            if (userId != null) {
                database.memoryDao().deleteUserSession(userId)
            }
            _activeUserSession.value = null
            _memories.value = emptyList()
            _timelineEvents.value = emptyList()
            _currentScreen.value = Screen.AUTH
        }
    }
}
