package nebula.plugin.stash.tasks

import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
class ClosePullRequestAfterBuildTask extends StashTask {
    static final String MESSAGE_CONFLICTED = "Build was successful but unable to merge pull request. Most likely the pull request was modified during the build (new commits or changing status)."
    static final String MESSAGE_SUCCESSFUL = "Build was successful. Merging pull request."
    @Input Long pullRequestVersion
    @Input Long pullRequestId

    @Override
    void executeStashCommand() {
        def targetBranch = "[Unable to determine target branch]"
        def originBranch = "[Unable to determine origin branch]"
        logger.info("Attempting to close Pull Request id $pullRequestId at version $pullRequestVersion.")
        try {
            def pr = stash.mergePullRequest([id:pullRequestId, version:pullRequestVersion])
            targetBranch = pr.toRef.displayId
            originBranch = pr.fromRef.displayId
            stash.commentPullRequest(pullRequestId, MESSAGE_SUCCESSFUL)
            logger.info("Finished processing pull request: ${pullRequestId}")
        } catch (Throwable e) {
            logger.error("Unexpected error in merge process: ${e.dump()}")
            stash.commentPullRequest(pullRequestId, MESSAGE_CONFLICTED)
            throw e
        }
        logger.info("Closed pull request and commented ($targetBranch -> $originBranch)")
    }
}

