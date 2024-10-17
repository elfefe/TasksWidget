import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.elfefe.common.controller.MarkdownVisualTransformation
import junit.framework.TestCase.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTest {

    private fun applyTransformation(input: String): TransformedText {
        val transformation = MarkdownVisualTransformation()
        val annotatedString = AnnotatedString(input)
        return transformation.filter(annotatedString)
    }

    @Test
    fun testNestedBoldAndItalic() {
        val input = "This is **bold and _italic_** text."
        val transformedText = applyTransformation(input)

        val expectedText = "This is bold and italic text."
        assertEquals(expectedText, transformedText.text.text)

        // Check bold annotations
        val boldAnnotations =
            transformedText.text.getStringAnnotations(tag = "**", start = 0, end = expectedText.length)
        assertFalse(boldAnnotations.isEmpty())

        val boldAnnotation = boldAnnotations.first()
        val boldExpectedStart = expectedText.indexOf("bold and italic")
        val boldExpectedEnd = boldExpectedStart + "bold and italic".length
        assertEquals(boldExpectedStart, boldAnnotation.start)
        assertEquals(boldExpectedEnd, boldAnnotation.end)

        // Check italic annotations
        val italicAnnotations =
            transformedText.text.getStringAnnotations(tag = "_", start = 0, end = expectedText.length)
        assertFalse(italicAnnotations.isEmpty())

        val italicAnnotation = italicAnnotations.first()
        val italicExpectedStart = expectedText.indexOf("italic")
        val italicExpectedEnd = italicExpectedStart + "italic".length
        assertEquals(italicExpectedStart, italicAnnotation.start)
        assertEquals(italicExpectedEnd, italicAnnotation.end)
    }

    @Test
    fun testOverlappingBoldAndStrikethrough() {
        val input = "~~This is **bold strikethrough** text~~."
        val transformedText = applyTransformation(input)

        val expectedText = "This is bold strikethrough text."
        assertEquals(expectedText, transformedText.text.text)

        // Check strikethrough annotations
        val strikethroughAnnotations =
            transformedText.text.getStringAnnotations(tag = "~~", start = 0, end = expectedText.length)
        assertFalse(strikethroughAnnotations.isEmpty())

        val strikethroughAnnotation = strikethroughAnnotations.first()
        assertEquals(0, strikethroughAnnotation.start)
        assertEquals(expectedText.length, strikethroughAnnotation.end)

        // Check bold annotations
        val boldAnnotations =
            transformedText.text.getStringAnnotations(tag = "**", start = 0, end = expectedText.length)
        assertFalse(boldAnnotations.isEmpty())

        val boldAnnotation = boldAnnotations.first()
        val boldExpectedStart = expectedText.indexOf("bold strikethrough")
        val boldExpectedEnd = boldExpectedStart + "bold strikethrough".length
        assertEquals(boldExpectedStart, boldAnnotation.start)
        assertEquals(boldExpectedEnd, boldAnnotation.end)
    }

    @Test
    fun testBoldItalicCode() {
        val input = "This is **bold and _italic with `code`_** text."
        val transformedText = applyTransformation(input)

        val expectedText = "This is bold and italic with code text."
        assertEquals(expectedText, transformedText.text.text)

        // Check bold annotations
        val boldAnnotations =
            transformedText.text.getStringAnnotations(tag = "**", start = 0, end = expectedText.length)
        assertFalse(boldAnnotations.isEmpty())

        val boldAnnotation = boldAnnotations.first()
        val boldExpectedStart = expectedText.indexOf("bold and italic with code")
        val boldExpectedEnd = boldExpectedStart + "bold and italic with code".length
        assertEquals(boldExpectedStart, boldAnnotation.start)
        assertEquals(boldExpectedEnd, boldAnnotation.end)

        // Check italic annotations
        val italicAnnotations =
            transformedText.text.getStringAnnotations(tag = "_", start = 0, end = expectedText.length)
        assertFalse(italicAnnotations.isEmpty())

        val italicAnnotation = italicAnnotations.first()
        val italicExpectedStart = expectedText.indexOf("italic with code")
        val italicExpectedEnd = italicExpectedStart + "italic with code".length
        assertEquals(italicExpectedStart, italicAnnotation.start)
        assertEquals(italicExpectedEnd, italicAnnotation.end)

        // Check code annotations
        val codeAnnotations = transformedText.text.getStringAnnotations(tag = "`", start = 0, end = expectedText.length)
        assertFalse(codeAnnotations.isEmpty())

        val codeAnnotation = codeAnnotations.first()
        val codeExpectedStart = expectedText.indexOf("code")
        val codeExpectedEnd = codeExpectedStart + "code".length
        assertEquals(codeExpectedStart, codeAnnotation.start)
        assertEquals(codeExpectedEnd, codeAnnotation.end)
    }
}