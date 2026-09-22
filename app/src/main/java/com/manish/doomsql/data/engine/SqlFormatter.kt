package com.manish.doomsql.data.engine

object SqlFormatter {

    private val CLAUSE_KEYWORDS = listOf(
        "SELECT",
        "FROM",
        "WHERE",
        "GROUP BY",
        "HAVING",
        "ORDER BY",
        "LIMIT",
        "OFFSET",
        "UNION ALL",
        "UNION",
        "WITH",
        "INSERT INTO",
        "VALUES",
        "UPDATE",
        "SET",
        "DELETE FROM"
    )

    private val JOIN_KEYWORDS = listOf(
        "LEFT OUTER JOIN",
        "RIGHT OUTER JOIN",
        "FULL OUTER JOIN",
        "INNER JOIN",
        "CROSS JOIN",
        "LEFT JOIN",
        "RIGHT JOIN",
        "FULL JOIN",
        "JOIN"
    )

    private val GENERAL_KEYWORDS = listOf(
        "DISTINCT", "AS", "ON", "AND", "OR", "NOT", "IN", "EXISTS",
        "BETWEEN", "LIKE", "IS NULL", "IS NOT NULL", "IS", "NULL",
        "CASE", "WHEN", "THEN", "ELSE", "END", "ASC", "DESC",
        "OVER", "PARTITION BY", "COUNT", "SUM", "AVG", "MIN", "MAX",
        "COALESCE", "ROUND", "STRFTIME", "CAST", "DENSE_RANK", "RANK", "ROW_NUMBER"
    )

    /**
     * Formats an SQL query into readable, line-by-line formatted SQL.
     * Keeps string literals intact, capitalizes standard SQL keywords,
     * and breaks clauses onto separate indented lines so users can read
     * line-by-line without horizontal scrolling.
     */
    fun format(rawSql: String): String {
        val trimmed = rawSql.trim()
        if (trimmed.isEmpty()) return ""

        // Preserve and extract comments and string literals
        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) return rawSql

        val formattedBuilder = StringBuilder()
        var currentClause = ""
        var parenDepth = 0

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            // Check if token matches a clause starter
            val matchedClause = findMultiTokenMatch(tokens, i, CLAUSE_KEYWORDS)
            val matchedJoin = findMultiTokenMatch(tokens, i, JOIN_KEYWORDS)

            if (matchedClause != null && parenDepth == 0) {
                if (formattedBuilder.isNotEmpty() && !formattedBuilder.endsWith("\n")) {
                    formattedBuilder.append("\n")
                }
                formattedBuilder.append(matchedClause.keyword)
                currentClause = matchedClause.keyword
                i += matchedClause.tokenCount

                if (i < tokens.size && tokens[i] != "\n") {
                    formattedBuilder.append("\n  ")
                }
                continue
            } else if (matchedJoin != null && parenDepth == 0) {
                if (formattedBuilder.isNotEmpty() && !formattedBuilder.endsWith("\n")) {
                    formattedBuilder.append("\n")
                }
                formattedBuilder.append(matchedJoin.keyword)
                formattedBuilder.append(" ")
                currentClause = matchedJoin.keyword
                i += matchedJoin.tokenCount
                continue
            }

            // Capitalize general keywords if not in string/comment
            val upperToken = token.uppercase()
            val isKeyword = isSqlKeyword(upperToken)

