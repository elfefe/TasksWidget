import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

class TasksTest {
    @Test
    fun `test tasks`() = runBlocking {
        Tasks.update(Task())
        // update() reflète la tâche via une coroutine de fond : on l'attend au
        // lieu de supposer qu'elle a déjà tourné. Sans cette attente, le test
        // gagnait la course sur une machine rapide et la perdait sur un runner
        // de CI plus lent.
        withTimeout(3000) { while (Tasks.tasks.isEmpty()) delay(10) }
        assertTrue(Tasks.tasks.isNotEmpty())
    }
}
