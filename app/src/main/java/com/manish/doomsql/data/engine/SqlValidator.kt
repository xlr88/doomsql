package com.manish.doomsql.data.engine

sealed interface SqlValidationResult {
    data class Valid(val sanitizedQuery: String) : SqlValidationResult
    data class Invalid(val reason: String) : SqlValidationResult
}

object SqlValidator {

    private val FORBIDDEN_KEYWORDS = setOf(
        "ATTACH",
        "DETACH",
        "PRAGMA",
        "VACUUM",
        "LOAD_EXTENSION"
    )

    private val ALLOWED_INITIAL_KEYWORDS = setOf(
        "SELECT",
        "WITH",
        "VALUES"
    )

    fun validate(rawQuery: String): SqlValidationResult {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            return SqlValidationResult.Invalid("Query cannot be empty.")
        }

        // Tokenize and clean query
        val cleanedBuilder = StringBuilder()
        var i = 0
        val n = trimmed.length
        var inSingleQuote = false
        var inDoubleQuote = false
        var inLineComment = false
        var inBlockComment = false

        val tokensOutsideQuotes = mutableListOf<String>()
        val currentToken = StringBuilder()
        var semicolonCount = 0
        var lastCharBeforeSemicolon = ' '

        while (i < n) {
            val c = trimmed[i]
            val nextC = if (i + 1 < n) trimmed[i + 1] else null

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false
                    cleanedBuilder.append(c)
                }
                i++
                continue
            }

            if (inBlockComment) {
                if (c == '*' && nextC == '/') {
                    inBlockComment = false
                    i += 2
                } else {
                    i++
                }
                continue
            }

            if (inSingleQuote) {
                cleanedBuilder.append(c)
                if (c == '\'') {
                    if (nextC == '\'') {
                        // Escaped single quote ''
                        cleanedBuilder.append('\'')
                        i += 2
                        continue
                    } else {
                        inSingleQuote = false
                    }
                }
                i++
                continue
            }

            if (inDoubleQuote) {
                cleanedBuilder.append(c)
                if (c == '"') {
                    inDoubleQuote = false
                }
                i++
                continue
            }

            // Outside any comment or quote
            if (c == '-' && nextC == '-') {
                inLineComment = true
                i += 2
                continue
            }

            if (c == '/' && nextC == '*') {
                inBlockComment = true
                i += 2
                continue
            }

            if (c == '\'') {
                inSingleQuote = true
                if (currentToken.isNotEmpty()) {
                    tokensOutsideQuotes.add(currentToken.toString())
                    currentToken.clear()
                }
                cleanedBuilder.append(c)
                i++
                continue
            }

            if (c == '"') {
                inDoubleQuote = true
                if (currentToken.isNotEmpty()) {
                    tokensOutsideQuotes.add(currentToken.toString())
                    currentToken.clear()
                }
                cleanedBuilder.append(c)
                i++
                continue
            }

            if (c == ';') {
                if (currentToken.isNotEmpty()) {
                    tokensOutsideQuotes.add(currentToken.toString())
                    currentToken.clear()
                }
                // Check if there is any non-whitespace/comment character remaining after this semicolon
                val remainingText = trimmed.substring(i + 1).trim()
                if (remainingText.isNotEmpty() && !isOnlyComments(remainingText)) {
                    return SqlValidationResult.Invalid("Multiple SQL statements are not permitted.")
                }
                semicolonCount++
                i++
                continue
            }

            // Accumulate alphanumeric tokens outside quotes
            if (c.isLetterOrDigit() || c == '_') {
                currentToken.append(c)
            } else {
                if (currentToken.isNotEmpty()) {
                    tokensOutsideQuotes.add(currentToken.toString())
                    currentToken.clear()
                }
            }

            cleanedBuilder.append(c)
            i++
        }

        if (currentToken.isNotEmpty()) {
            tokensOutsideQuotes.add(currentToken.toString())
        }

        if (inSingleQuote || inDoubleQuote) {
            return SqlValidationResult.Invalid("Unterminated string literal.")
        }
        if (inBlockComment) {
            return SqlValidationResult.Invalid("Unterminated block comment.")
        }

        val sanitized = cleanedBuilder.toString().trim()
        if (sanitized.isEmpty()) {
            return SqlValidationResult.Invalid("Query cannot be empty after removing comments.")
        }

        if (tokensOutsideQuotes.isEmpty()) {
            return SqlValidationResult.Invalid("No SQL statements found.")
        }

        // Check first keyword
        val firstKeyword = tokensOutsideQuotes.first().uppercase()
        if (!ALLOWED_INITIAL_KEYWORDS.contains(firstKeyword)) {
            return SqlValidationResult.Invalid(
                "Only SELECT, WITH, or VALUES queries are allowed. Found '$firstKeyword'."
            )
        }

        // Check forbidden keywords
        for (token in tokensOutsideQuotes) {
            val upper = token.uppercase()
            if (FORBIDDEN_KEYWORDS.contains(upper)) {
                return SqlValidationResult.Invalid("Forbidden keyword '$token' is not permitted in user queries.")
            }
        }

        return SqlValidationResult.Valid(sanitized)
    }

    private fun isOnlyComments(text: String): Boolean {
        var idx = 0
        val len = text.length
        while (idx < len) {
            val c = text[idx]
            val nextC = if (idx + 1 < len) text[idx + 1] else null
            if (c.isWhitespace()) {
                idx++
            } else if (c == '-' && nextC == '-') {
                val nextNewline = text.indexOf('\n', idx + 2)
                if (nextNewline == -1) return true
                idx = nextNewline + 1
            } else if (c == '/' && nextC == '*') {
                val endComment = text.indexOf("*/", idx + 2)
                if (endComment == -1) return true
                idx = endComment + 2
            } else {
                return false
            }
        }
        return true
    }
}
