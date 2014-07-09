package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import org.gradle.api.tasks.Input

class MergeBuiltPullRequestsTask extends StashTask {
    @Input String targetBranch

    @Override
    void executeStashCommand() {
        try {
            project.logger.info("Finding Pull Requests targeting $targetBranch.")
            List<Map> pullRequests = stash.getPullRequests(targetBranch)
            for (def pr : pullRequests) {
                println "PR : ${pr.dump()}"
                assert(pr.containsKey("fromRef"))
                assert(pr.fromRef.containsKey("latestChangeset"))
                def originBranch = pr.fromRef.displayId
                project.logger.info("Checking if pull request from $originBranch (pr id ${pr.id}, version ${pr.version}) can be closed. Pulling builds for current commit of branch.")
                List<Map> builds = stash.getBuilds(pr.fromRef.latestChangeset)
                if(builds.size() == 0) {
                    project.logger.info("pr id ${pr.id}, version ${pr.version} has no builds, so it can't be closed")
                }
                
                for (Map b : builds) {
                    project.logger.info("build: ${b.key} =? ${StashRestApi.RPM_BUILD_KEY}")
                    if (StashRestApi.RPM_BUILD_KEY == b.key) {
                        project.logger.info("Pull Request ${pr.id} has an RPM Build.")
                        closeIfNotInProgress(pr, b)
                        project.logger.info("Finished closing pull request: ${pr.dump()}.")
                    }
                }
            }
        } catch (Throwable e) {
            project.logger.error("Unexpected error in pull requests: ${e.dump()}")
            e.printStackTrace()
            throw e
        } finally {
            project.logger.info("Finished closing eligible built pull requests opened to $targetBranch.")
        }
    }

    public void closeIfNotInProgress(Map pr, build) {
        project.logger.info("build.state == ${build.state}")
        
        if (build.state == StashRestApi.SUCCESSFUL_BUILD_STATE) {
            // If a successful rpm build then merge and comment
            stash.mergePullRequest([id: pr.id, version: pr.version])
            stash.commentPullRequest(pr.id, "Commit has already been built successfully. See ${build.url}")
        } else if (build.state == StashRestApi.FAILED_BUILD_STATE) {
            // If failed then decline and comment
            stash.declinePullRequest([id: pr.id, version: pr.version])
            stash.commentPullRequest(pr.id, "Commit has already been built and failed. See ${build.url}")
        } else if (build.state == StashRestApi.INPROGRESS_BUILD_STATE) {
            project.logger.info("can't close a pull request with a build in progress")
        }
    }
}
