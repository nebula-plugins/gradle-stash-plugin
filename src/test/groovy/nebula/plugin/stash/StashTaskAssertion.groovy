package nebula.plugin.stash

import nebula.plugin.stash.tasks.StashTask
import org.gradle.api.tasks.TaskValidationException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

final class StashTaskAssertion {
    private StashTaskAssertion() {}

    static void runTaskExpectFail(StashTask task, String missingParam) {
        try {
            task.execute()
            fail("should have thrown a validation exception")
        } catch (TaskValidationException e) {
            assertEquals("No value has been specified for property '$missingParam'.".toString(), e.cause.message)
        }
    }
}
