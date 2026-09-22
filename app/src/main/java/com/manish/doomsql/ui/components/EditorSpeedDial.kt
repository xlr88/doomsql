package com.manish.doomsql.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.model.toSqlValue

@Composable
fun EditorSpeedDial(
    question: Question?,
    currentCode: String,
    onClearQuery: () -> Unit,
    onFormatSql: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pop-up menu items
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(spring()) + slideInVertically(spring()) { it / 2 },
            exit = fadeOut(spring()) + slideOutVertically(spring()) { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. AI Helper Button (Robot emoji / SmartToy icon)
                SpeedDialItem(
                    label = "Copy for AI 🤖",
                    icon = Icons.Default.SmartToy,
                    testTag = "speed_dial_ai",
                    onClick = {
                        expanded = false
                        if (question != null) {
                            val prompt = buildAiPrompt(question, currentCode)
                            clipboardManager.setText(AnnotatedString(prompt))
                            Toast.makeText(
                                context,
                                "AI prompt copied to clipboard! Ready to paste into ChatGPT, Claude, etc.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                // 2. Clear Query Button (Dustbin symbol)
                SpeedDialItem(
                    label = "Clear Query",
                    icon = Icons.Default.DeleteOutline,
                    testTag = "speed_dial_clear",
                    onClick = {
                        expanded = false
                        onClearQuery()
                        Toast.makeText(context, "Query cleared", Toast.LENGTH_SHORT).show()
                    }
                )

                // 3. Format SQL Button (Justify symbol)
                SpeedDialItem(
                    label = "Format SQL",
                    icon = Icons.Default.FormatAlignJustify,
                    testTag = "speed_dial_format",
                    onClick = {
                        expanded = false
                        onFormatSql()
                        Toast.makeText(context, "SQL formatted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Main Toggle Button (Round settings symbol when collapsed, cross mark when expanded)
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .size(46.dp)
                .testTag("editor_settings_fab"),
            shape = CircleShape,
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (expanded) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = if (expanded) "Close Tools" else "Editor Tools",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Label pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
            shadowElevation = if (enabled) 2.dp else 0.dp,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f)
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        SmallFloatingActionButton(
            onClick = { if (enabled) onClick() },
            modifier = Modifier
                .size(40.dp)
                .testTag(testTag),
            shape = CircleShape,
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            },
            contentColor = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            },
            elevation = FloatingActionButtonDefaults.elevation(if (enabled) 3.dp else 0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Builds the structured clipboard prompt for external AI chatbots.
 */
private fun buildAiPrompt(question: Question, currentCode: String): String {
    val cleanCode = currentCode.trim()
    val isDefaultTemplate = cleanCode.isBlank() ||
        (cleanCode.startsWith("--") && cleanCode.lines().size <= 2 && cleanCode.contains("SELECT"))

    val codeText = if (isDefaultTemplate) {
        "no code return yet"
    } else {
        cleanCode
    }

    val schemaText = buildString {
        question.tables.forEach { table ->
            appendLine("Table: ${table.name}")
            appendLine("Columns: ${table.columns.joinToString(", ") { "${it.name} (${it.type})" }}")
            if (table.rows.isNotEmpty()) {
                appendLine("Sample data:")
                appendLine(table.columns.joinToString(" | ") { it.name })
                table.rows.take(3).forEach { row ->
                    appendLine(row.joinToString(" | ") { it.toSqlValue().toString() })
                }
            }
            appendLine()
        }
    }.trim()

    val expOutputText = buildString {
        val exp = question.expectedOutput.toQueryResult()
        appendLine("Columns: ${exp.columns.joinToString(", ")}")
        appendLine("Expected sample rows:")
        exp.rows.take(3).forEach { row ->
            appendLine(row.joinToString(" | "))
        }
    }.trim()

    return """
Question: ${question.title}
Difficulty: ${question.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}
Description:
${question.description}

Schema & Tables:
$schemaText

Expected Output:
$expOutputText

My current code:
$codeText

I need guidance and help to solve this question. Could you please help me to understand what I am missing or how to approach this problem.
""".trimIndent()
}
