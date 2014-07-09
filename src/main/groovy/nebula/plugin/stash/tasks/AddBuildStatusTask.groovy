package nebula.plugin.stash.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class AddBuildStatusTask extends StashTask {
    @Input String buildState
    @Input String buildKey
    @Input String buildName
    @Input String buildUrl
    @Input String buildDescription
    @Input @Optional String buildCommit
    
    /**
     * Find the hash of the current commit in your current working directory
     * @return The commit hash if found, Null if not
     */
    def getCurrentCommit() {
        logger.info("getting the sha for the HEAD of the current directory")
        def currentSha = cmd.execute("git rev-parse HEAD", System.getProperty("user.dir"))
        logger.info("currentSha : ${currentSha}")
        return currentSha
    }
    
    @Override
    void executeStashCommand() {
        def commit
        
        if(buildCommit) {
            commit = buildCommit
        } else {
            logger.info("finding commit")
            commit = getCurrentCommit()
            if(!commit) {
                throw new GradleException("unable to determine the commit hash")
            }
        }
        logger.info("using commit : ${commit}")
        stash.postBuildStatus(commit, [state:buildState, key:buildKey, name:buildName, url:buildUrl, description:project.buildDescription])
    } 
}