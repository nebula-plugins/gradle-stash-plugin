package nebula.plugin.stash.tasks

import org.gradle.api.tasks.Input

class PostPullRequestTask extends StashTask {
    @Input String prFromBranch
    String prToRepo = stashRepo
    @Input String prToBranch
    @Input String prTitle
    @Input String prDescription

    @Override
    void executeStashCommand() {
        try {
            def pr = stash.postPullRequest(prFromBranch, prToRepo, prToBranch, prTitle, prDescription)
            logger.info "Finished postPullRequest: ${pr.dump()}"
        } catch (Throwable e) {
            logger.error "Unexpected error in postPullRequest"
            e.printStackTrace()
            throw e
        }
    }
}
