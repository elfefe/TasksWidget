package com.elfefe.common.controller

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.elfefe.common.ui.view.TaskCardManager

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        try {
            val originalText = text.text

            // Tokens and positions
            val tokens = mutableListOf<Token>()
            var i = 0

            // Tokenization
            while (i < originalText.length) {
                // Check for markdown symbols
                if (originalText.startsWith("#", i) && isStartOfLine(originalText, i)) {
                    val headerMatch = Regex("^(#{1,6})\\s").find(originalText.substring(i))
                    if (headerMatch != null) {
                        val headerSymbol = headerMatch.value
                        val level = headerSymbol.trimEnd().count { it == '#' }
                        i += headerSymbol.length

                        // Read the rest of the line as header content
                        val startContent = i
                        while (i < originalText.length && originalText[i] != '\n') {
                            i++
                        }
                        val content = originalText.substring(startContent, i)

                        tokens.add(Token.Header(level, content))

                        if (i < originalText.length && originalText[i] == '\n') {
                            i++ // Include the newline
                        }
                        continue // Move to the next iteration
                    }
                } else {
                    val symbol = when {
                        originalText.startsWith("```", i) -> originalText.substring(i, i + 3)
                        originalText.startsWith("**", i) -> originalText.substring(i, i + 2)
                        originalText.startsWith("_", i) -> originalText.substring(i, i + 1)
                        originalText.startsWith("~~", i) -> originalText.substring(i, i + 2)
                        originalText.startsWith("==", i) -> originalText.substring(i, i + 2)
                        originalText.startsWith("---", i) -> originalText.substring(i, i + 3)
                        else -> null
                    }

                    if (symbol != null) {
                        tokens.add(Token.MarkdownSymbol(symbol, i))
                        i += symbol.length
                    } else {
                        // Accumulate text until the next markdown symbol
                        val start = i
                        while (i < originalText.length && !isMarkdownSymbolStart(originalText, i)) {
                            i++
                        }
                        val content = originalText.substring(start, i)
                        if (text.isNotBlank()) tokens.add(Token.Text(content))
                    }
                }
            }

            // Parsing tokens into style spans
            val openSymbols = mutableMapOf<String, MutableList<Int>>() // Symbol to stack of positions

            val markdownStyles = mapOf(
                "_" to SpanStyle(fontStyle = FontStyle.Italic),
                "**" to SpanStyle(fontWeight = FontWeight.Bold),
                "~~" to SpanStyle(textDecoration = TextDecoration.LineThrough),
                "`" to SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray),
                "```" to SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray),
                "==" to SpanStyle(background = Color.Yellow),
                // Headers will be handled separately
            )

            // For mapping positions
            val originalToTransformed = mutableListOf<Int>()
            val transformedToOriginal = mutableListOf<Int>()

            val annotatedStringBuilder = AnnotatedString.Builder()
            var originalIndex = 0
            var transformedIndex = 0

            var numberOfLines = 1 // For handling numbered lists

            var headerLevel: Int? = null
            var headerStartIndex: Int? = null

            for (token in tokens) {
                when (token) {
                    is Token.Text -> {
                        val content = token.text

                        // Process links within the text
                        val linkPattern = Regex("\\[(.+?)\\]\\((.+?)\\)")
                        var lastIndex = 0
                        linkPattern.findAll(content).forEach { matchResult ->
                            val range = matchResult.range
                            if (range.first > lastIndex) {
                                val plainText = content.substring(lastIndex, range.first)
                                annotatedStringBuilder.append(plainText)
                                // Map positions
                                for (j in plainText.indices) {
                                    originalToTransformed.add(transformedIndex)
                                    transformedToOriginal.add(originalIndex)
                                    originalIndex++
                                    transformedIndex++
                                }
                            }

                            val displayText = matchResult.groupValues[1]
                            val url = matchResult.groupValues[2]
                            val start = transformedIndex
                            annotatedStringBuilder.append(displayText)
                            transformedIndex += displayText.length

                            // Apply link style
                            annotatedStringBuilder.addStyle(
                                SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                                start,
                                transformedIndex
                            )

                            // Optionally, add annotation for the URL
                            annotatedStringBuilder.addStringAnnotation(
                                tag = "URL",
                                annotation = url,
                                start = start,
                                end = transformedIndex
                            )

                            // Map positions
                            for (j in displayText.indices) {
                                originalToTransformed.add(transformedIndex - displayText.length + j)
                                transformedToOriginal.add(originalIndex)
                                originalIndex++
                            }

                            lastIndex = range.last + 1
                        }

                        if (lastIndex < content.length) {
                            val remainingText = content.substring(lastIndex)
                            annotatedStringBuilder.append(remainingText)
                            // Map positions
                            for (j in remainingText.indices) {
                                originalToTransformed.add(transformedIndex)
                                transformedToOriginal.add(originalIndex)
                                originalIndex++
                                transformedIndex++
                            }
                        }
                    }
                    is Token.MarkdownSymbol -> {
                        val symbol = token.symbol
                        val position = token.position

                        if (symbol == "---") {
                            // Handle horizontal rule
                            annotatedStringBuilder.append("\n──────────\n")
                            transformedIndex += 11 // Length of "──────────"

                            // Skip the original symbol in the mapping
                            for (j in symbol.indices) {
                                originalToTransformed.add(transformedIndex)
                                originalIndex++
                            }
                        } else if (symbol in openSymbols && openSymbols[symbol]?.isNotEmpty() == true) {
                            // Closing symbol
                            val startOriginalIndex = openSymbols[symbol]?.removeAt(openSymbols[symbol]!!.lastIndex) ?: continue
                            val style = markdownStyles[symbol] ?: continue

                            val transformedStart = getTransformedPosition(startOriginalIndex, originalToTransformed)
                            val transformedEnd = transformedIndex

                            // Apply style to the range
                            annotatedStringBuilder.addStyle(style, transformedStart, transformedEnd)
                        } else {
                            // Opening symbol
                            openSymbols.getOrPut(symbol) { mutableListOf() }.add(originalIndex)
                        }

                        // Skip the symbol in the transformed text (since we're removing markdown symbols)
                        for (j in symbol.indices) {
                            originalToTransformed.add(transformedIndex)
                            originalIndex++
                        }
                    }
                    is Token.Header -> {
                        val content = token.content
                        val level = token.level
                        val startIndex = transformedIndex

                        // Append the header content
                        annotatedStringBuilder.append(content)
                        transformedIndex += content.length

                        // Update mappings
                        for (j in content.indices) {
                            originalToTransformed.add(startIndex + j)
                            transformedToOriginal.add(originalIndex)
                            originalIndex++
                        }

                        // Append newline if present in the original text
                        if (originalIndex < originalText.length && originalText[originalIndex] == '\n') {
                            annotatedStringBuilder.append('\n')
                            transformedIndex += 1
                            originalToTransformed.add(transformedIndex - 1)
                            transformedToOriginal.add(originalIndex)
                            originalIndex++
                        }

                        // Apply header style
                        val headerEndIndex = transformedIndex
                        val fontSize = when (level) {
                            1 -> 32.sp
                            2 -> 28.sp
                            3 -> 24.sp
                            4 -> 20.sp
                            5 -> 16.sp
                            else -> 14.sp
                        }
                        annotatedStringBuilder.addStyle(
                            SpanStyle(fontSize = fontSize, fontWeight = FontWeight.Bold),
                            startIndex,
                            headerEndIndex
                        )
                    }
                }

                // Apply header style at the end of the line
                if (headerLevel != null && token is Token.Text && token.text.contains('\n')) {
                    val headerEndIndex = transformedIndex
                    val fontSize = when (headerLevel) {
                        1 -> 32.sp
                        2 -> 28.sp
                        3 -> 24.sp
                        4 -> 20.sp
                        5 -> 16.sp
                        else -> 14.sp
                    }
                    annotatedStringBuilder.addStyle(
                        SpanStyle(fontSize = fontSize, fontWeight = FontWeight.Bold),
                        headerStartIndex ?: 0,
                        headerEndIndex
                    )
                    headerLevel = null
                    headerStartIndex = null
                }
            }

            // Build the final AnnotatedString
            val transformedText = annotatedStringBuilder.toAnnotatedString()

            // Final mapping positions
            originalToTransformed.add(transformedIndex)
            transformedToOriginal.add(originalIndex)

            // OffsetMapping implementation
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return originalToTransformed.getOrNull(offset) ?: transformedIndex
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return transformedToOriginal.getOrNull(offset) ?: originalIndex
                }
            }

            return TransformedText(transformedText, offsetMapping)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return TransformedText(text, object: OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset
            }

            override fun transformedToOriginal(offset: Int): Int {
                return offset
            }
        })
    }

    private fun isMarkdownSymbolStart(text: String, index: Int): Boolean {
        return text.startsWith("#", index) && isStartOfLine(text, index) ||
                text.startsWith("```", index) ||
                text.startsWith("**", index) ||
                text.startsWith("_", index) ||
                text.startsWith("~~", index) ||
                text.startsWith("`", index) ||
                text.startsWith("==", index) ||
                text.startsWith("---", index)
    }

    private fun isStartOfLine(text: String, index: Int): Boolean {
        return index == 0 || text[index - 1] == '\n'
    }

    private fun getTransformedPosition(originalPosition: Int, originalToTransformed: List<Int>): Int {
        return originalToTransformed.getOrNull(originalPosition) ?: 0
    }

    // Token classes
    sealed class Token {
        data class Text(val text: String) : Token()
        data class MarkdownSymbol(val symbol: String, val position: Int) : Token()
        data class Header(val level: Int, val content: String) : Token()
    }

}

