package nebula.plugin.stash

import nebula.plugin.stash.tasks.MergeBuiltPullRequestsTask
import nebula.test.ProjectSpec

class StashRestBasePluginTest extends ProjectSpec {
    def setup() {
        project.apply plugin: 'gradle-stash-base'
    }

    def "Can create task of type StashTask"() {
        given:
        String givenStashRepo = 'myRepo'
        String givenStashProject = 'example'
        String givenStashHost = 'mytesthost'
        String givenStashUser = 'foobar'
        String givenStashPassword = 'qwerty'
        String givenTargetBranch = 'master'

        when:
        project.task('mergePullRequest', type: MergeBuiltPullRequestsTask) {
            stashRepo = givenStashRepo
            stashProject = givenStashProject
            stashHost = givenStashHost
            stashUser = givenStashUser
            stashPassword = givenStashPassword
            targetBranch = givenTargetBranch
        }

        then:
        MergeBuiltPullRequestsTask task = project.tasks.getByName('mergePullRequest')
        task.stashRepo == givenStashRepo
        task.stashProject == givenStashProject
        task.stashHost == givenStashHost
        task.stashUser == givenStashUser
        task.stashPassword == givenStashPassword
        task.targetBranch == givenTargetBranch
    }

    def "Can create task of type StashTask partly configured via extension"() {
        given:
        String givenStashRepo = 'myRepo'
        String givenStashProject = 'example'
        String givenStashHost = 'mytesthost'
        String givenStashUser = 'foobar'
        String givenStashPassword = 'qwerty'
        String givenTargetBranch = 'master'

        when:
        project.stash {
            stashRepo = givenStashRepo
            stashProject = givenStashProject
            stashHost = givenStashHost
            stashUser = givenStashUser
            stashPassword = givenStashPassword
        }

        project.task('mergePullRequest', type: MergeBuiltPullRequestsTask) {
            targetBranch = givenTargetBranch
        }

        then:
        MergeBuiltPullRequestsTask task = project.tasks.getByName('mergePullRequest')
        task.stashRepo == givenStashRepo
        task.stashProject == givenStashProject
        task.stashHost == givenStashHost
        task.stashUser == givenStashUser
        task.stashPassword == givenStashPassword
        task.targetBranch == givenTargetBranch
    }
}
