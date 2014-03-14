package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi;
import nebula.plugin.stash.StashRestApiImpl;

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input

class PostPullRequestTask extends DefaultTask{
    StashRestApi stash
    @Input prFromBranch
    @Input prToBranch
    @Input prTitle
    @Input prDescription
    
    @TaskAction
    def postPullRequest() {
        // for unit testing, don't reset if one is passed in
        stash = !stash ? new StashRestApiImpl(project.stash.repo, project.stash.project, project.stash.host, project.stash.user, project.stash.password) : stash
        stash.logger = project.logger

        try {
            def pr = stash.postPullRequest(prFromBranch, prToBranch, prTitle, prDescription)
            project.logger.info "Finished postPullRequest: ${pr.dump()}"
        } catch (Throwable e) {
            project.logger.error "Unexpected error in postPullRequest"
            it.printStackTrace()
            throw e
        }
    }
}
