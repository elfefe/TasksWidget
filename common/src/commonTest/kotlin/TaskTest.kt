import androidx.compose.ui.graphics.Color
import com.elfefe.common.model.TaskFieldOrder
import com.elfefe.common.model.TaskFieldOrderAdapter
import com.elfefe.common.model.ThemeColors
import com.elfefe.common.model.ThemeColorsAdapter
import com.elfefe.common.ui.theme.background
import com.elfefe.common.ui.theme.onBackground
import com.elfefe.common.ui.theme.onPrimary
import com.elfefe.common.ui.theme.onSecondary
import com.elfefe.common.ui.theme.primary
import com.elfefe.common.ui.theme.secondary
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskTest {
    @Test
    fun `test read and write TaskFieldOrder`() {
        val adapter = TaskFieldOrderAdapter()
        val taskFieldOrder = TaskFieldOrder("name", 1, true)
        val json = adapter.toJson(taskFieldOrder)
        val taskFieldOrder2 = adapter.fromJson(json)
        assertEquals(taskFieldOrder, taskFieldOrder2)
    }

    @Test
    fun `test read and write ThemeColors`() {
        val adapter = ThemeColorsAdapter()
        val themeColors = ThemeColors(
            primary = Color.primary,
            onPrimary = Color.onPrimary,
            secondary = Color.secondary,
            onSecondary = Color.onSecondary,
            background = Color.background,
            onBackground = Color.onBackground,
        )
        val json = adapter.toJson(themeColors)
        val themeColors2 = adapter.fromJson(json)
        assertEquals(themeColors, themeColors2)
    }
}