package nebula.plugin.stash

import nebula.plugin.stash.tasks.StashTask
import org.gradle.api.Project

final class StashPluginFixture {
    final static String DUMMY_VALUE = 'foo'

    private StashPluginFixture() {}

    static void setDummyExtensionPropertyValues(Project project) {
        project.stash {
            stashRepo = DUMMY_VALUE
            stashProject = DUMMY_VALUE
            stashUser = DUMMY_VALUE
            stashPassword = DUMMY_VALUE
            stashHost = DUMMY_VALUE
        }
    }

    static void setDummyStashTaskPropertyValues(Project project) {
        project.tasks.withType(StashTask) {
            stashRepo = DUMMY_VALUE
            stashProject = DUMMY_VALUE
            stashUser = DUMMY_VALUE
            stashPassword = DUMMY_VALUE
            stashHost = DUMMY_VALUE
        }
    }
}
