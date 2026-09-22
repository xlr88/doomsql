package com.manish.doomsql.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manish.doomsql.data.local.entity.QuestionProgressEntity
import com.manish.doomsql.data.model.Difficulty
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.repository.QuestionRepository
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
    initialDifficulty: Difficulty? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _difficultyFilter = MutableStateFlow<Difficulty?>(initialDifficulty)
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<QuestionsUiState> = combine(
        _questions,
        repository.progressMapFlow,
        _searchQuery,
        _difficultyFilter,
        _statusFilter
    ) { questions, progressMap, search, diff, status ->
        QuestionsUiState(
            questions = questions,
            progressMap = progressMap,
            searchQuery = search,
            difficultyFilter = diff,
            statusFilter = status,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuestionsUiState(isLoading = true)
    )

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = repository.getQuestions()
            _questions.value = list
            _isLoading.value = false
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
}
