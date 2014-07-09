package nebula.plugin.stash

import nebula.test.ProjectSpec
import org.gradle.api.Task

class StashRestPluginTest extends ProjectSpec {
    def setup() {
        project.apply plugin: 'gradle-stash'
    }

    def "Creates custom extension with default values"() {
        expect:
        StashPluginExtension extension = project.extensions.findByName(StashRestPlugin.EXTENSION_NAME)
        extension != null
        extension.stashRepo == null
        extension.stashProject == null
        extension.stashHost == null
        extension.stashUser == null
        extension.stashPassword == null
    }

    def "Creates default tasks"() {
        expect:
        Task mergeBuiltPullRequestsTask = findTask('mergeBuiltPullRequests')
        mergeBuiltPullRequestsTask.description == 'Any pending Pull Request that has been built prior will be merged or declined automatically.'
        Task syncNextPullRequestTask = findTask('syncNextPullRequest')
        syncNextPullRequestTask.description == 'Update a git directory to the branch where the next pull request originates from and apply any merge from master as necessary.'
        Task closePullRequestTask = findTask('closePullRequest')
        closePullRequestTask.description == 'After a build this task should be run to apply comments and merge the pull request.'
        Task addBuildStatusTask = findTask('addBuildStatus')
        addBuildStatusTask.description == 'Add a build status to a commit.'
        Task postPullRequestTask = findTask('postPullRequest')
        postPullRequestTask.description == 'Post a new pull request.'
        Task mergeBranchTask = findTask('mergeBranch')
        mergeBranchTask.description == 'Merge any changes from one branch into another.'
    }

    private Task findTask(String taskName) {
        project.tasks.findByName(taskName)
    }
}