fun wrapSelectionWith(
    manager: TaskCardManager, // Replace with your actual manager type
    prefix: String,
    suffix: String = prefix
) {
    try {
        if (manager.selections.isEmpty()) return

        val textFieldValue = manager.description
        val text = textFieldValue.text
        val selection = textFieldValue.selection

        // Ensure start is less than or equal to end
        val start = minOf(manager.selections.first().start, selection.end)
        val end = maxOf(manager.selections.first().start, selection.end)

        val selectedText = text.substring(start, end)

        // Determine if we need to wrap or unwrap the selected text
        val isAlreadyWrapped = selectedText.startsWith(prefix) && selectedText.endsWith(suffix)

        val newText: String
        val newSelection: TextRange

        if (isAlreadyWrapped) {
            // Remove the prefix and suffix
            val unwrappedText = selectedText.substring(prefix.length, selectedText.length - suffix.length)
            newText = text.substring(0, start) + unwrappedText + text.substring(end)
            // Adjust selection to account for the removed prefix and suffix
            newSelection = TextRange(start, start + unwrappedText.length)
        } else {
            if (start == end) {
                // No text selected, insert prefix and suffix at the cursor position
                newText = text.substring(0, start) + prefix + suffix + text.substring(end)
                // Move the cursor between the prefix and suffix
                newSelection = TextRange(start + prefix.length)
            } else {
                // Wrap the selected text with prefix and suffix
                newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
                // Adjust selection to include the new prefix and suffix
                newSelection = TextRange(start, end + prefix.length + suffix.length)
            }
        }

        manager.description = textFieldValue.copy(
            text = newText,
            selection = newSelection
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun insertAtCursor(
    manager: TaskCardManager, // Replace with your actual manager type
    insertText: String
) {
    val textFieldValue = manager.description
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    val start = manager.selections.first().start
    val end = selection.end

    val newText = StringBuilder(text)
        .replace(start, end, insertText)
        .toString()

    val newSelection = TextRange(start + insertText.length)

    manager.description = textFieldValue.copy(
        text = newText,
        selection = newSelection
    )
}

fun unwrapSelectionFrom(
    manager: TaskCardManager,
    prefix: String,
    suffix: String = prefix
) {
    val textFieldValue = manager.description
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    val start = minOf(manager.selections.first().start, selection.end)
    val end = maxOf(manager.selections.first().start, selection.end)

    val selectedText = text.substring(start, end)

    // Remove prefix and suffix if present
    val unwrappedText = selectedText
        .removePrefix(prefix)
        .removeSuffix(suffix)

    val newText = text.substring(0, start) + unwrappedText + text.substring(end)
    val newSelection = TextRange(start, start + unwrappedText.length)

    manager.description = textFieldValue.copy(
        text = newText,
        selection = newSelection
    )
}

fun toggleList(
    manager: TaskCardManager,
    listType: String // "bullet" or "numbered"
) {
    val textFieldValue = manager.description
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    val start = minOf(manager.selections.first().start, selection.end)
    val end = maxOf(manager.selections.first().start, selection.end)

    // Split text into lines and keep track of line start indices
    val lineStartIndices = mutableListOf<Int>()
    lineStartIndices.add(0)
    text.forEachIndexed { index, c ->
        if (c == '\n') {
            lineStartIndices.add(index + 1)
        }
    }

    val lines = text.split('\n')

    // Identify lines within selection
    val indicesInSelection = mutableListOf<Int>()
    for (i in lines.indices) {
        val lineStart = lineStartIndices[i]
        val lineEnd = lineStart + lines[i].length

        if (lineEnd >= start && lineStart <= end) {
            // Line is within selection
            indicesInSelection.add(i)
        }
    }

    val (regex, addPrefix) = when (listType) {
        "bullet" -> Regex("^\\s*([-•])\\s") to { line: String -> "• ${line.trimStart()}" }
        "numbered" -> Regex("^\\s*(\\d+)\\.\\s") to { line: String -> "" } // Placeholder
        else -> return
    }

    val linesInSelection = indicesInSelection.map { lines[it] }
    val allListed = linesInSelection.all { regex.containsMatchIn(it) }

    // Now, process all lines
    val newLines = mutableListOf<String>()
    var number = 1
    for (i in lines.indices) {
        val lineText = lines[i]
        if (i in indicesInSelection) {
            // Line is within selection
            if (allListed) {
                // Remove list prefix
                val newLine = lineText.replaceFirst(regex, "")
                newLines.add(newLine)
            } else {
                // Add list prefix
                val newLine = if (listType == "numbered") {
                    "${number++}. ${lineText.trimStart()}"
                } else {
                    addPrefix(lineText)
                }
                newLines.add(newLine)
            }
        } else {
            // Line is outside selection, keep as is
            newLines.add(lineText)
        }
    }

    // Reconstruct the text
    val newText = newLines.joinToString("\n")

    // Adjust selection
    val lengthDifference = newText.length - text.length
    val newSelection = TextRange(
        start = manager.selections.first().start,
        end = selection.end + lengthDifference
    )

    manager.description = textFieldValue.copy(
        text = newText,
        selection = newSelection
    )
}



fun toggleCodeBlock(manager: TaskCardManager) {
    val textFieldValue = manager.description
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    val start = minOf(manager.selections.first().start, selection.end)
    val end = maxOf(manager.selections.first().start, selection.end)

    val selectedText = text.substring(start, end)

    val codeBlockPattern = Regex("^```\\n[\\s\\S]*?\\n```$", RegexOption.MULTILINE)
    val isCodeBlock = codeBlockPattern.matches(selectedText)

    val newText: String
    val newSelection: TextRange

    if (isCodeBlock) {
        // Remove code block markers
        val unwrappedText = selectedText.removePrefix("```\n").removeSuffix("\n```")
        newText = text.substring(0, start) + unwrappedText + text.substring(end)
        newSelection = TextRange(start, start + unwrappedText.length)
    } else {
        // Add code block markers
        val wrappedText = "```\n$selectedText\n```"
        newText = text.substring(0, start) + wrappedText + text.substring(end)
        newSelection = TextRange(start, start + wrappedText.length)
    }

    manager.description = textFieldValue.copy(
        text = newText,
        selection = newSelection
    )
}

fun toggleHighlight(manager: TaskCardManager) {
    wrapSelectionWith(manager, "==")
}

fun toggleHeader(manager: TaskCardManager, level: Int) {
    val textFieldValue = manager.description
    val text = textFieldValue.text
    val cursorPosition = manager.selections.first().start

    // Find the start of the line containing the cursor
    val lineStart = text.lastIndexOf('\n', cursorPosition - 1)
        .let { if (it == -1) 0 else it + 1 }

    // Find the end of the line containing the cursor (exclude the newline character)
    val lineEnd = text.indexOf('\n', cursorPosition)
        .let { if (it == -1) text.length else it }

    val lineText = text.substring(lineStart, lineEnd)
    val headerRegex = Regex("^(#{1,6})\\s")
    val headerPrefix = "#".repeat(level) + " "

    val newLineText = if (lineText.startsWith(headerPrefix)) {
        // Remove header prefix
        lineText.replaceFirst(headerRegex, "").trimStart()
    } else {
        // Add header prefix
        headerPrefix + lineText
    }

    // Build the new text
    val newText = buildString {
        append(text.substring(0, lineStart))
        append(newLineText)
        append(text.substring(lineEnd))
    }

    // Adjust the cursor position
    val lengthDifference = newText.length - text.length
    val newCursorPosition = (cursorPosition + lengthDifference).coerceAtLeast(0)

    manager.description = textFieldValue.copy(
        text = newText,
        selection = TextRange(newCursorPosition)
    )
}


