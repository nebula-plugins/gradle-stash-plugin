package nebula.plugin.stash

import nebula.plugin.stash.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class StashRestPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'stash'

    @Override
    void apply(Project project) {
        project.plugins.apply(StashRestBasePlugin)
        project.logger.debug "gradle-stash tasks are enabled"
        createTasks(project)
    }

    private void createTasks(Project project) {
        project.task("mergeBuiltPullRequests", type: MergeBuiltPullRequestsTask) {
            description = "Any pending Pull Request that has been built prior will be merged or declined automatically."
        }

        project.task("syncNextPullRequest", type: SyncNextPullRequestTask) {
            description = "Update a git directory to the branch where the next pull request originates from and apply any merge from master as necessary."
        }

        project.task("closePullRequest", type: ClosePullRequestAfterBuildTask) {
            description = "After a build this task should be run to apply comments and merge the pull request."
        }

        project.task("addBuildStatus", type: AddBuildStatusTask) {
            description = "Add a build status to a commit."
        }

        project.task("postPullRequest", type: PostPullRequestTask) {
            description = "Post a new pull request."
        }

        project.task("mergeBranch", type: MergeBranchTask) {
            description = "Merge any changes from one branch into another."
        }

        project.task("openPostPullRequestIfNotOnBranchTask", type: OpenPostPullRequestIfNotOnBranchTask) {
            description = "Open a pull request from one branch to another if it contains a certain commit"
        }
    }
}