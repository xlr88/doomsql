package com.manish.doomsql.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.doomsql.data.engine.StreakCalculator
import com.manish.doomsql.data.local.entity.DailyActivityEntity
import com.manish.doomsql.data.local.entity.QuestionProgressEntity
import com.manish.doomsql.data.model.Difficulty
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.ui.theme.DifficultyEasy
import com.manish.doomsql.ui.theme.DifficultyHard
import com.manish.doomsql.ui.theme.DifficultyMedium
import com.manish.doomsql.ui.theme.SolvedGreen
import java.time.LocalDate

@Composable
fun HomeScreen(
    questions: List<Question>,
    progressMap: Map<String, QuestionProgressEntity>,
    dailyActivities: List<DailyActivityEntity>,
    weeklyGoal: Int,
    onNavigateToQuestion: (String) -> Unit,
    onNavigateToQuestionsList: () -> Unit,
    onNavigateToQuestionsFiltered: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = questions.size
    val solvedCount = questions.count { progressMap[it.id]?.isSolved == true }
    val progressFraction = if (totalCount > 0) solvedCount.toFloat() / totalCount.toFloat() else 0f

    // Calculate streaks from dates
    val today = LocalDate.now()
    val activeDates = dailyActivities
        .filter { it.queriesRun >= 1 }
        .mapNotNull {
            try {
                LocalDate.parse(it.date)
            } catch (e: Exception) {
                null
            }
        }
        .toSet()

    val currentStreak = StreakCalculator.calculateCurrentStreak(activeDates, today)

    // Calculate solved this week (Week starts Monday)
    val monday = StreakCalculator.getMondayOfWeek(today)
    val sunday = StreakCalculator.getSundayOfWeek(today)
    val solvedThisWeek = dailyActivities
        .filter {
            try {
                val d = LocalDate.parse(it.date)
                !d.isBefore(monday) && !d.isAfter(sunday)
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { it.questionsSolved }

    val weekGoalProgress = if (weeklyGoal > 0) {
        (solvedThisWeek.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Target question for "Continue Practicing":
    // Most recent unsolved attempted question; fallback to first unsolved question, or first question
    val attemptedUnsolved = questions
        .filter { q ->
            val p = progressMap[q.id]
            p != null && p.lastAttemptedAt != null && !p.isSolved
        }
        .maxByOrNull { progressMap[it.id]?.lastAttemptedAt ?: 0L }

    val targetQuestion = attemptedUnsolved
        ?: questions.firstOrNull { progressMap[it.id]?.isSolved != true }
        ?: questions.firstOrNull()

    val easyTotal = questions.count { it.difficulty == Difficulty.EASY }
    val easySolved = questions.count { it.difficulty == Difficulty.EASY && progressMap[it.id]?.isSolved == true }

    val medTotal = questions.count { it.difficulty == Difficulty.MEDIUM }
    val medSolved = questions.count { it.difficulty == Difficulty.MEDIUM && progressMap[it.id]?.isSolved == true }

    val hardTotal = questions.count { it.difficulty == Difficulty.HARD }
    val hardSolved = questions.count { it.difficulty == Difficulty.HARD && progressMap[it.id]?.isSolved == true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Streak & Solved Count
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_hero_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ">_",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DoomSQL",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            Text(
                                text = "Offline SQL Interview Practice",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // Streak Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_streak_pill")
                    ) {
                        Text(
                            text = "🔥 $currentStreak Day Streak",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Overall Solved Metric
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Questions Solved",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = "Solved $solvedCount / $totalCount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "This Week" Progress Bar (solved this week / weekly goal)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Week",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "$solvedThisWeek / $weeklyGoal Goal",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { weekGoalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SolvedGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Continue Practicing Card (most recent unsolved attempted question)
        if (targetQuestion != null) {
            val isTargetSolved = progressMap[targetQuestion.id]?.isSolved == true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigateToQuestion(targetQuestion.id) }
                    .testTag("continue_practicing_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTargetSolved) "Review Practice" else "Continue Practicing",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        DifficultyBadge(difficulty = targetQuestion.difficulty)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = targetQuestion.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = targetQuestion.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onNavigateToQuestion(targetQuestion.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_practicing_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTargetSolved) "Practice Again" else "Solve Question")
                    }
                }
            }
        }

        // Difficulty Buttons that open Questions pre-filtered
        Text(
            text = "Practice by Difficulty",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DifficultyFilterCard(
                difficulty = Difficulty.EASY,
                color = DifficultyEasy,
                solved = easySolved,
                total = easyTotal,
                onClick = { onNavigateToQuestionsFiltered(Difficulty.EASY) },
                modifier = Modifier.weight(1f)
            )
            DifficultyFilterCard(
                difficulty = Difficulty.MEDIUM,
                color = DifficultyMedium,
                solved = medSolved,
                total = medTotal,
                onClick = { onNavigateToQuestionsFiltered(Difficulty.MEDIUM) },
                modifier = Modifier.weight(1f)
            )
            DifficultyFilterCard(
                difficulty = Difficulty.HARD,
                color = DifficultyHard,
                solved = hardSolved,
                total = hardTotal,
                onClick = { onNavigateToQuestionsFiltered(Difficulty.HARD) },
                modifier = Modifier.weight(1f)
            )
        }

        // Action: Explore All Questions
        OutlinedButton(
            onClick = onNavigateToQuestionsList,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("browse_all_questions_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Browse All $totalCount Questions")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DifficultyFilterCard(
    difficulty: Difficulty,
    color: Color,
    solved: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                1.dp,
                color.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("home_difficulty_${difficulty.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$solved / $total",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: Difficulty) {
    val (bgColor, textColor, label) = when (difficulty) {
        Difficulty.EASY -> Triple(DifficultyEasy.copy(alpha = 0.15f), DifficultyEasy, "EASY")
        Difficulty.MEDIUM -> Triple(DifficultyMedium.copy(alpha = 0.15f), DifficultyMedium, "MEDIUM")
        Difficulty.HARD -> Triple(DifficultyHard.copy(alpha = 0.15f), DifficultyHard, "HARD")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}
