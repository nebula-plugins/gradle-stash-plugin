package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Poll stash pull requests and pick the top one.
 * Then sync/merge/push that branch.
 * @author dzapata
 *
 */
class SyncNextPullRequestTask extends StashTask {
    int consistencyPollRetryCount = 20
    long consistencyPollRetryDeplayMs = 250

    @Input String checkoutDir
    @Input @Optional String targetBranch

    @Override
    void executeStashCommand() {
        String buildPath = project.buildDir.getPath().toString()

        logger.info("checking for open pull requests")
        targetBranch = targetBranch ?:  "master"
        logger.info("Finding Pull Requests targeting $targetBranch.")

        try {
            def allPullReqs = stash.getPullRequests(targetBranch)
            if(allPullReqs.size() <= 0) {
                logger.info("no pull requests to merge")
            }
            
            for (Map pr : allPullReqs)
                if (isValidPullRequest(pr)) {
                    pr = mergeAndSyncPullRequest(pr)
                    setPropertiesFile(pr, buildPath)
                    project.ext.set("pullRequestId", pr.id)
                    project.ext.set("pullRequestVersion", pr.version)
                    project.ext.set("buildCommit", pr.fromRef.latestChangeset.trim())
                    logger.info("Finished processing pull request: {id: ${pr.id}, version: ${pr.version}}")
                    break
                }
        } catch (Throwable e) {
            logger.info("Closing pull request due to failure.")
            throw new GradleException("Unexpected error(s) in sync process: ${e.dump()}")
        } finally {
            logger.info("Finished task SyncNextPullRequestTask.")
        }
    }


    public boolean isValidPullRequest(Map pr) {
        def builds = stash.getBuilds(pr.fromRef.latestChangeset.toString())
        if (pr.fromRef.repository.cloneUrl != pr.toRef.repository.cloneUrl) {
            logger.info("Ignoring pull requests from other fork: $pr")
            return false
        }
        for (Map build : builds)
            if (StashRestApi.RPM_BUILD_KEY == build.key && StashRestApi.INPROGRESS_BUILD_STATE == build.state)
                return false // return true if the pull request does not have an in progress RPM build
        return true
    }

    public Map mergeAndSyncPullRequest(Map pr) {
        logger.info "Processing git sync for pull request id ${pr.id}"
        def fromBranch = pr.fromRef.displayId
        def targetBranch = pr.toRef.displayId
        try {
            logger.info("checkoutDir = ${checkoutDir.dump()}")
            cmd.execute("git fetch origin", checkoutDir)
            cmd.execute("git checkout origin/${fromBranch}", checkoutDir)
            def results = cmd.execute("git merge origin/$targetBranch --commit", checkoutDir)
            if (results ==~ /[\s\S]*Automatic merge failed[\s\S]*/)
                throw new GradleException("Merge conflict merging from '$targetBranch' to '$fromBranch'\n:$results")
            cmd.execute("git push origin HEAD:$fromBranch", checkoutDir)
            def localCommit = cmd.execute("git rev-parse HEAD", checkoutDir).trim()
            def pullReqCommit = pr.fromRef.latestChangeset.trim()
            if (!localCommit.equals(pullReqCommit))
                pr = retryStash(localCommit, pr)
            logger.info "Finished syncing git repo"
            return pr
        } catch (Exception e) {
            logger.info "Error in processing git sync err ${pr.dump()}"
            logger.error("could not execute git commands : " + e.getMessage())
            throw new GradleException("Failure when executing git commands: $e", e)
        }
    }

    public Map retryStash(String localCommit, Map pullRequest) {
        logger.info("Local latest commit does not match Pull Request latest commit. Polling Stash for consistency with commit: ${localCommit}")
        def fromBranch = pullRequest.fromRef.displayId
        def timeout = System.currentTimeMillis() + consistencyPollRetryDeplayMs
        def stashCommit
        for (int retryCount = 0; retryCount < consistencyPollRetryCount; retryCount++) {
            if (timeout > System.currentTimeMillis()) continue
            timeout = System.currentTimeMillis() + consistencyPollRetryDeplayMs
            def updatedPR = stash.getPullRequest(Integer.parseInt(pullRequest.id))
            stashCommit = updatedPR.fromRef.latestChangeset.trim()
            logger.info("Comparing stash head commit '$stashCommit' to local head commit '$localCommit'")
            if (stashCommit == localCommit)
                return updatedPR
        }
        throw new GradleException("Stash has not asynchronously updated git repo with changes to pull request. $stashCommit != $localCommit")
    }

    private void setPropertiesFile(Map pr, String buildPath) {
        def propPath = buildPath + "/pull-request.properties"
        logger.info "Writing pull request data to $propPath using ${pr.dump()}"
        Properties prop = new Properties();
        prop.setProperty("pullRequestId", Integer.toString(pr.id));
        prop.setProperty("pullRequestVersion", Integer.toString(pr.version));
        prop.setProperty("pullRequestSourceBranch", pr.fromRef.displayId);
        prop.setProperty("pullRequestTargetBranch", pr.toRef.displayId);
        prop.setProperty("buildCommit", pr.fromRef.latestChangeset.trim());
        
        logger.info "set properties"
        try {
            File f = new File(propPath)
            if (!f.exists()) {
                new File(f.getParent()).mkdirs()
                f.createNewFile()
            }
            prop.store(new FileOutputStream(f), null);
        } catch (IOException e) {
            logger.error("Could not write to properties file : " + e.getMessage())
            throw new GradleException("Cannot write properties file", e)
        }
        logger.info "Finished saving pull request details to properties file"
    }
}
