package nebula.plugin.stash

import nebula.plugin.stash.tasks.MergeBuiltPullRequestsTask
import nebula.test.ProjectSpec
import org.gradle.api.tasks.TaskValidationException
import spock.lang.Unroll

class StashTaskTest extends ProjectSpec {
    /**
     * Use a representative stash task to check for a missing input property value.
     */
    @Unroll
    def "Fails if input parameter #nullParamName is null"() {
        when:
        project.apply plugin: 'nebula.gradle-stash'

        MergeBuiltPullRequestsTask task = project.tasks.mergeBuiltPullRequests
        task.stashRepo = stashRepo
        task.stashProject = stashProject
        task.stashUser = stashUser
        task.stashPassword = stashPassword
        task.stashHost = stashHost
        task.targetBranch = "bar"
        task.execute()

        then:
        Throwable t = thrown(TaskValidationException)
        t.cause.message == "No value has been specified for property '$nullParamName'.".toString()

        where:
        stashRepo | stashProject | stashHost | stashUser | stashPassword | nullParamName
        null      | 'myProject'  | 'myHost'  | 'myUser'  | 'myPassword'  | 'stashRepo'
        'myRepo'  | null         | 'myHost'  | 'myUser'  | 'myPassword'  | 'stashProject'
        'myRepo'  | 'myProject'  | null      | 'myUser'  | 'myPassword'  | 'stashHost'
        'myRepo'  | 'myProject'  | 'myHost'  | null      | 'myPassword'  | 'stashUser'
        'myRepo'  | 'myProject'  | 'myHost'  | 'myUser'  | null          | 'stashPassword'
    }
}
