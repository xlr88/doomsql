package com.manish.doomsql.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.doomsql.data.model.SqlValue

@Composable
fun TableView(
    columns: List<String>,
    rows: List<List<SqlValue>>,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "table"
) {
    val scrollState = rememberScrollState()
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val rowAltBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val regularBg = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .horizontalScroll(scrollState)
            .testTag(testTagPrefix)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(headerBg)
                    .border(width = 0.5.dp, color = borderColor)
            ) {
                columns.forEachIndexed { index, colName ->
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 88.dp)
                            .widthIn(min = 88.dp)
                            .border(width = 0.5.dp, color = borderColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = colName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Data Rows
            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "0 rows returned",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                rows.forEachIndexed { rowIndex, row ->
                    val bg = if (rowIndex % 2 == 1) rowAltBg else regularBg
                    Row(
                        modifier = Modifier
                            .background(bg)
                            .border(width = 0.5.dp, color = borderColor)
                    ) {
                        row.forEachIndexed { colIndex, cellValue ->
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 88.dp)
                                    .widthIn(min = 88.dp)
                                    .border(width = 0.5.dp, color = borderColor)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CellValueText(cellValue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CellValueText(value: SqlValue) {
    when (value) {
        is SqlValue.Null -> {
            Text(
                text = "NULL",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium
                )
            )
        }
        is SqlValue.Integer -> {
            Text(
                text = value.value.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        is SqlValue.Real -> {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        is SqlValue.Text -> {
            Text(
                text = value.value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
