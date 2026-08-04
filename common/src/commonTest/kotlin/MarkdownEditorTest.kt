import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.elfefe.common.controller.MARKDOWN_URL_TAG
import com.elfefe.common.controller.MarkdownVisualTransformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Rendu du markdown **saisi** : listes, liens, et surtout la garantie que la
 * tokenisation termine. Les cas de style imbrique vivent dans [MarkdownTest].
 */
class MarkdownEditorTest {

    /**
     * Applique la transformation en bornant le temps. Un caractere qui annonce
     * un symbole sans en former un (« #tag », « [x ») faisait tourner la
     * tokenisation sans fin : l'interface gelait sans la moindre exception.
     * Un simple appel direct ne verrait pas la difference entre « long » et
     * « infini », d'ou le fil dedie.
     */
    private fun transform(input: String, timeoutMs: Long = 5_000): TransformedText {
        var result: TransformedText? = null
        var failure: Throwable? = null
        val worker = Thread {
            runCatching { MarkdownVisualTransformation().filter(AnnotatedString(input)) }
                .onSuccess { result = it }
                .onFailure { failure = it }
        }
        worker.isDaemon = true
        worker.start()
        worker.join(timeoutMs)
        if (worker.isAlive) fail("La transformation de \"$input\" ne termine pas (${timeoutMs} ms)")
        failure?.let { throw it }
        return result ?: fail("Aucun resultat pour \"$input\"")
    }

    @Test
    fun `un diese sans espace ne fige pas la transformation`() {
        assertEquals("#tag", transform("#tag").text.text)
    }

    @Test
    fun `un crochet isole ne fige pas la transformation`() {
        assertEquals("[note sans lien", transform("[note sans lien").text.text)
    }

    @Test
    fun `un diese en milieu de ligne reste du texte`() {
        assertEquals("ticket #42 corrige", transform("ticket #42 corrige").text.text)
    }

    @Test
    fun `un tiret en tete de ligne devient une puce`() {
        assertEquals("• premier\n• second", transform("- premier\n- second").text.text)
    }

    @Test
    fun `une etoile en tete de ligne est une puce, pas une italique`() {
        // Sans traitement de la puce, l'etoile ouvrait une italique et avalait
        // la suite du texte.
        assertEquals("• premier\n• second", transform("* premier\n* second").text.text)
    }

    @Test
    fun `un tiret en milieu de ligne reste un tiret`() {
        assertEquals("gauche - droite", transform("gauche - droite").text.text)
    }

    @Test
    fun `un filet horizontal reste un filet`() {
        assertTrue(transform("---").text.text.contains("──────"))
    }

    @Test
    fun `un lien n'affiche que son libelle`() {
        val transformed = transform("voir [le site](https://exemple.fr) demain")
        assertEquals("voir le site demain", transformed.text.text)
    }

    @Test
    fun `un lien porte son url en annotation`() {
        val transformed = transform("[le site](https://exemple.fr)")
        val annotations = transformed.text.getStringAnnotations(
            MARKDOWN_URL_TAG, 0, transformed.text.length
        )
        assertNotNull(annotations.firstOrNull())
        assertEquals("https://exemple.fr", annotations.first().item)
        assertEquals(0, annotations.first().start)
        assertEquals("le site".length, annotations.first().end)
    }

    @Test
    fun `les positions restent dans les bornes apres un lien`() {
        val input = "voir [le site](https://exemple.fr) demain"
        val transformed = transform(input)
        val mapping = transformed.offsetMapping
        // Le curseur doit rester placable partout dans le texte source sans
        // jamais designer une position inexistante dans le rendu.
        for (offset in 0..input.length) {
            val rendered = mapping.originalToTransformed(offset)
            assertTrue(
                rendered in 0..transformed.text.length,
                "position $offset projetee hors bornes : $rendered"
            )
        }
    }

    @Test
    fun `le gras survit a la presence d'une puce`() {
        val transformed = transform("- un **point** important")
        assertEquals("• un point important", transformed.text.text)
    }
}
