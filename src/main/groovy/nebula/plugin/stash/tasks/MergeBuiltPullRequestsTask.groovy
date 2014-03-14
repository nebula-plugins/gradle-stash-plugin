package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi;
import nebula.plugin.stash.StashRestApiImpl;

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input

class MergeBuiltPullRequestsTask extends DefaultTask {
    StashRestApi stash
    @Input String targetBranch

    @TaskAction
    def mergeBuiltPullRequests() {
        try {
            // for unit testing, don't reset if one is passed in
            stash = !stash ? new StashRestApiImpl(project.stash.repo, project.stash.projectName, project.stash.host, project.stash.user, project.stash.password) : stash
            stash.logger = project.logger
            
            project.logger.info("Finding Pull Requests targeting $targetBranch.")
            List<Map> pullRequests = stash.getPullRequests(targetBranch)
            for (def pr : pullRequests) {
                assert(pr.containsKey("fromRef"))
                assert(pr.fromRef.containsKey("latestChangeset"))
                def originBranch = pr.fromRef.displayId
                project.logger.info("Checking if pull request from $originBranch (pr id ${pr.id}, version ${pr.version}) can be closed. Pulling builds for current commit of branch.")
                List<Map> builds = stash.getBuilds(pr.fromRef.latestChangeset)
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
            project.logger.info("Finished closing built pull requests opened to $targetBranch.")
        }
    }

    public void closeIfNotInProgress(Map pr, build) {
        project.logger.info("closing pull request")
        if (build.state == StashRestApi.SUCCESSFUL_BUILD_STATE) {
            project.logger.info("build.state == SUCCESSFUL")
            // If a successful rpm build then merge and comment
            stash.mergePullRequest([id: pr.id, version: pr.version])
            stash.commentPullRequest(pr.id, "Commit has already been built successfully. See ${build.url}")
        } else if (build.state == StashRestApi.FAILED_BUILD_STATE) {
            project.logger.info("build.state == FAILED")
            // If failed then decline and comment
            stash.declinePullRequest([id: pr.id, version: pr.version])
            stash.commentPullRequest(pr.id, "Commit has already been built and failed. See ${build.url}")
        }
    }
}
