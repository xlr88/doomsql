package com.manish.doomsql.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.manish.doomsql.data.billing.BillingManager
import com.manish.doomsql.data.billing.BillingPurchaseEvent
import com.manish.doomsql.data.repository.AuthRepository
import com.manish.doomsql.data.repository.AuthResult
import com.manish.doomsql.data.repository.QuestionRepository
import com.manish.doomsql.data.repository.UserPreferencesRepository
import com.manish.doomsql.data.remote.SyncResult
import com.manish.doomsql.BuildConfig
import com.manish.doomsql.config.AppLinks
import com.manish.doomsql.ui.theme.ErrorRed
import com.manish.doomsql.ui.theme.SolvedGreen
import com.manish.doomsql.util.SupportHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    authRepository: AuthRepository,
    onResetAllProgress: () -> Unit,
    modifier: Modifier = Modifier,
    questionRepository: QuestionRepository? = null,
    userPreferences: UserPreferencesRepository? = null,
    billingManager: BillingManager? = null,
    onNavigateToSignIn: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by authRepository.currentUser.collectAsState()
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var isCheckingSync by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }
    val lastSyncAt by (userPreferences?.lastSyncAtFlow ?: kotlinx.coroutines.flow.flowOf(0L)).collectAsState(initial = 0L)
    val manifestVersion by (userPreferences?.manifestVersionFlow ?: kotlinx.coroutines.flow.flowOf(1)).collectAsState(initial = 1)
    var showResetDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var alsoResetLocalOnDelete by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(5) }
    var showBillingInfoDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val isSupporter by (userPreferences?.isSupporterFlow ?: kotlinx.coroutines.flow.flowOf(false)).collectAsState(initial = false)
    val tipsCount by (userPreferences?.tipsCountFlow ?: kotlinx.coroutines.flow.flowOf(0)).collectAsState(initial = 0)
    val productDetailsMap by (billingManager?.productDetailsMap ?: kotlinx.coroutines.flow.MutableStateFlow(emptyMap())).collectAsState()

    LaunchedEffect(billingManager) {
        billingManager?.purchaseEvent?.collect { event ->
            when (event) {
                is BillingPurchaseEvent.Success -> {
                    snackbarHostState.showSnackbar("🎉 Thank you so much for supporting DoomSQL!")
                }
                is BillingPurchaseEvent.Pending -> {
                    snackbarHostState.showSnackbar("⏳ UPI Payment pending. Will complete once approved in your UPI app.")
                }
                is BillingPurchaseEvent.Error -> {
                    snackbarHostState.showSnackbar("Payment: ${event.message}")
                }
                is BillingPurchaseEvent.Cancelled -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== ACCOUNT SECTION ====================
            if (currentUser == null) {
                // Signed Out State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_account_signed_out_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Account & Cloud Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Sync your progress across all your devices.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (!isGoogleSigningIn) {
                                    coroutineScope.launch {
                                        isGoogleSigningIn = true
                                        val res = authRepository.signInWithGoogle(context)
                                        isGoogleSigningIn = false
                                        when (res) {
                                            is AuthResult.Success -> {
                                                val name = res.data.displayName ?: res.data.email ?: "Google account"
                                                snackbarHostState.showSnackbar("Signed in as $name")
                                            }
                                            is AuthResult.Error -> {
                                                if (!res.message.contains("cancelled", ignoreCase = true)) {
                                                    snackbarHostState.showSnackbar(res.message)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isGoogleSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sign_in_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isGoogleSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Continue with Google")
                            }
                        }
                    }
                }
            } else {
                // Signed In State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_account_signed_in_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val photoUrl = currentUser?.photoUrl
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "Google User",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!currentUser?.email.isNullOrBlank()) {
                                    Text(
                                        text = currentUser?.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        authRepository.signOut()
                                        snackbarHostState.showSnackbar("Signed out successfully")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_sign_out_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }

                            OutlinedButton(
                                onClick = {
                                    alsoResetLocalOnDelete = false
                                    showDeleteAccountDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ErrorRed
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(ErrorRed.copy(alpha = 0.5f))
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_delete_account_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Account", color = ErrorRed)
                            }
                        }
                    }
                }
            }

            // ==================== SUPPORT THE DEVELOPER SECTION (2ND) ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("settings_support_developer_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF4081),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Support the Developer",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isSupporter) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SolvedGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SolvedGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (tipsCount > 1) "⭐ Supporter ($tipsCount)" else "⭐ Supporter",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SolvedGreen
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "DoomSQL is 100% free and ad-free. If you enjoy practicing SQL offline, consider leaving a small tip to fuel new questions and maintenance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tip Tiers Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BillingManager.TIP_TIERS.chunked(2).forEach { rowTiers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowTiers.forEach { tier ->
                                    val productDetails = productDetailsMap[tier.productId]
                                    val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                                        ?: tier.fallbackPrice

                                    OutlinedCard(
                                        onClick = {
                                            val activity = context as? Activity
                                            if (activity != null && billingManager != null) {
                                                billingManager.launchTipFlow(
                                                    activity = activity,
                                                    productId = tier.productId,
                                                    onNotConfiguredInPlayConsole = {
                                                        showBillingInfoDialog = true
                                                    }
                                                )
                                            } else {
                                                showBillingInfoDialog = true
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("tip_tier_${tier.productId}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tier.emoji,
                                                    fontSize = 20.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = price,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = tier.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = tier.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 14.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Policy & Payment Info notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ Official Google Play Billing: Supports UPI (GPay, PhonePe, Paytm), NetBanking & Cards. 100% compliant with Google Play policies.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // ==================== ABOUT & SUPPORT SECTION (3RD) ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("settings_about_support_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About & Support",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Share DoomSQL
                    SupportNavigationRow(
                        label = "Share DoomSQL",
                        icon = Icons.Default.Share,
                        contentDescription = "Share DoomSQL with friends",
                        testTag = "settings_row_share",
                        onClick = {
                            SupportHelper.shareApp(context) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Unable to share link")
                                }
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 2. Rate DoomSQL
                    SupportNavigationRow(
                        label = "Rate DoomSQL",
                        icon = Icons.Default.Star,
                        contentDescription = "Rate DoomSQL on Google Play",
                        testTag = "settings_row_rate",
                        onClick = {
                            showRateDialog = true
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 3. Tip & Support Developer
                    SupportNavigationRow(
                        label = "Tip & Support Developer",
                        icon = Icons.Default.Favorite,
                        contentDescription = "Support DoomSQL development via Google Play Billing",
                        testTag = "settings_row_support_developer",
                        trailingText = if (isSupporter) "⭐ Supporter" else "UPI / Play",
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(200)
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 4. Report a bug / Contact support
                    SupportNavigationRow(
                        label = "Report a bug / Contact support",
                        icon = Icons.Default.BugReport,
                        contentDescription = "Report a bug or contact support team",
                        testTag = "settings_row_support",
                        onClick = {
                            showContactDialog = true
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 5. Privacy Policy
                    SupportNavigationRow(
                        label = "Privacy Policy",
                        icon = Icons.Default.Policy,
                        contentDescription = "Read Privacy Policy",
                        testTag = "settings_row_privacy",
                        onClick = {
                            SupportHelper.openCustomTabOrBrowser(context, AppLinks.PRIVACY_POLICY_URL) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Unable to open browser")
                                }
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 6. Terms of Service
                    SupportNavigationRow(
                        label = "Terms of Service",
                        icon = Icons.Default.Description,
                        contentDescription = "Read Terms of Service",
                        testTag = "settings_row_terms",
                        onClick = {
                            SupportHelper.openCustomTabOrBrowser(context, AppLinks.TERMS_URL) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Unable to open browser")
                                }
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 7. App version (non-clickable, shows versionName)
                    SupportNavigationRow(
                        label = "App version",
                        icon = Icons.Default.Info,
                        contentDescription = "App version ${BuildConfig.VERSION_NAME}",
                        testTag = "settings_row_version",
                        trailingText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        isClickable = false
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 8. Check for new questions
                    SupportNavigationRow(
                        label = "Check for new questions",
                        icon = Icons.Default.Sync,
                        contentDescription = "Check for new questions online",
                        testTag = "settings_row_check_questions",
                        trailingText = if (isCheckingSync) "Checking…" else syncStatusText,
                        isClickable = !isCheckingSync,
                        onClick = {
                            if (!isCheckingSync && questionRepository != null) {
                                coroutineScope.launch {
                                    isCheckingSync = true
                                    syncStatusText = "Checking…"
                                    val res = questionRepository.syncQuestions(isManual = true)
                                    isCheckingSync = false
                                    syncStatusText = when (res) {
                                        is SyncResult.Success -> {
                                            if (res.newCount > 0) "Added ${res.newCount} new questions"
                                            else "Updated ${res.updatedCount} questions"
                                        }
                                        is SyncResult.UpToDate -> "You're up to date"
                                        is SyncResult.NoInternet -> "No internet connection"
                                        is SyncResult.Error -> "You're up to date"
                                        is SyncResult.Skipped -> "You're up to date"
                                    }
                                }
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 9. Question pack version
                    SupportNavigationRow(
                        label = "Question pack version",
                        icon = Icons.Default.Description,
                        contentDescription = "Question pack version $manifestVersion",
                        testTag = "settings_row_pack_version",
                        trailingText = "v$manifestVersion",
                        isClickable = false
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 10. Last checked
                    SupportNavigationRow(
                        label = "Last checked",
                        icon = Icons.Default.HelpOutline,
                        contentDescription = "Last checked ${formatTimeAgo(lastSyncAt)}",
                        testTag = "settings_row_last_checked",
                        trailingText = formatTimeAgo(lastSyncAt),
                        isClickable = false
                    )
                }
            }

            // Danger Zone: Reset All Progress
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        ErrorRed.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Clears all solved questions, saved SQL drafts, and historical activity stats.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_progress_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Practice Progress")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Rate DoomSQL In-Place & Play Store Dialog
    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Rate DoomSQL",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enjoying your SQL interview practice? How would you rate your experience?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 5 Star Rating Selector
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        for (star in 1..5) {
                            val isSelected = star <= selectedRating
                            IconButton(
                                onClick = { selectedRating = star },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rate_star_$star")
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.Star,
                                    contentDescription = "$star stars",
                                    tint = if (isSelected) Color(0xFFFFB800) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Rating description text
                    val ratingLabel = when (selectedRating) {
                        5 -> "Loved it! ⭐⭐⭐⭐⭐"
                        4 -> "Great experience! ⭐⭐⭐⭐"
                        3 -> "It's good ⭐⭐⭐"
                        2 -> "Needs improvement ⭐⭐"
                        else -> "Not satisfied ⭐"
                    }
                    Text(
                        text = ratingLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedRating >= 4) SolvedGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (selectedRating >= 4) {
                        Button(
                            onClick = {
                                showRateDialog = false
                                SupportHelper.openPlayStoreListing(context) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unable to open Google Play Store")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("rate_dialog_submit_play_store"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rate on Google Play")
                        }
                    } else {
                        Button(
                            onClick = {
                                showRateDialog = false
                                showContactDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("rate_dialog_send_feedback"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Us Feedback")
                        }

                        OutlinedButton(
                            onClick = {
                                showRateDialog = false
                                SupportHelper.openPlayStoreListing(context) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unable to open Google Play Store")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("rate_dialog_rate_anyway"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Rate on Play Store anyway")
                        }
                    }

                    TextButton(
                        onClick = {
                            showRateDialog = false
                            SupportHelper.openPlayStoreListing(context) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Unable to open Google Play Store")
                                }
                            }
                        },
                        modifier = Modifier.testTag("rate_dialog_direct_play_store")
                    ) {
                        Text(
                            text = "Or open Google Play directly",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showRateDialog = false },
                    modifier = Modifier.testTag("rate_dialog_dismiss")
                ) {
                    Text("Maybe Later")
                }
            }
        )
    }

    // Google Play In-App Billing Info & Setup Dialog
    if (showBillingInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBillingInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Google Play In-App Billing",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DoomSQL uses official Google Play In-App Billing (consumable purchases) so users can tip and support the developer legally without violating Google Play policies.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• In India, Google Play natively accepts UPI apps (Google Pay, PhonePe, Paytm, BHIM, CRED), NetBanking, and Cards directly in the Google Play bottom sheet.\n• When users pay, Google processes the payment and credits the amount directly into your linked bank account via your Google Play Console Merchant Profile.\n• To enable live purchases: in Google Play Console, go to Monetize > In-app products and create the products: tip_small, tip_medium, tip_large, tip_hero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBillingInfoDialog = false },
                    modifier = Modifier.testTag("billing_info_ok_button")
                ) {
                    Text("Got it")
                }
            }
        )
    }

    // Reset Progress Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed
                )
            },
            title = { Text("Reset All Progress?") },
            text = {
                Text("This action will permanently delete all your question solve records, saved SQL drafts, and activity statistics. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetAllProgress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Yes, Reset Everything")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    modifier = Modifier.testTag("cancel_reset_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report a bug / Contact support Dialog
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Report or Feedback") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "How can we help you today?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = {
                            showContactDialog = false
                            SupportHelper.sendSupportEmail(
                                context = context,
                                subject = "DoomSQL Bug Report",
                                body = SupportHelper.buildEmailBody(),
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("support_dialog_report_bug"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Report a bug",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report a bug")
                    }

                    OutlinedButton(
                        onClick = {
                            showContactDialog = false
                            SupportHelper.sendSupportEmail(
                                context = context,
                                subject = "DoomSQL Feedback",
                                body = SupportHelper.buildEmailBody(),
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("support_dialog_general_feedback"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = "General feedback",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("General feedback")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showContactDialog = false },
                    modifier = Modifier.testTag("support_dialog_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation Dialog (Google Play Requirement)
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) showDeleteAccountDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed
                )
            },
            title = {
                Text(
                    text = "Delete Account?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete your DoomSQL account? This action removes your cloud user credentials and cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alsoResetLocalOnDelete = !alsoResetLocalOnDelete }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = alsoResetLocalOnDelete,
                            onCheckedChange = { alsoResetLocalOnDelete = it },
                            modifier = Modifier.testTag("delete_account_reset_local_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Also erase progress on this device",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "In compliance with Google Play policy, you can also delete your account via the web at: ${AppLinks.ACCOUNT_DELETION_URL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDeletingAccount = true
                            val res = authRepository.deleteAccount(
                                context = context,
                                alsoResetLocalProgress = alsoResetLocalOnDelete,
                                onResetLocalProgress = onResetAllProgress
                            )
                            isDeletingAccount = false
                            when (res) {
                                is AuthResult.Success -> {
                                    showDeleteAccountDialog = false
                                    snackbarHostState.showSnackbar("Account successfully deleted.")
                                }
                                is AuthResult.Error -> {
                                    snackbarHostState.showSnackbar(res.message)
                                }
                            }
                        }
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete Forever")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount,
                    modifier = Modifier.testTag("cancel_delete_account_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupportNavigationRow(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    trailingText: String? = null,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    val clickableModifier = if (isClickable) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = label,
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(clickableModifier)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            )
        } else if (isClickable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr${if (hours > 1) "s" else ""} ago"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}
