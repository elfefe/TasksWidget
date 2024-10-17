import androidx.compose.ui.graphics.Color
import com.elfefe.common.model.Configs
import com.elfefe.common.model.ConfigsAdapter
import com.elfefe.common.model.TaskFieldOrder
import com.elfefe.common.model.ThemeColors
import com.elfefe.common.ui.theme.background
import com.elfefe.common.ui.theme.onBackground
import com.elfefe.common.ui.theme.onPrimary
import com.elfefe.common.ui.theme.onSecondary
import com.elfefe.common.ui.theme.primary
import com.elfefe.common.ui.theme.secondary
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigsTest {
    @Test
    fun `test read and write Configs`() {
        val adapter = ConfigsAdapter()
        val configs = Configs(
            taskFieldsOrder = listOf(
                TaskFieldOrder("name", 1, true),
                TaskFieldOrder("priority", 2, true),
                TaskFieldOrder("active", 3, true),
            ),
            themeColors = ThemeColors(
                primary = Color.primary,
                onPrimary = Color.onPrimary,
                secondary = Color.secondary,
                onSecondary = Color.onSecondary,
                background = Color.background,
                onBackground = Color.onBackground,
            ),
            language = "en"
        )
        val json = adapter.toJson(configs)
        val configs2 = adapter.fromJson(json)
        assertEquals(configs.toString(), configs2.toString())
    }
}