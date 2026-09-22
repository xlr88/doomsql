package com.manish.doomsql.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ProgressScreen(
    questions: List<Question>,
    progressMap: Map<String, QuestionProgressEntity>,
    dailyActivities: List<DailyActivityEntity>,
    weeklyGoal: Int,
    modifier: Modifier = Modifier
) {
    val totalQuestions = questions.size
    val totalSolved = questions.count { progressMap[it.id]?.isSolved == true }

    val easyTotal = questions.count { it.difficulty == Difficulty.EASY }
    val easySolved = questions.count { it.difficulty == Difficulty.EASY && progressMap[it.id]?.isSolved == true }

    val medTotal = questions.count { it.difficulty == Difficulty.MEDIUM }
    val medSolved = questions.count { it.difficulty == Difficulty.MEDIUM && progressMap[it.id]?.isSolved == true }

    val hardTotal = questions.count { it.difficulty == Difficulty.HARD }
    val hardSolved = questions.count { it.difficulty == Difficulty.HARD && progressMap[it.id]?.isSolved == true }

    // Streak calculations from dates
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
    val longestStreak = StreakCalculator.calculateLongestStreak(activeDates)

    // Week metrics (Mon-Sun)
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

    val daysOfWeek = StreakCalculator.getDaysOfWeek(today)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card: Total Solved
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("progress_summary_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Overall Mastery",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$totalSolved / $totalQuestions",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "Questions Solved",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        val pct = if (totalQuestions > 0) (totalSolved * 100 / totalQuestions) else 0
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Streak Metrics (Current & Longest)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(14.dp)
                    )
                    .testTag("current_streak_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Streak",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔥 $currentStreak ${if (currentStreak == 1) "Day" else "Days"}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(14.dp)
                    )
                    .testTag("longest_streak_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Longest Streak",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🏆 $longestStreak ${if (longestStreak == 1) "Day" else "Days"}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // Active Days Mon–Sun Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(16.dp)
                )
                .testTag("week_active_days_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Weekly Activity (Mon – Sun)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Active day = at least one query run",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (date in daysOfWeek) {
                        val isActive = activeDates.contains(date)
                        val isToday = date == today
                        val dayLabel = when (date.dayOfWeek) {
                            DayOfWeek.MONDAY -> "Mon"
                            DayOfWeek.TUESDAY -> "Tue"
                            DayOfWeek.WEDNESDAY -> "Wed"
                            DayOfWeek.THURSDAY -> "Thu"
                            DayOfWeek.FRIDAY -> "Fri"
                            DayOfWeek.SATURDAY -> "Sat"
                            DayOfWeek.SUNDAY -> "Sun"
                            else -> ""
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> SolvedGreen
                                            isToday -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        }
                                    )
                                    .then(
                                        if (isToday) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active day",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = "·",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weekly Goal Progress Card (solved this week vs goal)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(16.dp)
                )
                .testTag("weekly_goal_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Goal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "$solvedThisWeek / $weeklyGoal Solved",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { weekGoalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = SolvedGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (solvedThisWeek >= weeklyGoal) {
                        "🎉 Weekly goal achieved!"
                    } else {
                        "${weeklyGoal - solvedThisWeek} more question${if (weeklyGoal - solvedThisWeek == 1) "" else "s"} to reach this week's goal"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Difficulty Breakdown Card: Solved per difficulty (Easy x/5, Medium x/4, Hard x/3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(16.dp)
                )
                .testTag("difficulty_breakdown_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Solved per Difficulty",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))

                DifficultyStatBar("Easy", DifficultyEasy, easySolved, easyTotal)
                Spacer(modifier = Modifier.height(12.dp))
                DifficultyStatBar("Medium", DifficultyMedium, medSolved, medTotal)
                Spacer(modifier = Modifier.height(12.dp))
                DifficultyStatBar("Hard", DifficultyHard, hardSolved, hardTotal)
            }
        }
    }
}

@Composable
private fun DifficultyStatBar(
    name: String,
    color: Color,
    solved: Int,
    total: Int
) {
    val fraction = if (total > 0) solved.toFloat() / total.toFloat() else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            )
            Text(
                text = "$solved / $total",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
