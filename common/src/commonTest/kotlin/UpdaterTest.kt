import com.elfefe.common.controller.isRemoteNewer
import com.elfefe.common.controller.versionParts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdaterTest {

    @Test
    fun `tag prefixe v egale version locale n'est pas plus recent`() {
        // Le bug historique : le tag GitHub "v1.4.4" et la version embarquée
        // "1.4.4" étaient jugés différents, donc l'appli se croyait périmée.
        assertFalse(isRemoteNewer("v1.4.4", "1.4.4"))
        assertFalse(isRemoteNewer("1.4.4", "1.4.4"))
    }

    @Test
    fun `release plus recente est detectee`() {
        assertTrue(isRemoteNewer("v1.4.5", "1.4.4"))
        assertTrue(isRemoteNewer("v2.0.0", "1.9.9"))
        assertTrue(isRemoteNewer("v1.5", "1.4.9"))
        assertTrue(isRemoteNewer("v1.4.10", "1.4.9"))
    }

    @Test
    fun `release plus ancienne ou nulle n'est pas proposee`() {
        assertFalse(isRemoteNewer("v1.4.3", "1.4.4"))
        assertFalse(isRemoteNewer("v1.4.4", "1.4.5"))
        assertFalse(isRemoteNewer(null, "1.4.4"))
        assertFalse(isRemoteNewer("v1.4.4", null))
    }

    @Test
    fun `decoupage de version tolere le prefixe et les suffixes`() {
        assertEquals(listOf(1, 4, 4), versionParts("v1.4.4"))
        assertEquals(listOf(1, 4, 4), versionParts("1.4.4"))
        assertEquals(listOf(1, 4, 4), versionParts("1.4.4-rc1"))
    }
}
