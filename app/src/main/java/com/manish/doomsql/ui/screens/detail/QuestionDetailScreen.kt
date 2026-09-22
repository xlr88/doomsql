package com.manish.doomsql.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.doomsql.config.AppLinks
import com.manish.doomsql.data.engine.SqlFormatter
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.model.SqlValue
import com.manish.doomsql.data.model.toSqlValue
import com.manish.doomsql.ui.components.EditorSpeedDial
import com.manish.doomsql.ui.components.SqlEditor
import com.manish.doomsql.ui.components.SqlSuggestionBar
import com.manish.doomsql.ui.components.TableView
import com.manish.doomsql.ui.screens.home.DifficultyBadge
import com.manish.doomsql.ui.theme.DifficultyMedium
import com.manish.doomsql.ui.theme.ErrorRed
import com.manish.doomsql.ui.theme.ErrorRedContainer
import com.manish.doomsql.ui.theme.SolvedGreen
import com.manish.doomsql.util.SupportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    uiState: QuestionDetailUiState,
    onNavigateBack: () -> Unit,
    onNavigateToNextQuestion: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onEditorValueChanged: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onDismissExecutionStatus: () -> Unit,
    onRunQuery: () -> Unit,
    onToggleSolutionVisibility: () -> Unit,
    onDismissSolutionDialog: () -> Unit,
    onConfirmShowSolution: () -> Unit,
    onClearEditor: () -> Unit,
    onFormatEditor: () -> Unit,
    onDisposeSaveDraft: () -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOverflowMenu by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onDisposeSaveDraft()
        }
    }

    val question = uiState.question

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = question?.title ?: "Question",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (question != null) {
                            Text(
                                text = "Dialect: ${question.sqlDialect}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (question != null) {
                        DifficultyBadge(difficulty = question.difficulty)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    // Run button ALWAYS visible in top bar!
                    Button(
                        onClick = onRunQuery,
                        enabled = uiState.executionStatus !is ExecutionStatus.Running,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("top_bar_run_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.executionStatus is ExecutionStatus.Running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("detail_overflow_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report a problem with this question") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    if (question != null) {
                                        val subject = "DoomSQL Question Issue: ${question.id}"
                                        val body = SupportHelper.buildEmailBody("Question ID: ${question.id}\nQuestion Title: ${question.title}")
                                        SupportHelper.sendSupportEmail(
                                            context = context,
                                            subject = subject,
                                            body = body,
                                            onNoEmailApp = { email ->
                                                coroutineScope.launch {
                                                    val res = snackbarHostState.showSnackbar(
                                                        message = "No email app found. Support: $email",
                                                        actionLabel = "Copy",
                                                        duration = SnackbarDuration.Long
                                                    )
                                                    if (res == SnackbarResult.ActionPerformed) {
                                                        SupportHelper.copyToClipboard(context, email)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("detail_menu_report_problem")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading || question == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("detail_loading"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Row: Problem & Editor
                TabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = uiState.selectedTabIndex == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("Problem", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_problem")
                    )
                    Tab(
                        selected = uiState.selectedTabIndex == 1,
                        onClick = { onTabSelected(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Editor", fontWeight = FontWeight.SemiBold)
                                if (uiState.progress?.isSolved == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Solved",
                                        tint = SolvedGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_editor")
                    )
                }

                // Tab Content
                if (uiState.selectedTabIndex == 0) {
                    ProblemContent(
                        question = question,
                        isSolutionVisible = uiState.isSolutionVisible,
                        onToggleSolutionVisibility = onToggleSolutionVisibility
                    )
                } else {
                    EditorContent(
                        uiState = uiState,
                        onEditorValueChanged = onEditorValueChanged,
                        onDismissExecutionStatus = onDismissExecutionStatus,
                        onNavigateToNextQuestion = onNavigateToNextQuestion,
                        onClearQuery = onClearEditor,
                        onFormatSql = onFormatEditor,
                        onUndo = onUndo,
                        onRedo = onRedo
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Solution reveal
    if (uiState.showSolutionDialog) {
        AlertDialog(
            onDismissRequest = onDismissSolutionDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = DifficultyMedium
                )
            },
            title = { Text("Show Solution?") },
            text = {
                Text("This will reveal the solution query and explanation. Ready to view it?")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmShowSolution,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("confirm_show_solution_button")
                ) {
                    Text("Show Solution")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissSolutionDialog,
                    modifier = Modifier.testTag("cancel_show_solution_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProblemContent(
    question: Question,
    isSolutionVisible: Boolean,
    onToggleSolutionVisibility: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = question.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (question.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        question.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Schema & Sample Tables
        Text(
            text = "Schema & Sample Data",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        question.tables.forEach { table ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Table: ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = table.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                val columns = table.columns.map { it.name }
                val rows = table.rows.map { row -> row.map { it.toSqlValue() } }
                TableView(
                    columns = columns,
                    rows = rows,
                    testTagPrefix = "sample_table_${table.name}"
                )
            }
        }

        // Expected Output Table
        Text(
            text = "Expected Output",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        val expResult = question.expectedOutput.toQueryResult()
        TableView(
            columns = expResult.columns,
            rows = expResult.rows,
            testTagPrefix = "expected_output_table"
        )

        // Solution Section
        Spacer(modifier = Modifier.height(8.dp))
        if (!isSolutionVisible) {
            OutlinedButton(
                onClick = onToggleSolutionVisibility,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("show_solution_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Show Solution",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Show Solution")
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revealed_solution_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Solution Query",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        TextButton(
                            onClick = onToggleSolutionVisibility,
                            modifier = Modifier.testTag("hide_solution_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hide Solution",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hide", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val formattedSolution = remember(question.solutionQuery) {
                        SqlFormatter.format(question.solutionQuery)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = formattedSolution,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        )
                    }

                    if (question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Explanation",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditorContent(
    uiState: QuestionDetailUiState,
    onEditorValueChanged: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onDismissExecutionStatus: () -> Unit,
    onNavigateToNextQuestion: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFormatSql: () -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SQL Editor Box with floating speed dial pinned at bottom-right of the editor viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            SqlEditor(
                value = uiState.editorValue,
                onValueChange = onEditorValueChanged,
                modifier = Modifier.fillMaxSize()
            )

            // Right down corner round settings symbol button & popup menu - always constant!
            EditorSpeedDial(
                question = uiState.question,
                currentCode = uiState.editorValue.text,
                onClearQuery = onClearQuery,
                onFormatSql = onFormatSql,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Context-aware SQL Keywords & Table Columns Suggestions with Tab button and Undo/Redo
        SqlSuggestionBar(
            question = uiState.question,
            editorValue = uiState.editorValue,
            onValueChange = onEditorValueChanged,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            onUndo = onUndo,
            onRedo = onRedo,
            modifier = Modifier.fillMaxWidth()
        )

        // Execution Verdict Banner
        when (val status = uiState.executionStatus) {
            ExecutionStatus.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Press \"Run\" in the top bar to execute your query.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            ExecutionStatus.Running -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Executing query in SQLite sandbox...")
                    }
                }
            }

            is ExecutionStatus.SuccessCorrect -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("correct_verdict_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = SolvedGreen.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SolvedGreen.copy(alpha = 0.4f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SolvedGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Correct! Solved.",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SolvedGreen
                                    )
                                )
                                Text(
                                    text = "Execution time: ${status.result.executionTimeMs}ms · ${status.result.rows.size} rows returned",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        if (uiState.nextQuestionId != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigateToNextQuestion(uiState.nextQuestionId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("next_question_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SolvedGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Next Question")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            is ExecutionStatus.SuccessIncorrect -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incorrect_verdict_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = DifficultyMedium.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DifficultyMedium.copy(alpha = 0.4f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(DifficultyMedium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Not quite",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DifficultyMedium
                                        )
                                    )
                                    Text(
                                        text = "Your result: ${status.actualResult.rows.size} rows · ${status.actualResult.columns.size} cols / " +
                                            "Expected: ${status.expectedResult.rows.size} rows · ${status.expectedResult.columns.size} cols",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissExecutionStatus,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = DifficultyMedium,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            is ExecutionStatus.SqlError -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sql_error_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorRedContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SQL Error",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ErrorRed
                                    )
                                )
                            }
                            IconButton(
                                onClick = onDismissExecutionStatus,
                                modifier = Modifier.size(24.dp).testTag("dismiss_sql_error_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = ErrorRed
                            )
                        )
                    }
                }
            }
        }

        // Output Result Table (if query executed successfully)
        val queryResult = when (val s = uiState.executionStatus) {
            is ExecutionStatus.SuccessCorrect -> s.result
            is ExecutionStatus.SuccessIncorrect -> s.actualResult
            else -> null
        }

        if (queryResult != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Query Results",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${queryResult.rows.size} rows · ${queryResult.executionTimeMs}ms",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                TableView(
                    columns = queryResult.columns,
                    rows = queryResult.rows,
                    testTagPrefix = "user_query_result_table"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
