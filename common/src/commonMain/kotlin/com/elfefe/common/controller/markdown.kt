package com.elfefe.common.controller

import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import com.elfefe.common.ui.view.TaskCardManager

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text

        // Tokens and positions
        val tokens = mutableListOf<Token>()
        var i = 0

        // Tokenization
        while (i < originalText.length) {
            // Check for markdown symbols
            val symbol = when {
                originalText.startsWith("**", i) || originalText.startsWith("__", i) -> originalText.substring(i, i + 2)
                originalText.startsWith("*", i) || originalText.startsWith("_", i) || originalText.startsWith("`", i) -> originalText.substring(i, i + 1)
                originalText.startsWith("~~", i) -> originalText.substring(i, i + 2)
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
                tokens.add(Token.Text(originalText.substring(start, i)))
            }
        }

        // Parsing tokens into style spans
        val openSymbols = mutableMapOf<String, MutableList<Int>>() // Symbol to stack of positions

        val markdownStyles = mapOf(
            "*" to SpanStyle(fontStyle = FontStyle.Italic),
            "_" to SpanStyle(fontStyle = FontStyle.Italic),
            "**" to SpanStyle(fontWeight = FontWeight.Bold),
            "__" to SpanStyle(fontWeight = FontWeight.Bold),
            "~~" to SpanStyle(textDecoration = TextDecoration.LineThrough),
            "`" to SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray)
        )

        // For mapping positions
        val originalToTransformed = mutableListOf<Int>()
        val transformedToOriginal = mutableListOf<Int>()

        val annotatedStringBuilder = AnnotatedString.Builder()
        var originalIndex = 0
        var transformedIndex = 0

        for (token in tokens) {
            when (token) {
                is Token.Text -> {
                    val content = token.text
                    annotatedStringBuilder.append(content)

                    // Map positions
                    for (j in content.indices) {
                        originalToTransformed.add(transformedIndex)
                        transformedToOriginal.add(originalIndex)
                        originalIndex++
                        transformedIndex++
                    }
                }
                is Token.MarkdownSymbol -> {
                    val symbol = token.symbol
                    val position = token.position

                    if (symbol in openSymbols && openSymbols[symbol]?.isNotEmpty() == true) {
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
                    for (j in 0 until symbol.length) {
                        originalToTransformed.add(transformedIndex)
                        originalIndex++
                    }
                }
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
    }

    private fun isMarkdownSymbolStart(text: String, index: Int): Boolean {
        return text.startsWith("**", index) ||
                text.startsWith("__", index) ||
                text.startsWith("*", index) ||
                text.startsWith("_", index) ||
                text.startsWith("~~", index) ||
                text.startsWith("`", index)
    }

    private fun getTransformedPosition(originalPosition: Int, originalToTransformed: List<Int>): Int {
        return originalToTransformed.getOrNull(originalPosition) ?: 0
    }

    // Token classes
    sealed class Token {
        data class Text(val text: String) : Token()
        data class MarkdownSymbol(val symbol: String, val position: Int) : Token()
    }
}



fun wrapSelectionWith(
    manager: TaskCardManager, // Replace with your actual manager type
    prefix: String,
    suffix: String = prefix
) {
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
}

fun insertAtCursor(
    manager: TaskCardManager, // Replace with your actual manager type
    insertText: String
) {
    val textFieldValue = manager.description
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    val start = selection.start
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

    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)

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

sealed class Token {
    data class Text(val text: String) : Token()
    data class MarkdownSymbol(val symbol: String, val position: Int) : Token()
}

data class StyleSpan(
    val style: SpanStyle,
    val start: Int,
    val end: Int
)

