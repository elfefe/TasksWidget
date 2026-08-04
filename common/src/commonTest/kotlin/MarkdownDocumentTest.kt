import com.elfefe.common.controller.MARKDOWN_URL_TAG
import com.elfefe.common.controller.MarkdownBlock
import com.elfefe.common.controller.MarkdownDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Rendu du markdown **recu** : les reponses de Claude, lues depuis le transcript. */
class MarkdownDocumentTest {

    @Test
    fun `un titre est reconnu avec son niveau`() {
        val blocks = MarkdownDocument.parse("## Resultat")
        val heading = blocks.filterIsInstance<MarkdownBlock.Heading>().firstOrNull()
        assertNotNull(heading)
        assertEquals(2, heading.level)
        assertEquals("Resultat", heading.text.text)
    }

    @Test
    fun `une liste a puces donne une entree par element`() {
        val blocks = MarkdownDocument.parse("- premier\n- second")
        val entries = blocks.filterIsInstance<MarkdownBlock.ListEntry>()
        assertEquals(2, entries.size)
        assertEquals("premier", entries[0].text.text)
        assertEquals("•", entries[0].marker)
    }

    @Test
    fun `une liste numerotee garde son rang`() {
        val blocks = MarkdownDocument.parse("1. premier\n2. second")
        val entries = blocks.filterIsInstance<MarkdownBlock.ListEntry>()
        assertEquals(2, entries.size)
        assertEquals("1.", entries[0].marker)
        assertEquals("2.", entries[1].marker)
    }

    @Test
    fun `une sous-liste est mise en retrait`() {
        val blocks = MarkdownDocument.parse("- parent\n    - enfant")
        val entries = blocks.filterIsInstance<MarkdownBlock.ListEntry>()
        assertEquals(2, entries.size)
        assertTrue(entries[1].depth > entries[0].depth, "l'enfant doit etre plus a droite")
    }

    @Test
    fun `un bloc de code garde son langage et son contenu`() {
        val blocks = MarkdownDocument.parse("```kotlin\nval x = 1\n```")
        val code = blocks.filterIsInstance<MarkdownBlock.CodeBlock>().firstOrNull()
        assertNotNull(code)
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
    }

    @Test
    fun `le gras perd ses marqueurs`() {
        val blocks = MarkdownDocument.parse("un mot **important** ici")
        val paragraph = blocks.filterIsInstance<MarkdownBlock.Paragraph>().first()
        assertEquals("un mot important ici", paragraph.text.text)
    }

    @Test
    fun `un lien conserve son url en annotation`() {
        val blocks = MarkdownDocument.parse("voir [le site](https://exemple.fr)")
        val paragraph = blocks.filterIsInstance<MarkdownBlock.Paragraph>().first()
        assertEquals("voir le site", paragraph.text.text)
        val annotation = paragraph.text
            .getStringAnnotations(MARKDOWN_URL_TAG, 0, paragraph.text.length)
            .firstOrNull()
        assertNotNull(annotation)
        assertEquals("https://exemple.fr", annotation.item)
    }

    @Test
    fun `une citation devient un bloc de citation`() {
        val blocks = MarkdownDocument.parse("> une remarque")
        val quote = blocks.filterIsInstance<MarkdownBlock.Quote>().firstOrNull()
        assertNotNull(quote)
        assertEquals("une remarque", quote.text.text)
    }

    @Test
    fun `du texte sans markdown reste un paragraphe intact`() {
        val blocks = MarkdownDocument.parse("juste une phrase")
        assertEquals(1, blocks.size)
        assertEquals("juste une phrase", (blocks.first() as MarkdownBlock.Paragraph).text.text)
    }

    @Test
    fun `un texte vide ne produit aucun bloc`() {
        assertTrue(MarkdownDocument.parse("").isEmpty())
    }

    @Test
    fun `une reponse melangee garde l'ordre de ses blocs`() {
        val source = """
            ## Resultat

            - **corrige** le bug
            - voir `main.kt`

            ```kotlin
            val x = 1
            ```
        """.trimIndent()
        val blocks = MarkdownDocument.parse(source)
        val kinds = blocks.map { it::class.simpleName }
        assertEquals("Heading", kinds.first())
        assertTrue(blocks.filterIsInstance<MarkdownBlock.ListEntry>().size == 2)
        assertEquals("CodeBlock", kinds.last())
    }
}
