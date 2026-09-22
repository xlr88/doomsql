package com.manish.doomsql.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.doomsql.data.model.Question

enum class SuggestionType {
    COLUMN,
    QUALIFIED_COLUMN,
    TABLE,
    KEYWORD
}

data class SqlSuggestion(
    val displayText: String,
    val insertText: String,
    val type: SuggestionType,
    val tableHint: String? = null
)

object SqlSuggestionHelper {

    val SQL_KEYWORDS = listOf(
        "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT",
        "JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "CROSS JOIN",
        "ON", "AS", "AND", "OR", "NOT", "IN",
        "NULL", "NOTNULL", "NULLIF", "IS NULL", "IS NOT NULL",
        "COALESCE()", "LIKE", "BETWEEN", "DISTINCT", "EXISTS",
        "CASE", "WHEN", "THEN", "ELSE", "END",
        "COUNT(*)", "COUNT()", "SUM()", "AVG()", "MIN()", "MAX()", "ROUND()",
        "UNION", "UNION ALL", "ASC", "DESC",
        "OVER()", "PARTITION BY", "ROW_NUMBER()", "DENSE_RANK()", "RANK()",
        "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM"
    )

    fun extractPrefix(text: String, cursor: Int): Pair<String, IntRange> {
        val safeCursor = cursor.coerceIn(0, text.length)
        if (safeCursor == 0) return Pair("", IntRange.EMPTY)

        var start = safeCursor
        while (start > 0) {
            val ch = text[start - 1]
            if (ch.isLetterOrDigit() || ch == '_' || ch == '.') {
                start--
            } else {
                break
            }
        }
        val prefix = text.substring(start, safeCursor)
        return Pair(prefix, start until safeCursor)
    }

    fun buildSuggestions(question: Question?, currentPrefix: String): List<SqlSuggestion> {
        val allSuggestions = mutableListOf<SqlSuggestion>()

        // 1. Add table columns and qualified columns
        question?.tables?.forEach { table ->
            // Table name
            allSuggestions.add(
                SqlSuggestion(
                    displayText = table.name,
                    insertText = table.name,
                    type = SuggestionType.TABLE
                )
            )

            // Columns
            table.columns.forEach { col ->
                allSuggestions.add(
                    SqlSuggestion(
                        displayText = col.name,
                        insertText = col.name,
                        type = SuggestionType.COLUMN,
                        tableHint = table.name
                    )
                )
                allSuggestions.add(
                    SqlSuggestion(
                        displayText = "${table.name}.${col.name}",
                        insertText = "${table.name}.${col.name}",
                        type = SuggestionType.QUALIFIED_COLUMN,
                        tableHint = table.name
                    )
                )
            }
        }

        // 2. Add SQL Keywords
        SQL_KEYWORDS.forEach { kw ->
            allSuggestions.add(
                SqlSuggestion(
                    displayText = kw,
                    insertText = kw,
                    type = SuggestionType.KEYWORD
                )
            )
        }

        val trimmedPrefix = currentPrefix.trim()
        if (trimmedPrefix.isEmpty()) {
            // When no prefix is typed, return common defaults:
            // Table columns first, then common SQL clauses
            val defaultColumns = allSuggestions.filter { it.type == SuggestionType.COLUMN || it.type == SuggestionType.TABLE }
            val defaultKeywords = allSuggestions.filter {
                it.type == SuggestionType.KEYWORD && (
                    it.insertText in listOf("SELECT", "FROM", "WHERE", "JOIN", "GROUP BY", "ORDER BY", "AND", "OR", "LIMIT", "COUNT(*)")
                )
            }
            return (defaultColumns + defaultKeywords).distinctBy { it.displayText }
        }

        val lowerPrefix = trimmedPrefix.lowercase()

        // Partition by exact prefix matches first, then contains matches
        val startsWithMatches = allSuggestions.filter { suggestion ->
            when (suggestion.type) {
                SuggestionType.QUALIFIED_COLUMN -> {
                    suggestion.displayText.lowercase().startsWith(lowerPrefix) ||
                        suggestion.displayText.substringAfter(".").lowercase().startsWith(lowerPrefix)
                }
                else -> suggestion.displayText.lowercase().startsWith(lowerPrefix)
            }
        }

        val containsMatches = allSuggestions.filter { suggestion ->
            !startsWithMatches.contains(suggestion) &&
                suggestion.displayText.lowercase().contains(lowerPrefix)
        }

        // Sort startsWithMatches: Column names first, qualified column names next, keywords, then tables
        val sortedStartsWith = startsWithMatches.sortedWith(
            compareBy(
                {
                    when (it.type) {
                        SuggestionType.COLUMN -> 0
                        SuggestionType.QUALIFIED_COLUMN -> 1
                        SuggestionType.KEYWORD -> 2
                        SuggestionType.TABLE -> 3
                    }
                },
                { it.displayText.length }
            )
        )

        val combined = (sortedStartsWith + containsMatches).distinctBy { it.displayText }
        return combined.take(25)
    }

