package nebula.plugin.stash

import nebula.plugin.stash.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class StashRestPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'stash'

    @Override
    void apply(Project project) {
        project.logger.lifecycle "gradle-stash tasks are enabled"
        StashPluginExtension extension = project.extensions.create(EXTENSION_NAME, StashPluginExtension)

        configureStashTasks(project, extension)
        createTasks(project)
    }

    private void configureStashTasks(Project project, StashPluginExtension extension) {
        project.tasks.withType(StashTask) {
            conventionMapping.stashRepo = { project.hasProperty('stashRepo') ? project.stashRepo : extension.stashRepo }
            conventionMapping.stashProject = { project.hasProperty('stashProject') ? project.stashProject : extension.stashProject }
            conventionMapping.stashHost = { project.hasProperty('stashHost') ? project.stashHost : extension.stashHost }
            conventionMapping.stashUser = { project.hasProperty('stashUser') ? project.stashUser : extension.stashUser }
            conventionMapping.stashPassword = { project.hasProperty('stashPassword') ? project.stashPassword : extension.stashPassword }
        }
    }

    private void createTasks(Project project) {
        project.task("mergeBuiltPullRequests", type: MergeBuiltPullRequestsTask) {
            description = "Any pending Pull Request that has been built prior will be merged or declined automatically."
            targetBranch = project.hasProperty('targetBranch') ? project.targetBranch : null
        }

        project.task("syncNextPullRequest", type: SyncNextPullRequestTask) {
            description = "Update a git directory to the branch where the next pull request originates from and apply any merge from master as necessary."
            checkoutDir = project.hasProperty('checkoutDir') ? project.checkoutDir : null
            targetBranch = project.hasProperty('targetBranch') ? project.targetBranch : null
        }

        project.task("closePullRequest", type: ClosePullRequestAfterBuildTask) {
            description = "After a build this task should be run to apply comments and merge the pull request."
            pullRequestVersion = project.hasProperty('pullRequestVersion') ? project.pullRequestVersion.toLong() : null
            pullRequestId = project.hasProperty('pullRequestId') ? project.pullRequestId.toLong() : null
        }

        project.task("addBuildStatus", type: AddBuildStatusTask) {
            description = "Add a build status to a commit."
            buildState = project.hasProperty('buildState') ? project.buildState : null
            buildKey = project.hasProperty('buildKey') ? project.buildKey : null
            buildName = project.hasProperty('buildName') ? project.buildName : null
            buildUrl = project.hasProperty('buildUrl') ? project.buildUrl : null
            buildDescription = project.hasProperty('buildDescription') ? project.buildDescription : null
            buildCommit = project.hasProperty('buildCommit') ? project.buildCommit : null
        }

        project.task("postPullRequest", type: PostPullRequestTask) {
            description = "Post a new pull request."
            prFromBranch = project.hasProperty('prFromBranch') ? project.prFromBranch : null
            prToBranch = project.hasProperty('prToBranch') ? project.prToBranch : null
            prTitle = project.hasProperty('prTitle') ? project.prTitle : null
            prDescription = project.hasProperty('prDescription') ? project.prDescription : null
        }

        project.task("mergeBranch", type: MergeBranchTask) {
            description = "Merge any changes from one branch into another."
            pullFromBranch = project.hasProperty('pullFromBranch') ? project.pullFromBranch : null
            mergeToBranch = project.hasProperty('mergeToBranch') ? project.mergeToBranch : null
            remoteName = project.hasProperty('remoteName') ? project.remoteName : null
            repoUrl = project.hasProperty('repoUrl') ? project.repoUrl : null
            workingPath = project.hasProperty('workingPath') ? project.workingPath : null
            autoMergeBranch = project.hasProperty('autoMergeBranch') ? project.autoMergeBranch : null
            mergeMessage = project.hasProperty('mergeMessage') ? project.mergeMessage : null
            repoName = project.hasProperty('repoName') ? project.repoName : null
            acceptFilter = project.hasProperty('acceptFilter') ? project.acceptFilter : null
        }
    }
}