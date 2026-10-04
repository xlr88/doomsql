package com.chaduvukondi.firstu

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.chaduvukondi.firstu.data.local.entity.DailyActivityEntity
import com.chaduvukondi.firstu.data.local.entity.QuestionProgressEntity
import com.chaduvukondi.firstu.data.model.Question
import com.chaduvukondi.firstu.ui.navigation.BottomNavItems
import com.chaduvukondi.firstu.ui.navigation.Screen
import com.chaduvukondi.firstu.ui.screens.welcome.WelcomeScreen
import com.chaduvukondi.firstu.ui.screens.welcome.WelcomeViewModel
import com.chaduvukondi.firstu.ui.screens.detail.QuestionDetailScreen
import com.chaduvukondi.firstu.ui.screens.detail.QuestionDetailViewModel
import com.chaduvukondi.firstu.ui.screens.home.HomeScreen
import com.chaduvukondi.firstu.ui.screens.progress.ProgressScreen
import com.chaduvukondi.firstu.ui.screens.questions.QuestionsListScreen
import com.chaduvukondi.firstu.ui.screens.questions.QuestionsViewModel
import com.chaduvukondi.firstu.ui.screens.settings.SettingsScreen
import com.chaduvukondi.firstu.ui.theme.DoomSqlTheme
import com.chaduvukondi.firstu.util.SupportHelper
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
fun DoomSqlApp(appContainer: com.chaduvukondi.firstu.di.AppContainer) {
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

    val allQuestions by appContainer.repository.questionsFlow.collectAsState()
    var isAppReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appContainer.repository.getQuestions()
        isAppReady = true
        // Check for new questions in background if connected
        launch {
            appContainer.repository.syncQuestions(isManual = false)
        }
    }

    // Auto-sync when device connects to the internet
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                coroutineScope.launch {
                    appContainer.repository.syncQuestions(isManual = false)
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {}

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {}
        }
    }

    val progressMap by appContainer.repository.progressMapFlow.collectAsState(initial = emptyMap())
    val dailyActivities by appContainer.repository.dailyActivitiesFlow.collectAsState(initial = emptyList())
    val weeklyGoal by appContainer.userPreferences.weeklyGoalFlow.collectAsState(initial = 10)
    val hasCompletedWelcome by appContainer.userPreferences.hasCompletedWelcomeFlow.collectAsState(initial = null)
    val lastOpenedId = remember(progressMap) { appContainer.repository.getLastOpenedQuestionId() }

    if (!isAppReady || hasCompletedWelcome == null) {
        AppOpenSplashScreen()
        return
    }

    // Critical: Remember initial start destination so NavHost does not rebuild its graph
    // when hasCompletedWelcome changes from false to true in DataStore.
    val initialStartDestination = remember {
        if (hasCompletedWelcome == true) Screen.Home.route else Screen.Welcome.route
    }

    NavHost(
        navController = navController,
        startDestination = initialStartDestination,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        // Main Tabs Screen with HorizontalPager supporting swipe and tap navigation
        composable(Screen.Home.route) {
            MainTabsScreen(
                appContainer = appContainer,
                allQuestions = allQuestions,
                progressMap = progressMap,
                dailyActivities = dailyActivities,
                weeklyGoal = weeklyGoal,
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
                        popUpTo(Screen.Home.route)
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

        // Direct routes for BottomNav items (in case of deep link / direct call)
        composable(
            route = Screen.Questions.route,
            arguments = listOf(navArgument("difficulty") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            MainTabsScreen(
                appContainer = appContainer,
                allQuestions = allQuestions,
                progressMap = progressMap,
                dailyActivities = dailyActivities,
                weeklyGoal = weeklyGoal,
                initialPageIndex = 1,
                onNavigateToQuestion = { id ->
                    navController.navigate(Screen.QuestionDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Progress.route) {
            MainTabsScreen(
                appContainer = appContainer,
                allQuestions = allQuestions,
                progressMap = progressMap,
                dailyActivities = dailyActivities,
                weeklyGoal = weeklyGoal,
                initialPageIndex = 2,
                onNavigateToQuestion = { id ->
                    navController.navigate(Screen.QuestionDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Settings.route) {
            MainTabsScreen(
                appContainer = appContainer,
                allQuestions = allQuestions,
                progressMap = progressMap,
                dailyActivities = dailyActivities,
                weeklyGoal = weeklyGoal,
                initialPageIndex = 3,
                onNavigateToQuestion = { id ->
                    navController.navigate(Screen.QuestionDetail.createRoute(id))
                }
            )
        }

        // Welcome Screen (shown on first launch with Google / Offline choices)
        composable(Screen.Welcome.route) {
            val welcomeViewModel: WelcomeViewModel = viewModel {
                WelcomeViewModel(
                    authRepository = appContainer.authRepository,
                    userPreferences = appContainer.userPreferences
                )
            }
            val welcomeUiState by welcomeViewModel.uiState.collectAsState()

            WelcomeScreen(
                uiState = welcomeUiState,
                onContinueWithGoogle = { ctx ->
                    welcomeViewModel.continueWithGoogle(ctx) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onPracticeOffline = {
                    welcomeViewModel.practiceOffline {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    welcomeViewModel.skip {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onDismissError = welcomeViewModel::onDismissError
            )
        }
    }
}

@Composable
fun MainTabsScreen(
    appContainer: com.chaduvukondi.firstu.di.AppContainer,
    allQuestions: List<Question>,
    progressMap: Map<String, QuestionProgressEntity>,
    dailyActivities: List<DailyActivityEntity>,
    weeklyGoal: Int,
    initialPageIndex: Int = 0,
    onNavigateToQuestion: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPageIndex) { BottomNavItems.size }
    val coroutineScope = rememberCoroutineScope()

    val questionsViewModel: QuestionsViewModel = viewModel {
        QuestionsViewModel(
            repository = appContainer.repository,
            userPreferences = appContainer.userPreferences,
            initialDifficulty = null
        )
    }

    // Android back button: return to Home tab if on Questions, Progress, or Settings
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                BottomNavItems.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = {
                            screen.icon?.let { icon ->
                                Icon(imageVector = icon, contentDescription = screen.title)
                            }
                        },
                        label = { Text(screen.title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.testTag("nav_item_${screen.title.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> {
                    HomeScreen(
                        questions = allQuestions,
                        progressMap = progressMap,
                        dailyActivities = dailyActivities,
                        weeklyGoal = weeklyGoal,
                        onNavigateToQuestion = onNavigateToQuestion,
                        onNavigateToQuestionsList = {
                            questionsViewModel.onDifficultyFilterSelected(null)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        onNavigateToQuestionsFiltered = { difficulty ->
                            questionsViewModel.onDifficultyFilterSelected(difficulty)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                }
                1 -> {
                    val uiState by questionsViewModel.uiState.collectAsState()
                    QuestionsListScreen(
                        uiState = uiState,
                        onSearchQueryChanged = questionsViewModel::onSearchQueryChanged,
                        onDifficultyFilterSelected = questionsViewModel::onDifficultyFilterSelected,
                        onStatusFilterSelected = questionsViewModel::onStatusFilterSelected,
                        onDismissBanner = questionsViewModel::onDismissBanner,
                        onNavigateToQuestion = onNavigateToQuestion
                    )
                }
                2 -> {
                    ProgressScreen(
                        questions = allQuestions,
                        progressMap = progressMap,
                        dailyActivities = dailyActivities,
                        weeklyGoal = weeklyGoal
                    )
                }
                3 -> {
                    SettingsScreen(
                        authRepository = appContainer.authRepository,
                        questionRepository = appContainer.repository,
                        userPreferences = appContainer.userPreferences,
                        billingManager = appContainer.billingManager,
                        onResetAllProgress = {
                            coroutineScope.launch {
                                appContainer.repository.resetAllProgress()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppOpenSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B101D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = ">_",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00D2FF)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DoomSQL",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Interactive SQL Sandbox",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            )
        }
    }
}
