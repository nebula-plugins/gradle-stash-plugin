package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.StashRestApiImpl
import nebula.plugin.stash.util.ExternalProcess
import nebula.plugin.stash.util.ExternalProcessImpl
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Abstract Stash task that provides basic functionality needed for all tasks of the plugin.
 */
@DisableCachingByDefault
abstract class StashTask extends DefaultTask {
    @Input
    String stashRepo

    @Input
    String stashProject

    @Input
    String stashHost

    @Input
    String stashUser

    @Input
    String stashPassword

    protected ExternalProcess cmd = new ExternalProcessImpl()
    protected StashRestApi stash

    @TaskAction
    void runAction() {
        stash = createStashClient()
        executeStashCommand()
    }

    private StashRestApi createStashClient() {
        stash ?: new StashRestApiImpl(getStashRepo(), getStashProject(), getStashHost(), getStashUser(), getStashPassword())
    }

    abstract void executeStashCommand()
}
