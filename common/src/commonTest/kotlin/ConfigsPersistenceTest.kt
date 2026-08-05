import androidx.compose.ui.graphics.Color
import com.elfefe.common.model.Configs
import com.elfefe.common.model.ConfigsAdapter
import com.elfefe.common.model.HANDLE_Y_AUTO
import com.elfefe.common.model.HoldKey
import com.elfefe.common.model.TaskState
import com.elfefe.common.ui.theme.AppFont
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Enregistrement de la configuration : les reglages doivent survivre a un
 * aller-retour sur disque, et toute modification doit se signaler.
 */
class ConfigsPersistenceTest {

    @Test
    fun `la touche de maintien et la place de la poignee survivent a un aller-retour`() {
        val adapter = ConfigsAdapter()
        val configs = Configs(holdKey = HoldKey.ALT, handleY = 412.5f, anchorRight = false)

        val restored = adapter.fromJson(adapter.toJson(configs))

        assertEquals(HoldKey.ALT, restored.holdKey)
        assertEquals(412.5f, restored.handleY)
        assertFalse(restored.anchorRight)
    }

    @Test
    fun `l'epingle survit a un aller-retour et se signale`() {
        val adapter = ConfigsAdapter()
        val restored = adapter.fromJson(adapter.toJson(Configs(pinned = true)))
        assertTrue(restored.pinned)

        var notified = 0
        val configs = Configs().apply { onChanged = { notified++ } }
        assertFalse(configs.pinned)
        configs.updatePinned(true)
        assertTrue(configs.pinned)
        assertEquals(1, notified)
    }

    @Test
    fun `la police survit a un aller-retour et se signale`() {
        val adapter = ConfigsAdapter()
        val restored = adapter.fromJson(adapter.toJson(Configs(font = AppFont.PLEX)))
        assertEquals(AppFont.PLEX, restored.font)

        var notified = 0
        val configs = Configs().apply { onChanged = { notified++ } }
        assertEquals(AppFont.INTER, configs.font)
        configs.updateFont(AppFont.ATKINSON)
        assertEquals(AppFont.ATKINSON, configs.font)
        assertEquals(1, notified)
    }

    @Test
    fun `une police inconnue retombe sur Inter`() {
        // Une configuration ecrite avant ce reglage, ou par une version future,
        // ne doit pas empecher la lecture du reste du fichier.
        assertEquals(AppFont.INTER, AppFont.of(null))
        assertEquals(AppFont.INTER, AppFont.of("police-qui-n-existe-pas"))
        assertEquals(AppFont.SYSTEM, AppFont.of("system"))
    }

    @Test
    fun `le filtre d'etats survit a un aller-retour et se signale`() {
        val adapter = ConfigsAdapter()
        val choisi = setOf(TaskState.BUSY, TaskState.DONE)
        val restored = adapter.fromJson(adapter.toJson(Configs(stateFilters = choisi)))
        assertEquals(choisi, restored.stateFilters)

        var notified = 0
        val configs = Configs().apply { onChanged = { notified++ } }
        assertEquals(TaskState.DEFAULT, configs.stateFilters)
        configs.updateStateFilters(setOf(TaskState.WAITING))
        assertEquals(setOf(TaskState.WAITING), configs.stateFilters)
        assertEquals(1, notified)
    }

    @Test
    fun `tout decocher est un choix, pas un fichier muet`() {
        // Une selection vide masque toutes les cartes : c'est volontaire et cela
        // doit se relire tel quel, sans etre repris pour un reglage absent qu'on
        // remplacerait par celui d'origine.
        val adapter = ConfigsAdapter()
        val restored = adapter.fromJson(adapter.toJson(Configs(stateFilters = emptySet())))
        assertTrue(restored.stateFilters.isEmpty())
    }

    @Test
    fun `une configuration ecrite avant le filtre montre ce qu'elle montrait`() {
        // Fichier d'une version anterieure : pas de champ `stateFilters`. La pile
        // doit retrouver son affichage d'alors — tout sauf les taches terminees.
        val json = """
            {
              "orders": [],
              "language": "fr",
              "holdKey": "ctrl"
            }
        """.trimIndent()

        assertEquals(TaskState.DEFAULT, ConfigsAdapter().fromJson(json).stateFilters)
    }

    @Test
    fun `un etat inconnu est ignore sans emporter les autres`() {
        val json = """
            {
              "orders": [],
              "stateFilters": ["busy", "etat-d-une-version-future", "done"]
            }
        """.trimIndent()

        assertEquals(
            setOf(TaskState.BUSY, TaskState.DONE),
            ConfigsAdapter().fromJson(json).stateFilters
        )
    }

    @Test
    fun `une configuration neuve laisse la poignee se placer d'office`() {
        val restored = ConfigsAdapter().let { it.fromJson(it.toJson(Configs())) }
        assertEquals(HANDLE_Y_AUTO, restored.handleY)
        assertTrue(restored.anchorRight)
    }

    @Test
    fun `un champ inconnu ne fait pas perdre la configuration`() {
        // Ouvrir une version plus recente puis revenir en arriere ajoutait des
        // champs que la lecture refusait : le fichier partait en .bak et
        // l'utilisateur retrouvait un theme neuf.
        val json = """
            {
              "orders": [],
              "colors": {
                "primary": "ff123456",
                "onPrimary": "ffffffff",
                "secondary": "ff005e7d",
                "onSecondary": "ffffffff",
                "background": "ffffffff",
                "onBackground": "ff000000"
              },
              "language": "fr",
              "holdKey": "shift",
              "reglageDUneVersionFuture": 42
            }
        """.trimIndent()

        val restored = ConfigsAdapter().fromJson(json)

        assertEquals(HoldKey.SHIFT, restored.holdKey)
        assertEquals("fr", restored.language)
    }

    @Test
    fun `changer une couleur previent qu'il faut enregistrer`() {
        // Les mutateurs changent l'interieur de l'objet sans jamais remplacer sa
        // reference : sans ce rappel, rien ne declenchait l'ecriture et le
        // fichier de configuration restait vide.
        var notified = 0
        val configs = Configs().apply { onChanged = { notified++ } }

        configs.updateThemeColors(primary = Color.Red)
        assertEquals(1, notified)

        configs.updateHoldKey(HoldKey.SHIFT)
        assertEquals(2, notified)

        configs.updateHandlePlacement(300f, anchorRight = false)
        assertEquals(3, notified)

        configs.resetThemeColors()
        assertEquals(4, notified)
    }

    @Test
    fun `retablir les couleurs redonne la palette livree`() {
        val configs = Configs()
        val livree = configs.themeColors

        configs.updateThemeColors(primary = Color.Magenta, background = Color.Yellow)
        configs.resetThemeColors()

        assertEquals(livree.primary, configs.themeColors.primary)
        assertEquals(livree.background, configs.themeColors.background)
    }

    @Test
    fun `la touche inconnue retombe sur Ctrl`() {
        assertEquals(HoldKey.CONTROL, HoldKey.of("touche-qui-n-existe-pas"))
        assertEquals(HoldKey.CONTROL, HoldKey.of(null))
        assertEquals(HoldKey.ALT, HoldKey.of("alt"))
    }
}
