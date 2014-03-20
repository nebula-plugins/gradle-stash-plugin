package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi;
import nebula.plugin.stash.StashRestApiImpl;

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input

class ClosePullRequestAfterBuildTask extends DefaultTask{
    
    public static String MESSAGE_CONFLICTED = "Build was successful but unable to merge pull request. Most likely the pull request was modified during the build (new commits or changing status)."
    public static String MESSAGE_SUCCESSFUL = "Build was successful. Merging pull request."
    StashRestApi stash
    @Input int pullRequestVersion
    @Input int pullRequestId
    
    @TaskAction
    def closePullRequestAfterBuild() {
        // for unit testing, don't reset if one is passed in
        stash = !stash ? new StashRestApiImpl(project.stash.repo, project.stash.projectName, project.stash.host, project.stash.user, project.stash.password) : stash
        stash.logger = project.logger

        def targetBranch = "[Unable to determine target branch]"
        def originBranch = "[Unable to determine origin branch]"
        project.logger.info("Attempting to close Pull Request id $pullRequestId at version $pullRequestVersion.")
        try {
            def pr = stash.mergePullRequest([id:pullRequestId, version:pullRequestVersion])
            targetBranch = pr.toRef.displayId
            originBranch = pr.fromRef.displayId
            stash.commentPullRequest(pullRequestId, MESSAGE_SUCCESSFUL)
            project.logger.info("Finished processing pull request: ${pullRequestId}")
        } catch (Throwable e) {
            project.logger.error("Unexpected error in merge process: ${e.dump()}")
            stash.commentPullRequest(pullRequestId, MESSAGE_CONFLICTED)
            throw e
        }
        project.logger.info("Closed pull request and commented ($targetBranch -> $originBranch)")
    }
}

