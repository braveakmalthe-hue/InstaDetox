package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DetoxSession
import com.example.data.repository.DetoxRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone

sealed class TimerState {
    object Idle : TimerState()
    object Running : TimerState()
    object Paused : TimerState()
    object Finished : TimerState()
}

class DetoxViewModel(private val repository: DetoxRepository) : ViewModel() {

    // Database flow of all sessions
    val sessionsState: StateFlow<List<DetoxSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived statistics: Streak
    val streakState: StateFlow<Int> = sessionsState.map { sessions ->
        calculateStreak(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Derived statistics: Total time saved
    val totalTimeSavedState: StateFlow<Double> = sessionsState.map { sessions ->
        sessions.sumOf { it.durationSeconds }.toDouble() / 3600.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Timer States
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState = _timerState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(900L) // Default 15 mins (900s)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _targetSeconds = MutableStateFlow(900L)
    val targetSeconds = _targetSeconds.asStateFlow()

    private val _sessionType = MutableStateFlow("Digital Fast")
    val sessionType = _sessionType.asStateFlow()

    // Quotes and Reflection State
    private val _currentQuote = MutableStateFlow("")
    val currentQuote = _currentQuote.asStateFlow()

    val reflectionText = MutableStateFlow("")
    val moodRating = MutableStateFlow("Calm")

    private var timerJob: Job? = null

    private val detoxQuotes = listOf(
        "The algorithm is a slot machine designed to capture your focus. Reclaim your crown.",
        "Your undivided attention is the most valuable currency on earth.",
        "Nothing on social media is more important than the physical space you are sitting in.",
        "The discover feed is a reflection of everyone else's lives. Live your own.",
        "Be a producer of your real life, not a consumer of curated highlight reels.",
        "Reclaim your silent boredom. Innovation and peace of mind are born in quiet spaces.",
        "They are monetizing your nervous system. Take a breath and log off.",
        "Enjoy the freedom of existing without the need to prove it to strangers.",
        "Real connection happens eye-to-eye, at one single instance of time.",
        "The world is quiet, vast, and waiting for you outside the 6-inch glowing rectangle.",
        "Social posts are edited, filtered, and staged. Your messy present is authentic and real."
    )

    init {
        rotateQuote()
    }

    fun rotateQuote() {
        _currentQuote.value = detoxQuotes.random()
    }

    fun setTimerDuration(seconds: Long, type: String = "Digital Fast") {
        if (_timerState.value is TimerState.Idle) {
            _targetSeconds.value = seconds
            _remainingSeconds.value = seconds
            _sessionType.value = type
        }
    }

    fun startTimer() {
        if (_timerState.value is TimerState.Idle || _timerState.value is TimerState.Paused) {
            _timerState.value = TimerState.Running
            rotateQuote()
            timerJob = viewModelScope.launch {
                while (_remainingSeconds.value > 0) {
                    delay(1000)
                    _remainingSeconds.value -= 1
                }
                _timerState.value = TimerState.Finished
                // Setup default draft text for reflection
                reflectionText.value = ""
                moodRating.value = "Calm"
            }
        }
    }

    fun pauseTimer() {
        if (_timerState.value is TimerState.Running) {
            timerJob?.cancel()
            _timerState.value = TimerState.Paused
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.Idle
        _remainingSeconds.value = _targetSeconds.value
    }

    fun saveCompletedSession() {
        val durationCompleted = _targetSeconds.value - _remainingSeconds.value
        if (durationCompleted > 0) {
            viewModelScope.launch {
                val session = DetoxSession(
                    sessionType = _sessionType.value,
                    durationSeconds = durationCompleted,
                    reflectionText = reflectionText.value.trim(),
                    moodRating = moodRating.value
                )
                repository.insertSession(session)
                resetTimer()
            }
        }
    }

    fun saveInstantChallenge(challengeName: String, durationMins: Long, mood: String, reflection: String) {
        viewModelScope.launch {
            val session = DetoxSession(
                sessionType = challengeName,
                durationSeconds = durationMins * 60,
                reflectionText = reflection,
                moodRating = mood
            )
            repository.insertSession(session)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSessionById(sessionId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            resetTimer()
        }
    }

    private fun calculateStreak(sessions: List<DetoxSession>): Int {
        if (sessions.isEmpty()) return 0
        val zoneId = TimeZone.getDefault()
        val dayMultiplier = 24 * 60 * 60 * 1000L

        // Group sessions to unique system date keys (epoch dates) based on localized system offsets
        val epochDays = sessions.map {
            val localTime = it.timestamp + zoneId.getOffset(it.timestamp)
            localTime / dayMultiplier
        }.toSet().sortedDescending()

        if (epochDays.isEmpty()) return 0

        val todayLocal = (System.currentTimeMillis() + zoneId.getOffset(System.currentTimeMillis())) / dayMultiplier
        val mostRecentDay = epochDays.first()

        // If the gap between today and the most recent session is > 1 day, the streak is broken
        if (todayLocal - mostRecentDay > 1) {
            return 0
        }

        var streak = 0
        var expectedDay = mostRecentDay
        for (day in epochDays) {
            if (day == expectedDay) {
                streak++
                expectedDay--
            } else if (day > expectedDay) {
                continue
            } else {
                break
            }
        }
        return streak
    }
}

class DetoxViewModelFactory(private val repository: DetoxRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetoxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetoxViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
