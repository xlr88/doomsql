package com.manish.doomsql.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Questions : Screen("questions?difficulty={difficulty}", "Questions", Icons.Default.List) {
        fun createRoute(difficulty: com.manish.doomsql.data.model.Difficulty? = null) =
            if (difficulty != null) "questions?difficulty=${difficulty.name}" else "questions"
    }
    data object Progress : Screen("progress", "Progress", Icons.Default.CheckCircle)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    data object QuestionDetail : Screen("question_detail/{questionId}", "Question") {
        fun createRoute(questionId: String) = "question_detail/$questionId"
    }
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Questions,
    Screen.Progress,
    Screen.Settings
)
