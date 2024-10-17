import com.elfefe.common.controller.deadlineDate
import com.elfefe.common.controller.getDate
import kotlin.test.Test
import kotlin.test.assertTrue

class TaskUtilsTest {
    @Test
    fun `test get date in correct format`() {
        val date = getDate()
        assert(date.matches(Regex("\\d{2}\\d{2}")))
    }

    @Test
    fun `test deadline date`() {
        val deadlineOld = deadlineDate("0000")
        val deadlineRecent = deadlineDate("9999")
        val deadlineToday = deadlineDate(getDate())
        assertTrue(deadlineOld < 0)
        assertTrue(deadlineRecent > 0)
        assertTrue(deadlineToday == 0)
    }
}