package com.manish.doomsql.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SqlEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val editorBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val gutterBg = if (isDarkTheme) Color(0xFF0B1120) else Color(0xFFEDF2F7)
    val gutterText = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
    val defaultTextColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val lineCount = value.text.count { it == '\n' } + 1
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(editorBg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .testTag("sql_editor_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // Line Numbers Gutter
            Column(
                modifier = Modifier
                    .background(gutterBg)
                    .border(width = 0.5.dp, color = borderColor)
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 56.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (line in 1..lineCount) {
                    Text(
                        text = line.toString(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = gutterText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Editor Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(top = 12.dp, start = 4.dp, bottom = 56.dp, end = 48.dp)
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        text = "-- Type your SQL query here...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = gutterText
                        )
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = { newVal ->
                        val adjusted = handleAutoIndent(value, newVal)
                        onValueChange(adjusted)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sql_editor_input"),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = defaultTextColor
                    ),
                    cursorBrush = SolidColor(if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Ascii
                    ),
                    visualTransformation = SqlSyntaxVisualTransformation(isDarkTheme)
                )
            }
        }
    }
}

// Preserve indentation of the previous line when Enter is pressed
private fun handleAutoIndent(oldVal: TextFieldValue, newVal: TextFieldValue): TextFieldValue {
    val oldText = oldVal.text
    val newText = newVal.text
    val sel = newVal.selection

    // Detect if Enter was pressed (1 char added and it is '\n')
    if (newText.length == oldText.length + 1 && sel.start > 0 && newText[sel.start - 1] == '\n') {
        val newlinePos = sel.start - 1
        val lastLineStart = newText.lastIndexOf('\n', newlinePos - 1).let {
            if (it == -1) 0 else it + 1
        }
        val prevLine = newText.substring(lastLineStart, newlinePos)
        val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
        if (indent.isNotEmpty()) {
            val adjustedText = newText.substring(0, newlinePos + 1) + indent + newText.substring(newlinePos + 1)
            val newCursor = newlinePos + 1 + indent.length
            return TextFieldValue(adjustedText, TextRange(newCursor))
        }
    }
    return newVal
}

class SqlSyntaxVisualTransformation(private val isDark: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(highlightSql(text.text, isDark), OffsetMapping.Identity)
    }
}

private val SQL_KEYWORDS = setOf(
    "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "CROSS", "ON",
    "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "AS", "WITH", "DISTINCT", "UNION",
    "ALL", "CASE", "WHEN", "THEN", "ELSE", "END", "AND", "OR", "NOT", "IN", "IS", "NULL",
    "LIKE", "BETWEEN", "EXISTS", "OVER", "PARTITION", "VALUES", "INSERT", "INTO", "UPDATE",
    "DELETE", "CREATE", "TABLE", "DROP", "ALTER", "CAST", "COUNT", "SUM", "AVG", "MIN", "MAX",
    "ROW_NUMBER", "RANK", "DENSE_RANK", "LAG", "LEAD", "COALESCE", "SUBSTR", "ROUND", "DATE",
    "TIME", "DATETIME", "STRFTIME", "ASC", "DESC"
)

fun highlightSql(text: String, isDark: Boolean): AnnotatedString {
    val keywordColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
    val stringColor = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
    val numberColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val commentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val defaultTextColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    return buildAnnotatedString {
        append(text)
        if (text.isNotEmpty()) {
            addStyle(SpanStyle(color = defaultTextColor), 0, text.length)
        }
        var i = 0
        val n = text.length

        while (i < n) {
            val c = text[i]
            val nextC = if (i + 1 < n) text[i + 1] else null

            // Single line comment: -- ...
            if (c == '-' && nextC == '-') {
                val start = i
                val end = text.indexOf('\n', start).let { if (it == -1) n else it }
                addStyle(SpanStyle(color = commentColor, fontWeight = FontWeight.Normal), start, end)
                i = end
                continue
            }

            // Block comment: /* ... */
            if (c == '/' && nextC == '*') {
                val start = i
                val end = text.indexOf("*/", start + 2).let { if (it == -1) n else it + 2 }
                addStyle(SpanStyle(color = commentColor, fontWeight = FontWeight.Normal), start, end)
                i = end
                continue
            }

            // Single-quote string literal: '...'
            if (c == '\'') {
                val start = i
                i++
                while (i < n) {
                    if (text[i] == '\'') {
                        if (i + 1 < n && text[i + 1] == '\'') {
                            i += 2
                        } else {
                            i++
                            break
                        }
                    } else {
                        i++
                    }
                }
                addStyle(SpanStyle(color = stringColor, fontWeight = FontWeight.Normal), start, i)
                continue
            }

            // Number: digits
            if (c.isDigit()) {
                val start = i
                while (i < n && (text[i].isDigit() || text[i] == '.')) {
                    i++
                }
                addStyle(SpanStyle(color = numberColor, fontWeight = FontWeight.SemiBold), start, i)
                continue
            }

            // Word / Identifier
            if (c.isLetter() || c == '_') {
                val start = i
                while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) {
                    i++
                }
                val word = text.substring(start, i)
                if (SQL_KEYWORDS.contains(word.uppercase())) {
                    addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), start, i)
                }
                continue
            }

            i++
        }
    }
}