            when {
                token == "(" -> {
                    parenDepth++
                    formattedBuilder.append(token)
                }
                token == ")" -> {
                    if (parenDepth > 0) parenDepth--
                    formattedBuilder.append(token)
                }
                token == "," -> {
                    formattedBuilder.append(token)
                    if (parenDepth == 0 && (currentClause == "SELECT" || currentClause == "GROUP BY" || currentClause == "ORDER BY")) {
                        formattedBuilder.append("\n  ")
                    } else {
                        formattedBuilder.append(" ")
                    }
                }
                token.startsWith("--") || token.startsWith("/*") -> {
                    if (formattedBuilder.isNotEmpty() && !formattedBuilder.endsWith("\n")) {
                        formattedBuilder.append("\n")
                    }
                    formattedBuilder.append(token)
                    formattedBuilder.append("\n")
                }
                token.equals("AND", ignoreCase = true) || token.equals("OR", ignoreCase = true) -> {
                    if (parenDepth == 0 && (currentClause == "WHERE" || currentClause == "HAVING")) {
                        if (!formattedBuilder.endsWith("\n  ")) {
                            formattedBuilder.append("\n  ")
                        }
                    } else {
                        if (!formattedBuilder.endsWith(" ")) formattedBuilder.append(" ")
                    }
                    formattedBuilder.append(upperToken)
                    formattedBuilder.append(" ")
                }
                token.equals("ON", ignoreCase = true) -> {
                    if (!formattedBuilder.endsWith(" ")) formattedBuilder.append(" ")
                    formattedBuilder.append("ON ")
                }
                isKeyword -> {
                    if (formattedBuilder.isNotEmpty() && !formattedBuilder.endsWith(" ") && !formattedBuilder.endsWith("\n") && !formattedBuilder.endsWith("  ") && !formattedBuilder.endsWith("(")) {
                        formattedBuilder.append(" ")
                    }
                    formattedBuilder.append(upperToken)
                    if (i + 1 < tokens.size && tokens[i + 1] != "," && tokens[i + 1] != ")" && tokens[i + 1] != ";") {
                        formattedBuilder.append(" ")
                    }
                }
                else -> {
                    if (formattedBuilder.isNotEmpty() && !formattedBuilder.endsWith(" ") && !formattedBuilder.endsWith("\n") && !formattedBuilder.endsWith("  ") && !formattedBuilder.endsWith("(") && !formattedBuilder.endsWith(".")) {
                        formattedBuilder.append(" ")
                    }
                    formattedBuilder.append(token)
                }
            }

            i++
        }

        return cleanUpFormatting(formattedBuilder.toString())
    }

    private data class MatchResult(val keyword: String, val tokenCount: Int)

    private fun findMultiTokenMatch(tokens: List<String>, startIndex: Int, keywords: List<String>): MatchResult? {
        for (kw in keywords) {
            val kwParts = kw.split(" ")
            if (startIndex + kwParts.size <= tokens.size) {
                var matches = true
                for (j in kwParts.indices) {
                    if (!tokens[startIndex + j].equals(kwParts[j], ignoreCase = true)) {
                        matches = false
                        break
                    }
                }
                if (matches) {
                    return MatchResult(kw, kwParts.size)
                }
            }
        }
        return null
    }

    private fun isSqlKeyword(token: String): Boolean {
        return GENERAL_KEYWORDS.contains(token) ||
            CLAUSE_KEYWORDS.contains(token) ||
            JOIN_KEYWORDS.contains(token)
    }

    private fun tokenize(sql: String): List<String> {
        val result = mutableListOf<String>()
        val length = sql.length
        var i = 0

        while (i < length) {
            val c = sql[i]

            // Whitespace
            if (c.isWhitespace()) {
                i++
                continue
            }

            // Single line comment
            if (c == '-' && i + 1 < length && sql[i + 1] == '-') {
                val end = sql.indexOf('\n', i)
                if (end != -1) {
                    result.add(sql.substring(i, end).trim())
                    i = end + 1
                } else {
                    result.add(sql.substring(i).trim())
                    break
                }
                continue
            }

            // String literal '...'
            if (c == '\'') {
                var end = i + 1
                while (end < length) {
                    if (sql[end] == '\'') {
                        if (end + 1 < length && sql[end + 1] == '\'') {
                            end += 2 // Escaped quote
                        } else {
                            end++
                            break
                        }
                    } else {
                        end++
                    }
                }
                result.add(sql.substring(i, end.coerceAtMost(length)))
                i = end
                continue
            }

            // Symbols
            if (c == '(' || c == ')' || c == ',' || c == ';' || c == '.') {
                result.add(c.toString())
                i++
                continue
            }

            // Comparison / Arithmetic operators
            if (c == '=' || c == '<' || c == '>' || c == '!' || c == '+' || c == '-' || c == '*' || c == '/') {
                var end = i + 1
                if (end < length && (sql[end] == '=' || sql[end] == '>')) {
                    end++
                }
                result.add(sql.substring(i, end))
                i = end
                continue
            }

            // Identifiers / Keywords / Numbers
            var end = i
            while (end < length && !sql[end].isWhitespace() && "(),;.<>!=+-*/'".indexOf(sql[end]) == -1) {
                end++
            }
            if (end > i) {
                result.add(sql.substring(i, end))
                i = end
            } else {
                i++
            }
        }

        return result
    }

    private fun cleanUpFormatting(sql: String): String {
        return sql.lines()
            .map { it.trimEnd() }
            .filterIndexed { index, line ->
                // Remove consecutive blank lines
                line.isNotEmpty() || index == 0
            }
            .joinToString("\n")
            .trim()
    }
}
