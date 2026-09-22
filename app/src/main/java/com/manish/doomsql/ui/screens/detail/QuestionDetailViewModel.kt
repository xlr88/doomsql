package com.manish.doomsql.ui.screens.detail

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manish.doomsql.data.engine.ComparisonResult
import com.manish.doomsql.data.engine.SqlExecutionEngine
import com.manish.doomsql.data.engine.SqlResultComparator
import com.manish.doomsql.data.local.entity.QuestionProgressEntity
import com.manish.doomsql.data.model.QueryExecutionResult
import com.manish.doomsql.data.model.QueryResult
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.repository.QuestionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExecutionStatus {
    data object Idle : ExecutionStatus
    data object Running : ExecutionStatus
    data class SuccessCorrect(val result: QueryResult) : ExecutionStatus
    data class SuccessIncorrect(
        val actualResult: QueryResult,
        val expectedResult: QueryResult,
        val reason: String
    ) : ExecutionStatus
    data class SqlError(val message: String) : ExecutionStatus
}

data class QuestionDetailUiState(
    val question: Question? = null,
    val nextQuestionId: String? = null,
    val progress: QuestionProgressEntity? = null,
    val editorValue: TextFieldValue = TextFieldValue(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val selectedTabIndex: Int = 0, // 0 = Problem, 1 = Editor
    val executionStatus: ExecutionStatus = ExecutionStatus.Idle,
    val showSolutionDialog: Boolean = false,
    val isSolutionVisible: Boolean = false, // Always starts hidden when opening the screen
    val isLoading: Boolean = true
)

class QuestionDetailViewModel(
    private val questionId: String,
    private val repository: QuestionRepository,
    private val sqlEngine: SqlExecutionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()

    private var draftSaveJob: Job? = null
    private var errorAutoDismissJob: Job? = null

    init {
        loadQuestionData()
    }

    private fun loadQuestionData() {
        viewModelScope.launch {
            repository.setLastOpenedQuestionId(questionId)
            val allQuestions = repository.getQuestions()
            val currentIdx = allQuestions.indexOfFirst { it.id == questionId }
            val question = allQuestions.getOrNull(currentIdx)
            val nextQuestionId = if (currentIdx != -1 && currentIdx + 1 < allQuestions.size) {
                allQuestions[currentIdx + 1].id
            } else null

            val draft = repository.getDraft(questionId)
            val initialQuery = draft ?: "-- Write your query for ${question?.title ?: ""}\nSELECT "

            // Observe progress for this question
            launch {
                repository.progressMapFlow.collect { map ->
                    val progress = map[questionId]
                    _uiState.value = _uiState.value.copy(
                        progress = progress
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                question = question,
                nextQuestionId = nextQuestionId,
                editorValue = TextFieldValue(initialQuery),
                canUndo = false,
                canRedo = false,
                isLoading = false
            )
        }
    }

    fun onEditorValueChanged(newValue: TextFieldValue) {
        val textChanged = newValue.text != _uiState.value.editorValue.text
        if (textChanged) {
            undoStack.add(_uiState.value.editorValue)
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
        }

        val newStatus = if (textChanged && (_uiState.value.executionStatus is ExecutionStatus.SqlError || _uiState.value.executionStatus is ExecutionStatus.SuccessIncorrect)) {
            ExecutionStatus.Idle
        } else {
            _uiState.value.executionStatus
        }

        if (textChanged) {
            errorAutoDismissJob?.cancel()
        }

        _uiState.value = _uiState.value.copy(
            editorValue = newValue,
            executionStatus = newStatus,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )

        // Debounce 500ms to save draft
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(500L)
            repository.saveDraft(questionId, newValue.text)
        }
    }

    fun onUndo() {
        if (undoStack.isEmpty()) return
        val previousState = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(_uiState.value.editorValue)

        _uiState.value = _uiState.value.copy(
            editorValue = previousState,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )

        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(500L)
            repository.saveDraft(questionId, previousState.text)
        }
    }

    fun onRedo() {
        if (redoStack.isEmpty()) return
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(_uiState.value.editorValue)

        _uiState.value = _uiState.value.copy(
            editorValue = nextState,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )

        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(500L)
            repository.saveDraft(questionId, nextState.text)
        }
    }

    fun onDismissExecutionStatus() {
        errorAutoDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(executionStatus = ExecutionStatus.Idle)
    }

    fun saveDraftImmediately() {
        viewModelScope.launch {
            repository.saveDraft(questionId, _uiState.value.editorValue.text)
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }

    fun onRunQuery() {
        val question = _uiState.value.question ?: return
        val rawQuery = _uiState.value.editorValue.text

        errorAutoDismissJob?.cancel()

        // Immediately switch to Editor tab so user sees the execution & results
        _uiState.value = _uiState.value.copy(
            selectedTabIndex = 1,
            executionStatus = ExecutionStatus.Running
        )

        viewModelScope.launch {
            // Record attempt and today's activity
            repository.recordAttempt(questionId)

            // Run user's query
            val executionResult = sqlEngine.execute(question, rawQuery)
            when (executionResult) {
                is QueryExecutionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        executionStatus = ExecutionStatus.SqlError(executionResult.message)
                    )
                    // Auto-dismiss error after 6 seconds if user doesn't interact
                    errorAutoDismissJob = launch {
                        delay(6000L)
                        if (_uiState.value.executionStatus is ExecutionStatus.SqlError) {
                            _uiState.value = _uiState.value.copy(executionStatus = ExecutionStatus.Idle)
                        }
                    }
                }
                is QueryExecutionResult.Success -> {
                    // Compare user's result with question's solution
                    val expectedResult = question.expectedOutput.toQueryResult()
                    val comparison = SqlResultComparator.compare(
                        actual = executionResult.result,
                        expected = expectedResult,
                        orderSensitive = question.orderSensitive
                    )

                    if (comparison.isEqual) {
                        // Mark solved
                        repository.recordSolve(questionId)
                        _uiState.value = _uiState.value.copy(
                            executionStatus = ExecutionStatus.SuccessCorrect(executionResult.result)
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            executionStatus = ExecutionStatus.SuccessIncorrect(
                                actualResult = executionResult.result,
                                expectedResult = expectedResult,
                                reason = comparison.reason ?: "Results do not match expected output."
                            )
                        )
                    }
                }
            }
        }
    }

    fun onToggleSolutionVisibility() {
        if (_uiState.value.isSolutionVisible) {
            _uiState.value = _uiState.value.copy(isSolutionVisible = false)
            return
        }

        // To reveal solution:
        if (_uiState.value.progress?.solutionViewed == true) {
            // Already viewed previously, reveal directly
            _uiState.value = _uiState.value.copy(isSolutionVisible = true)
        } else {
            // Show confirmation dialog before revealing
            _uiState.value = _uiState.value.copy(showSolutionDialog = true)
        }
    }

    fun onDismissSolutionDialog() {
        _uiState.value = _uiState.value.copy(showSolutionDialog = false)
    }

    fun onConfirmShowSolution() {
        _uiState.value = _uiState.value.copy(
            showSolutionDialog = false,
            isSolutionVisible = true
        )
        viewModelScope.launch {
            repository.markSolutionViewed(questionId)
        }
    }

    fun onClearEditor() {
        onEditorValueChanged(TextFieldValue(""))
    }

    fun onFormatEditor() {
        val currentText = _uiState.value.editorValue.text
        if (currentText.isNotBlank()) {
            val formatted = com.manish.doomsql.data.engine.SqlFormatter.format(currentText)
            onEditorValueChanged(
                TextFieldValue(
                    text = formatted,
                    selection = androidx.compose.ui.text.TextRange(formatted.length)
                )
            )
        }
    }
}
