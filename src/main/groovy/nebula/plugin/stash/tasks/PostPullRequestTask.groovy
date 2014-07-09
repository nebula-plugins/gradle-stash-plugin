package nebula.plugin.stash.tasks

import org.gradle.api.tasks.Input

class PostPullRequestTask extends StashTask {
    @Input prFromBranch
    @Input prToBranch
    @Input prTitle
    @Input prDescription

    @Override
    void executeStashCommand() {
        try {
            def pr = stash.postPullRequest(prFromBranch, prToBranch, prTitle, prDescription)
            project.logger.info "Finished postPullRequest: ${pr.dump()}"
        } catch (Throwable e) {
            project.logger.error "Unexpected error in postPullRequest"
            e.printStackTrace()
            throw e
        }
    }
}
