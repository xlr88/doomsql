package com.chaduvukondi.firstu.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaduvukondi.firstu.data.local.entity.QuestionProgressEntity
import com.chaduvukondi.firstu.data.model.Difficulty
import com.chaduvukondi.firstu.data.model.Question
import com.chaduvukondi.firstu.data.repository.QuestionRepository
import com.chaduvukondi.firstu.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter {
    ALL, SOLVED, UNSOLVED
}

data class QuestionsUiState(
    val questions: List<Question> = emptyList(),
    val progressMap: Map<String, QuestionProgressEntity> = emptyMap(),
    val searchQuery: String = "",
    val difficultyFilter: Difficulty? = null, // null means ALL
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val newQuestionsBannerCount: Int = 0,
    val isLoading: Boolean = true
) {
    val filteredQuestions: List<Question>
        get() {
            return questions.filter { q ->
                // Search query match (title, description, tags)
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.trim().lowercase()
                    q.title.lowercase().contains(query) ||
                        q.description.lowercase().contains(query) ||
                        q.tags.any { it.lowercase().contains(query) }
                }

                // Difficulty match
                val matchesDifficulty = difficultyFilter == null || q.difficulty == difficultyFilter

                // Status match
                val isSolved = progressMap[q.id]?.isSolved == true
                val matchesStatus = when (statusFilter) {
                    StatusFilter.ALL -> true
                    StatusFilter.SOLVED -> isSolved
                    StatusFilter.UNSOLVED -> !isSolved
                }

                matchesSearch && matchesDifficulty && matchesStatus
            }
        }
}

class QuestionsViewModel(
    private val repository: QuestionRepository,
    private val userPreferences: UserPreferencesRepository? = null,
    initialDifficulty: Difficulty? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _difficultyFilter = MutableStateFlow<Difficulty?>(initialDifficulty)
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _bannerFlow: Flow<Int> = userPreferences?.newQuestionsBannerCountFlow ?: MutableStateFlow(0)

    val uiState: StateFlow<QuestionsUiState> = combine(
        repository.questionsFlow,
        repository.progressMapFlow,
        _searchQuery,
        _difficultyFilter,
        combine(_statusFilter, _bannerFlow) { status, banner -> Pair(status, banner) }
    ) { questions, progressMap, search, diff, statusBannerPair ->
        val (status, banner) = statusBannerPair
        QuestionsUiState(
            questions = questions,
            progressMap = progressMap,
            searchQuery = search,
            difficultyFilter = diff,
            statusFilter = status,
            newQuestionsBannerCount = banner,
            isLoading = questions.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuestionsUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.getQuestions()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDifficultyFilterSelected(difficulty: Difficulty?) {
        _difficultyFilter.value = difficulty
    }

    fun onStatusFilterSelected(status: StatusFilter) {
        _statusFilter.value = status
    }

    fun onDismissBanner() {
        viewModelScope.launch {
            repository.dismissNewQuestionsBanner()
        }
    }
}
