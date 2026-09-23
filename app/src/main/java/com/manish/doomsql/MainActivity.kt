package com.manish.doomsql

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.ui.navigation.BottomNavItems
import com.manish.doomsql.ui.navigation.Screen
import com.manish.doomsql.ui.screens.auth.SignInScreen
import com.manish.doomsql.ui.screens.auth.SignInViewModel
import com.manish.doomsql.ui.screens.detail.QuestionDetailScreen
import com.manish.doomsql.ui.screens.detail.QuestionDetailViewModel
import com.manish.doomsql.ui.screens.home.HomeScreen
import com.manish.doomsql.ui.screens.progress.ProgressScreen
import com.manish.doomsql.ui.screens.questions.QuestionsListScreen
import com.manish.doomsql.ui.screens.questions.QuestionsViewModel
import com.manish.doomsql.ui.screens.settings.SettingsScreen
import com.manish.doomsql.ui.theme.DoomSqlTheme
import com.manish.doomsql.util.SupportHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as DoomSqlApplication).container

        setContent {
            DoomSqlTheme {
                DoomSqlApp(appContainer = appContainer)
            }
        }
    }
}

@Composable
fun DoomSqlApp(appContainer: com.manish.doomsql.di.AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    fun checkInAppReviewTrigger() {
        if (activity != null) {
            coroutineScope.launch {
                val hasPrompted = appContainer.userPreferences.hasPromptedReviewFlow.first()
                if (!hasPrompted) {
                    val solvedCount = appContainer.repository.getSolvedCount()
                    if (solvedCount >= 5) {
                        appContainer.userPreferences.markReviewPrompted()
                        SupportHelper.requestInAppReview(activity)
                    }
                }
            }
        }
    }

    var allQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    LaunchedEffect(Unit) {
        allQuestions = appContainer.repository.getQuestions()
    }

    val progressMap by appContainer.repository.progressMapFlow.collectAsState(initial = emptyMap())
    val dailyActivities by appContainer.repository.dailyActivitiesFlow.collectAsState(initial = emptyList())
    val weeklyGoal by appContainer.userPreferences.weeklyGoalFlow.collectAsState(initial = 10)
    val lastOpenedId = remember(progressMap) { appContainer.repository.getLastOpenedQuestionId() }

    val showBottomBar = currentRoute in BottomNavItems.map { it.route } || currentRoute?.startsWith("questions") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    BottomNavItems.forEach { screen ->
                        val isSelected = when (screen) {
                            Screen.Questions -> currentRoute?.startsWith("questions") == true
                            else -> currentRoute == screen.route
                        }
                        val targetRoute = if (screen == Screen.Questions) "questions" else screen.route

                        NavigationBarItem(
                            icon = {
                                screen.icon?.let { icon ->
                                    Icon(imageVector = icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.title.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    questions = allQuestions,
                    progressMap = progressMap,
                    dailyActivities = dailyActivities,
                    weeklyGoal = weeklyGoal,
                    onNavigateToQuestion = { id ->
                        navController.navigate(Screen.QuestionDetail.createRoute(id))
                    },
                    onNavigateToQuestionsList = {
                        navController.navigate("questions") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToQuestionsFiltered = { difficulty ->
                        navController.navigate(Screen.Questions.createRoute(difficulty)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Questions Screen
            composable(
                route = Screen.Questions.route,
                arguments = listOf(navArgument("difficulty") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val diffStr = backStackEntry.arguments?.getString("difficulty")
                val initialDifficulty = diffStr?.let {
                    try {
                        com.manish.doomsql.data.model.Difficulty.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }

                val questionsViewModel = viewModel(key = "questions_$diffStr") {
                    QuestionsViewModel(
                        repository = appContainer.repository,
                        initialDifficulty = initialDifficulty
                    )
                }
                val uiState by questionsViewModel.uiState.collectAsState()

                QuestionsListScreen(
                    uiState = uiState,
                    onSearchQueryChanged = questionsViewModel::onSearchQueryChanged,
                    onDifficultyFilterSelected = questionsViewModel::onDifficultyFilterSelected,
                    onStatusFilterSelected = questionsViewModel::onStatusFilterSelected,
                    onNavigateToQuestion = { id ->
                        navController.navigate(Screen.QuestionDetail.createRoute(id))
                    }
                )
            }

            // Question Detail Screen
            composable(
                route = Screen.QuestionDetail.route,
                arguments = listOf(navArgument("questionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
                val detailViewModel = viewModel(
                    key = "detail_$questionId"
                ) {
                    QuestionDetailViewModel(
                        questionId = questionId,
                        repository = appContainer.repository,
                        sqlEngine = appContainer.sqlEngine
                    )
                }
                val uiState by detailViewModel.uiState.collectAsState()

                QuestionDetailScreen(
                    uiState = uiState,
                    onNavigateBack = {
                        checkInAppReviewTrigger()
                        navController.popBackStack()
                    },
                    onNavigateToNextQuestion = { nextId ->
                        checkInAppReviewTrigger()
                        navController.navigate(Screen.QuestionDetail.createRoute(nextId)) {
                            popUpTo(Screen.Questions.route)
                        }
                    },
                    onTabSelected = detailViewModel::onTabSelected,
                    onEditorValueChanged = detailViewModel::onEditorValueChanged,
                    onDismissExecutionStatus = detailViewModel::onDismissExecutionStatus,
                    onRunQuery = detailViewModel::onRunQuery,
                    onToggleSolutionVisibility = detailViewModel::onToggleSolutionVisibility,
                    onDismissSolutionDialog = detailViewModel::onDismissSolutionDialog,
                    onConfirmShowSolution = detailViewModel::onConfirmShowSolution,
                    onClearEditor = detailViewModel::onClearEditor,
                    onFormatEditor = detailViewModel::onFormatEditor,
                    onDisposeSaveDraft = detailViewModel::saveDraftImmediately,
                    onUndo = detailViewModel::onUndo,
                    onRedo = detailViewModel::onRedo
                )
            }

            // Progress Screen
            composable(Screen.Progress.route) {
                ProgressScreen(
                    questions = allQuestions,
                    progressMap = progressMap,
                    dailyActivities = dailyActivities,
                    weeklyGoal = weeklyGoal
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    authRepository = appContainer.authRepository,
                    onNavigateToSignIn = {
                        navController.navigate(Screen.SignIn.route)
                    },
                    onResetAllProgress = {
                        coroutineScope.launch {
                            appContainer.repository.resetAllProgress()
                        }
                    }
                )
            }

            // Optional Sign-In Screen
            composable(Screen.SignIn.route) {
                val signInViewModel: SignInViewModel = viewModel {
                    SignInViewModel(authRepository = appContainer.authRepository)
                }
                val signInUiState by signInViewModel.uiState.collectAsState()

                SignInScreen(
                    uiState = signInUiState,
                    onEmailChanged = signInViewModel::onEmailChanged,
                    onPasswordChanged = signInViewModel::onPasswordChanged,
                    onConfirmPasswordChanged = signInViewModel::onConfirmPasswordChanged,
                    onToggleMode = signInViewModel::toggleMode,
                    onSignInWithGoogle = { ctx, onSuccess ->
                        signInViewModel.signInWithGoogle(ctx, onSuccess)
                    },
                    onSubmitEmailAuth = { onSuccess ->
                        signInViewModel.submitEmailAuth(onSuccess)
                    },
                    onOpenForgotPasswordDialog = signInViewModel::openForgotPasswordDialog,
                    onDismissForgotPasswordDialog = signInViewModel::dismissForgotPasswordDialog,
                    onForgotPasswordEmailChanged = signInViewModel::onForgotPasswordEmailChanged,
                    onSubmitForgotPassword = signInViewModel::submitForgotPassword,
                    onDismissVerificationDialog = { onSuccess ->
                        signInViewModel.dismissVerificationNoticeDialog(onSuccess)
                    },
                    onDismissError = signInViewModel::onDismissError,
                    onDismissSuccess = signInViewModel::onDismissSuccess,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
