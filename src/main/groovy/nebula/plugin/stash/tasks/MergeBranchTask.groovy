package nebula.plugin.stash.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.StashRestApiImpl
import nebula.plugin.stash.util.ExternalProcess
import nebula.plugin.stash.util.ExternalProcessImpl

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.text.SimpleDateFormat

public class MergeBranchTask extends DefaultTask {

    ExternalProcess cmd
    StashRestApi stash
    @Input def pullFromBranch
    @Input def mergeToBranch
    @Optional def remoteName
    @Input def repoUrl
    @Input def workingPath
    @Optional def autoMergeBranch
    @Optional def mergeMessage
    @Optional def repoName
    @Optional def acceptFilter
    File path
    File clonePath
    
    public MergeBranchTask(){
        this.cmd = new ExternalProcessImpl()
    }

    @TaskAction
    def mergeBranchTask() {
        final Logger logger = project.logger
        remoteName = remoteName ?: "origin"
        autoMergeBranch = autoMergeBranch ?: "automerge-$pullFromBranch-to-$mergeToBranch"
        mergeMessage = mergeMessage ?: "Down-merged branch '$pullFromBranch' into '$mergeToBranch' ($autoMergeBranch)"                                       
        repoName = repoName ?:  inferRepoName(repoUrl)

        // for unit testing, don't reset if one is passed in
        stash = !stash ? new StashRestApiImpl(project.stash.stashRepo, project.stash.stashProject, project.stash.stashHost, project.stash.stashUser, project.stash.stashPassword) : stash
        stash.logger = project.logger

        String shortPath = workingPath
        String workingPath = "$workingPath/$repoName"

        clonePath = !clonePath ? new File(workingPath) : clonePath
        if (clonePath.exists())
            failTask("Cannot clone. Path already exists '$workingPath'")
        cmd.execute("git clone $repoUrl $repoName", shortPath)
        path = !path ? new File(workingPath) : path
        logger.info "path : ${path.dump()}"
        if (!path.exists() || !path.isDirectory())
            failTask("Cannot access git repo path '$workingPath'")
        // make sure auto-merge branch exists on the server, if not, error out
        //https://stash/rest/api/1.0/projects/EDGE/repos/server-fork/branches?base&details&filterText=automerge-dz-testing-to-master&orderBy
        def branches = stash.getBranchesMatching(autoMergeBranch)
        if(branches.size() <= 0) {
            failTask("${autoMergeBranch} must exist on the server before you can run this task")
        }
        cmd.execute("git checkout -t origin/$autoMergeBranch", workingPath)
        cmd.execute("git pull origin $mergeToBranch", workingPath)
        def results = cmd.execute("git pull origin $pullFromBranch", workingPath)
        if (results ==~ /[\s\S]*Automatic merge failed[\s\S]*/) {
            // get the list of conflicting files
            def conflictingFiles =  cmd.execute("git diff --name-only --diff-filter=U", workingPath)
            if(acceptFilter) {
                boolean acceptSource = true
                conflictingFiles.eachLine {
                    if(!it.endsWith("/${acceptFilter}")) {
                        acceptSource = false
                    }
                }
                if(acceptSource) {
                    logger.info("fixing merge issue by accepting theirs")
                    // run git command to resolve merge with source
                    def mergeFiles =  cmd.execute("git pull -s theirs origin $pullFromBranch", workingPath)                    
                } else {
                    failTask("Merge conflict merging from '$pullFromBranch' to '$mergeToBranch'\n:$results")
                }                
            } else {
                failTask("Merge conflict merging from '$pullFromBranch' to '$mergeToBranch'\n:$results")
            }
        }
        logger.info("Merge successful.")
        def pushResults = cmd.execute("git push origin $autoMergeBranch", workingPath)
        
        // get the hash of the source and target branches
        // only post a pull request if they are different
        def autoMergeRev = cmd.execute("git rev-parse origin/$autoMergeBranch", workingPath)
        def targetBranchRev = cmd.execute("git rev-parse origin/$mergeToBranch", workingPath)
        
        logger.info("Push successful.")
        if (!(pushResults ==~ /[\s\S]*Everything up-to-date[\s\S]*/) && !autoMergeRev.equals(targetBranchRev)) {
            try {
                stash.postPullRequest(autoMergeBranch, mergeToBranch, "Auto merge $pullFromBranch to $mergeToBranch", mergeMessage)
            } catch (Throwable e) {
                logger.error("Problem opening pull request")
                failTask("Problem opening pull request : ${e.getMessage()}")
            }
        } else {
            logger.info("Nothing to merge, not opening a pull request from '$autoMergeBranch' to '$mergeToBranch'.")  
        }
        logger.info("Completed merge from '$pullFromBranch' to '$mergeToBranch'.")
        try {
            // path.exists() is really for working around mocking not mocking deleteDir for testing
            if(path.exists() && !path.deleteDir()) { // this could return false or throw an exception
                failTask("Could not delete git clone directory used for merging : ${path.toString()}")
            }
        } catch (Throwable e) {
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            e.printStackTrace(pw)
            failTask("Could not delete git clone directory used for merging. \n ${sw.toString()}")
        }
    }

    private void failTask(GString failureMsg) {
        logger.error(failureMsg)
        throw new GradleException(failureMsg)
    }

    private String inferRepoName(String repoUrl) {
        String x
        def m = (repoUrl =~ /([^\/]+).git$/)
        if (!m.matches()) {
            def m2 = (repoUrl =~ /^.*\/([^\/]*)$/)
            if (!m2.matches())
                failTask("Cannot get repo name from URL $repoUrl")
            x = m2[0][1]
        }
        else
            x = m[0][1]
        logger.info("Inferred repo name '$x'")
        final String dt = new SimpleDateFormat("yy-MM-dd.HH:mm:ss").format(new Date())
        def dir = x + "." + dt
        logger.info("Using dir '$dir'")
        return dir
    }
}
