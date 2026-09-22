package com.manish.doomsql.ui.screens.questions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.doomsql.data.model.Difficulty
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.ui.screens.home.DifficultyBadge
import com.manish.doomsql.ui.theme.SolvedGreen

@Composable
fun QuestionsListScreen(
    uiState: QuestionsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onDifficultyFilterSelected: (Difficulty?) -> Unit,
    onStatusFilterSelected: (StatusFilter) -> Unit,
    onNavigateToQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("questions_search_input"),
            placeholder = { Text("Search by title, description, or tag...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips row (Horizontally scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty Chips
            FilterChip(
                selected = uiState.difficultyFilter == null,
                onClick = { onDifficultyFilterSelected(null) },
                label = { Text("All Difficulties") },
                modifier = Modifier.testTag("filter_diff_all")
            )
            FilterChip(
                selected = uiState.difficultyFilter == Difficulty.EASY,
                onClick = {
                    onDifficultyFilterSelected(
                        if (uiState.difficultyFilter == Difficulty.EASY) null else Difficulty.EASY
                    )
                },
                label = { Text("Easy") },
                modifier = Modifier.testTag("filter_diff_easy")
            )
            FilterChip(
                selected = uiState.difficultyFilter == Difficulty.MEDIUM,
                onClick = {
                    onDifficultyFilterSelected(
                        if (uiState.difficultyFilter == Difficulty.MEDIUM) null else Difficulty.MEDIUM
                    )
                },
                label = { Text("Medium") },
                modifier = Modifier.testTag("filter_diff_medium")
            )
            FilterChip(
                selected = uiState.difficultyFilter == Difficulty.HARD,
                onClick = {
                    onDifficultyFilterSelected(
                        if (uiState.difficultyFilter == Difficulty.HARD) null else Difficulty.HARD
                    )
                },
                label = { Text("Hard") },
                modifier = Modifier.testTag("filter_diff_hard")
            )

            // Status separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            // Status Chips
            FilterChip(
                selected = uiState.statusFilter == StatusFilter.ALL,
                onClick = { onStatusFilterSelected(StatusFilter.ALL) },
                label = { Text("All Status") },
                modifier = Modifier.testTag("filter_status_all")
            )
            FilterChip(
                selected = uiState.statusFilter == StatusFilter.SOLVED,
                onClick = { onStatusFilterSelected(StatusFilter.SOLVED) },
                label = { Text("Solved") },
                modifier = Modifier.testTag("filter_status_solved")
            )
            FilterChip(
                selected = uiState.statusFilter == StatusFilter.UNSOLVED,
                onClick = { onStatusFilterSelected(StatusFilter.UNSOLVED) },
                label = { Text("Unsolved") },
                modifier = Modifier.testTag("filter_status_unsolved")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Questions List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("questions_loading"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredQuestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("questions_empty"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No questions match your filter.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search or clear filters.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("questions_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.filteredQuestions,
                    key = { question -> question.id }
                ) { question ->
                    val isSolved = uiState.progressMap[question.id]?.isSolved == true
                    QuestionItemCard(
                        question = question,
                        isSolved = isSolved,
                        onClick = { onNavigateToQuestion(question.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuestionItemCard(
    question: Question,
    isSolved: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSolved) SolvedGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("question_card_${question.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSolved) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Solved status icon: ✓ or ○
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSolved) SolvedGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.dp,
                        if (isSolved) SolvedGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSolved) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Solved",
                        tint = SolvedGreen,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = question.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Difficulty Badge
            DifficultyBadge(difficulty = question.difficulty)
        }
    }
}