    fun applySuggestion(
        currentValue: TextFieldValue,
        suggestion: SqlSuggestion
    ): TextFieldValue {
        val text = currentValue.text
        val cursor = currentValue.selection.end.coerceIn(0, text.length)
        val (_, prefixRange) = extractPrefix(text, cursor)

        val insertText = suggestion.insertText
        val (replacement, cursorAdvance) = if (insertText.endsWith("()")) {
            // Place cursor inside parentheses e.g. COUNT(|)
            Pair(insertText, insertText.length - 1)
        } else {
            // Add trailing space for ease of next input
            Pair("$insertText ", insertText.length + 1)
        }

        val start = if (prefixRange.isEmpty()) cursor else prefixRange.first
        val end = if (prefixRange.isEmpty()) cursor else prefixRange.last + 1

        val newText = text.substring(0, start) + replacement + text.substring(end)
        val newCursor = (start + cursorAdvance).coerceIn(0, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    fun insertTab(currentValue: TextFieldValue): TextFieldValue {
        val text = currentValue.text
        val sel = currentValue.selection
        val start = sel.start.coerceIn(0, text.length)
        val end = sel.end.coerceIn(0, text.length)

        val indent = "  " // 2 spaces standard for SQL
        val newText = text.substring(0, start) + indent + text.substring(end)
        val newCursor = (start + indent.length).coerceIn(0, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }
}

@Composable
fun SqlSuggestionBar(
    question: Question?,
    editorValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cursor = editorValue.selection.end
    val (prefix, _) = remember(editorValue.text, cursor) {
        SqlSuggestionHelper.extractPrefix(editorValue.text, cursor)
    }

    val suggestions by remember(question, prefix) {
        derivedStateOf {
            SqlSuggestionHelper.buildSuggestions(question, prefix)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sql_suggestion_bar"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pinned Tab Button - always visible and accessible
            Surface(
                onClick = {
                    onValueChange(SqlSuggestionHelper.insertTab(editorValue))
                },
                modifier = Modifier
                    .height(34.dp)
                    .testTag("suggestion_tab_button"),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardTab,
                        contentDescription = "Tab key",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Tab",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Undo & Redo buttons
            Surface(
                onClick = { if (canUndo) onUndo() },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("suggestion_undo_button"),
                shape = RoundedCornerShape(6.dp),
                color = if (canUndo) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (canUndo) 0.3f else 0.15f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(16.dp),
                        tint = if (canUndo) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(3.dp))

            Surface(
                onClick = { if (canRedo) onRedo() },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("suggestion_redo_button"),
                shape = RoundedCornerShape(6.dp),
                color = if (canRedo) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (canRedo) 0.3f else 0.15f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        modifier = Modifier.size(16.dp),
                        tint = if (canRedo) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Horizontally Scrollable Suggestions Row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (suggestions.isEmpty()) {
                    Text(
                        text = "No suggestions",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                } else {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            suggestion = suggestion,
                            onClick = {
                                onValueChange(
                                    SqlSuggestionHelper.applySuggestion(editorValue, suggestion)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    suggestion: SqlSuggestion,
    onClick: () -> Unit
) {
    val containerColor = when (suggestion.type) {
        SuggestionType.COLUMN, SuggestionType.QUALIFIED_COLUMN ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        SuggestionType.TABLE ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        SuggestionType.KEYWORD ->
            MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when (suggestion.type) {
        SuggestionType.COLUMN, SuggestionType.QUALIFIED_COLUMN ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        SuggestionType.TABLE ->
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        SuggestionType.KEYWORD ->
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    val textColor = when (suggestion.type) {
        SuggestionType.COLUMN, SuggestionType.QUALIFIED_COLUMN ->
            MaterialTheme.colorScheme.onPrimaryContainer
        SuggestionType.TABLE ->
            MaterialTheme.colorScheme.onTertiaryContainer
        SuggestionType.KEYWORD ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(34.dp)
            .testTag("suggestion_${suggestion.displayText}"),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (suggestion.type) {
                SuggestionType.COLUMN -> {
                    Text(
                        text = "col",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                SuggestionType.QUALIFIED_COLUMN -> {
                    Text(
                        text = suggestion.tableHint ?: "col",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                SuggestionType.TABLE -> {
                    Text(
                        text = "tbl",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                SuggestionType.KEYWORD -> {
                    // No prefix badge needed for SQL keywords
                }
            }

            Text(
                text = suggestion.displayText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = if (suggestion.type == SuggestionType.KEYWORD) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            )
        }
    }
}
