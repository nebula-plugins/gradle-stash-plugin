package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import org.gradle.api.tasks.Input

class MergeBuiltPullRequestsTask extends StashTask {
    @Input String targetBranch

    @Override
    void executeStashCommand() {
        try {
            logger.info("Finding Pull Requests targeting $targetBranch.")
            List<Map> pullRequests = stash.getPullRequests(targetBranch, "OPEN", "OLDEST")
            for (def pr : pullRequests) {
                println "PR : ${pr.dump()}"
                assert(pr.containsKey("fromRef"))
                assert(pr.fromRef.containsKey("latestChangeset"))
                def originBranch = pr.fromRef.displayId
                logger.info("Checking if pull request from $originBranch (pr id ${pr.id}, version ${pr.version}) can be closed. Pulling builds for current commit of branch.")
                List<Map> builds = stash.getBuilds(pr.fromRef.latestChangeset)
                if(builds.size() == 0) {
                    logger.info("pr id ${pr.id}, version ${pr.version} has no builds, so it can't be closed")
                }
                
                for (Map b : builds) {
                    logger.info("build: ${b.key} =? ${StashRestApi.RPM_BUILD_KEY}")
                    if (StashRestApi.RPM_BUILD_KEY == b.key) {
                        logger.info("Pull Request ${pr.id} has an RPM Build.")
                        closeIfNotInProgress(pr, b)
                        logger.info("Finished closing pull request: ${pr.dump()}.")
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Unexpected error in pull requests: ${e.dump()}")
            e.printStackTrace()
            throw e
        } finally {
            logger.info("Finished closing eligible built pull requests opened to $targetBranch.")
        }
    }

    public void closeIfNotInProgress(Map pr, build) {
        logger.info("build.state == ${build.state}")
        
        if (build.state == StashRestApi.SUCCESSFUL_BUILD_STATE) {
            for (Map reviewer : pr.reviewers) {
                if(!reviewer.approved) {
                    logger.info("reviewer has not approved the PR : ${reviewer.user.displayName}")
                    return
                }
            }            // If a successful rpm build then merge and comment
            stash.mergePullRequest([id: pr.id, version: pr.version])
            stash.commentPullRequest(pr.id, "Commit has already been built successfully. See ${build.url}")
        } else if (build.state == StashRestApi.FAILED_BUILD_STATE) {
            // EDGE-1738 : don't decline reopened PRs
            //stash.declinePullRequest([id: pr.id, version: pr.version])
            //stash.commentPullRequest(pr.id, "Commit has already been built and failed. See ${build.url}")
        } else if (build.state == StashRestApi.INPROGRESS_BUILD_STATE) {
            logger.info("can't close a pull request with a build in progress")
        }
    }
}
