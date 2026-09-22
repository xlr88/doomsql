package com.manish.doomsql.ui.screens.settings

import android.app.Activity
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
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.manish.doomsql.BuildConfig
import com.manish.doomsql.config.AppLinks
import com.manish.doomsql.ui.theme.ErrorRed
import com.manish.doomsql.ui.theme.SolvedGreen
import com.manish.doomsql.util.SupportHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onResetAllProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
            // Offline Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_offline_card"),
                colors = CardDefaults.cardColors(
                    containerColor = SolvedGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SolvedGreen.copy(alpha = 0.3f))
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SolvedGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "100% Offline Architecture",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SolvedGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No internet permissions, no external APIs, no analytics. All SQL executes in a local sandboxed engine.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Engine Specification Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Execution Engine & Sandbox",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    EngineDetailRow("Driver", "androidx.sqlite:sqlite-bundled (BundledSQLiteDriver)")
                    EngineDetailRow("Dialect", "SQLite 3.46 (Supports Window Functions & RIGHT JOIN)")
                    EngineDetailRow("Sandbox", "Ephemeral fresh :memory: instance per execution")
                    EngineDetailRow("Read-Only Mode", "PRAGMA query_only = ON")
                    EngineDetailRow("Row Limit", "Max 1,000 rows fetched per query")
                    EngineDetailRow("Timeout", "3,000ms execution guard")
                }
            }

            // App Information Card (with 2-tap privacy policy link)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Application Info",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    EngineDetailRow("App Name", "DoomSQL")
                    EngineDetailRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    EngineDetailRow("Package", context.packageName)
                    EngineDetailRow("Database", "Room Database 2.7.0")

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            SupportHelper.openCustomTabOrBrowser(context, AppLinks.PRIVACY_POLICY_URL) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Unable to open browser")
                                }
                            }
                        },
                        modifier = Modifier.testTag("about_privacy_policy_link")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = "Open Privacy Policy",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy Policy", style = MaterialTheme.typography.labelMedium)
                    }
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

            // About & Support Section
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
                            val activity = context as? Activity
                            if (activity != null) {
                                SupportHelper.requestInAppReview(activity) {
                                    SupportHelper.openPlayStoreListing(context)
                                }
                            } else {
                                SupportHelper.openPlayStoreListing(context)
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 3. Report a bug / Contact support
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

                    // 4. Privacy Policy
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

                    // 5. Terms of Service
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

                    // 6. App version (non-clickable, shows versionName)
                    SupportNavigationRow(
                        label = "App version",
                        icon = Icons.Default.Info,
                        contentDescription = "App version ${BuildConfig.VERSION_NAME}",
                        testTag = "settings_row_version",
                        trailingText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        isClickable = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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

@Composable
private fun EngineDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
