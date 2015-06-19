package nebula.plugin.stash.tasks

import org.gradle.api.tasks.Input

/**
 * Given a commit and a target branch:
 * 1) Determine if the commit exists on the target branch
 * 2) Determine which branch the commit lives on
 * 3) If the commit is the head of that branch, open a pull request to get the commit on the target branch if one isn't already opened
 */
class OpenPostPullRequestIfNotOnBranchTask extends StashTask {
    @Input String prCommit
    String prToRepo = stashRepo
    @Input String prToBranch
    @Input String prTitle
    @Input String prDescription
    String prFromBranch

    @Override
    void executeStashCommand() {
        try {
            def branches = stash.getBranchInfo(prCommit)
            boolean onTargetBranch = false
            branches.each { // find a branch with this commit
                if(it.displayId == prToBranch) {
                    onTargetBranch = true
                } else {
                    prFromBranch = it.displayId
                }
            }

            if(branches.size() == 0) {
                throw new RuntimeException("could not determine source branch for this commit : ${prCommit}")
            } else if (branches.size() > 1 && !onTargetBranch) {
                throw new RuntimeException("multiple (non-${prToBranch}) branches have this commit : ${prCommit}, branches : ${branches}")
            } else if (!onTargetBranch) {
                def matchingBranches = stash.getBranchesMatching(prFromBranch)
                Map result = matchingBranches.find {
                    it.displayId == prFromBranch
                }
                assert result.latestChangeset.startsWith(prCommit) // prCommit may be the short commit hash, so account for that

                // make sure there isn't already a PR for this
                def openPrs = stash.getPullRequests(prToBranch, "OPEN", null)
                Map matchingPr = openPrs.find {
                    it.fromRef.id == "refs/heads/${prFromBranch}"
                }

                if(openPrs.size() == 0 || !matchingPr) {
                    logger.info("posting a pull request : ${prFromBranch} ${prToBranch} ${prTitle} ${prDescription}")
                    def postPrResult = stash.postPullRequest(prFromBranch, prToRepo, prToBranch, prTitle, prDescription)
                    logger.info("post PR response : ${postPrResult}")
                    logger.info("successfully posted PR : ${postPrResult.link.url}")
                } else {
                    logger.info("there is already a PR open for this source (${prFromBranch})and target branch (${prToBranch})")
                }
            } else {
                logger.info("this commit (${prCommit}) is already on the target branch (${prToBranch}), nothing to do")
            }

            logger.info "Finished oenPostPullRequestIfNotOnBranchTask"
        } catch (Throwable e) {
            logger.error "Unexpected error in openPostPullRequestIfNotOnBranchTask"
            e.printStackTrace()
            throw e
        }
    }
}
