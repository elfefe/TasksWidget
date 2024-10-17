import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import kotlin.test.Test
import kotlin.test.assertTrue

class TasksTest {
    @Test
    fun `test tasks`() {
        Tasks.update(Task())
        assertTrue(Tasks.tasks.isNotEmpty())
    }
}