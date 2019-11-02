package nebula.plugin.stash

import nebula.plugin.stash.tasks.StashTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class StashRestBasePlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'stash'

    @Override
    void apply(Project project) {
        StashPluginExtension extension = project.extensions.create(EXTENSION_NAME, StashPluginExtension)
        configureStashTasks(project, extension)
    }

    private void configureStashTasks(Project project, StashPluginExtension extension) {
        project.tasks.withType(StashTask).configureEach(new Action<StashTask>() {
            @Override
            void execute(StashTask stashTask) {
                stashTask.conventionMapping.stashRepo = { project.hasProperty('stashRepo') ? project.stashRepo : extension.stashRepo }
                stashTask. conventionMapping.stashProject = { project.hasProperty('stashProject') ? project.stashProject : extension.stashProject }
                stashTask. conventionMapping.stashHost = { project.hasProperty('stashHost') ? project.stashHost : extension.stashHost }
                stashTask.conventionMapping.stashUser = { project.hasProperty('stashUser') ? project.stashUser : extension.stashUser }
                stashTask.conventionMapping.stashPassword = { project.hasProperty('stashPassword') ? project.stashPassword : extension.stashPassword }
            }
        })
    }
}
