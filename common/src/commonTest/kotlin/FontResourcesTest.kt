import com.elfefe.common.ui.theme.AppFont
import com.elfefe.common.ui.theme.family
import org.jetbrains.skia.Data
import org.jetbrains.skia.Typeface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Les polices sont embarquées, plus héritées de la machine.
 *
 * Le seul mode d'échec réaliste est un fichier absent du jar ou un chemin de
 * ressource erroné : `Font(resource = ...)` ne lit rien à la construction, la
 * panne n'apparaîtrait qu'au premier texte dessiné, donc chez l'utilisateur.
 * On vérifie ici que chaque fichier est présent dans le classpath et que Skia
 * — le moteur qui les dessinera — sait réellement les ouvrir.
 */
class FontResourcesTest {

    private val faces = listOf(
        "Inter-Regular.ttf" to "Inter",
        "Inter-Medium.ttf" to "Inter",
        "Inter-SemiBold.ttf" to "Inter",
        "IBMPlexSans-Regular.ttf" to "IBM Plex Sans",
        "IBMPlexSans-Medium.ttf" to "IBM Plex Sans",
        "IBMPlexSans-SemiBold.ttf" to "IBM Plex Sans",
        "AtkinsonHyperlegible-Regular.ttf" to "Atkinson Hyperlegible",
        "AtkinsonHyperlegible-Bold.ttf" to "Atkinson Hyperlegible",
        "JetBrainsMono-Regular.ttf" to "JetBrains Mono",
        "JetBrainsMono-Bold.ttf" to "JetBrains Mono",
    )

    @Test
    fun `chaque police embarquee est presente et lisible par Skia`() {
        faces.forEach { (file, expectedFamily) ->
            val bytes = javaClass.classLoader.getResourceAsStream("fonts/$file")?.readBytes()
            assertNotNull(bytes, "fonts/$file absent des ressources")
            assertTrue(bytes.size > 10_000, "fonts/$file semble tronqué (${bytes.size} octets)")

            val typeface = Typeface.makeFromData(Data.makeFromBytes(bytes))
            assertEquals(expectedFamily, typeface.familyName, "fonts/$file")
        }
    }

    @Test
    fun `la licence des polices est distribuee avec elles`() {
        // Les quatre familles sont sous SIL Open Font License 1.1, qui impose
        // de joindre son texte aux fichiers redistribués.
        val licence = javaClass.classLoader.getResourceAsStream("fonts/OFL.txt")
            ?.readBytes()?.toString(Charsets.UTF_8)
        assertNotNull(licence)
        assertTrue(licence.contains("SIL OPEN FONT LICENSE", ignoreCase = true))
    }

    @Test
    fun `chaque police proposee a une famille resolue`() {
        // Un ajout d'entrée dans l'énumération sans fichier correspondant
        // passerait inaperçu jusqu'à l'affichage.
        AppFont.values().forEach { font ->
            assertNotNull(font.family(), font.label)
        }
    }
}
