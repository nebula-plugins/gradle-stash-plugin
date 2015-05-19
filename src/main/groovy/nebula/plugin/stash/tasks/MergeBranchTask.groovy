package nebula.plugin.stash.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.text.SimpleDateFormat

public class MergeBranchTask extends StashTask {
    @Input String pullFromBranch
    String mergeToRepo = stashRepo
    @Input String mergeToBranch
    @Input @Optional String remoteName
    @Input String repoUrl
    @Input String workingPath
    @Input @Optional String autoMergeBranch
    @Input @Optional String mergeMessage
    @Input @Optional String repoName
    @Input @Optional Boolean acceptFilter
    String path
    File pathFile
    File clonePath

    @Override
    void executeStashCommand() {
        remoteName = remoteName ?: "origin"
        autoMergeBranch = autoMergeBranch ?: "automerge-$pullFromBranch-to-$mergeToBranch"
        mergeMessage = mergeMessage ?: "Down-merged branch '$pullFromBranch' into '$mergeToBranch' ($autoMergeBranch)"                                       
        repoName = repoName ?:  inferRepoName(repoUrl)

        String shortPath = workingPath
        String workingPath = "$workingPath/$repoName"

        clonePath = !clonePath ? new File(workingPath) : clonePath
        if (clonePath.exists())
            failTask("Cannot clone. Path already exists '$workingPath'")
        cmd.execute("git clone $repoUrl $repoName", shortPath)
        if(!pathFile && path) {
            pathFile = new File(path)
        }
        def pathFile = pathFile ?: new File(workingPath)
        logger.info "path : ${pathFile}"
        if (!pathFile.exists() || !pathFile.isDirectory())
            failTask("Cannot access git repo path '$workingPath'")
        // make sure auto-merge branch exists on the server, if not, error out
        //https://stash/rest/api/1.0/projects/EDGE/repos/server-fork/branches?base&details&filterText=automerge-dz-testing-to-master&orderBy
        def branches = stash.getBranchesMatching(autoMergeBranch)
        if(branches.size() <= 0) {
            failTask("${autoMergeBranch} must exist on the server before you can run this task")
        }
        cmd.execute("git checkout -t $remoteName/$autoMergeBranch", workingPath)
        cmd.execute("git pull $remoteName $mergeToBranch", workingPath)
        def results = cmd.execute("git pull $remoteName $pullFromBranch", workingPath, true)
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
                    // undo any changes
                    cmd.execute("git reset --hard @{u}", workingPath)
                    // run git command to resolve merge with source
                    def mergeFiles =  cmd.execute("git pull -X theirs --no-edit $remoteName $pullFromBranch", workingPath)
                } else {
                    failTask("Merge conflict merging from '$pullFromBranch' to '$mergeToBranch'\n:$results")
                }                
            } else {
                failTask("Merge conflict merging from '$pullFromBranch' to '$mergeToBranch'\n:$results")
            }
        }
        logger.info("Merge successful.")
        def pushResults = cmd.execute("git push $remoteName $autoMergeBranch", workingPath)
        
        // get the hash of the source and target branches
        // only post a pull request if they are different
        def autoMergeRev = cmd.execute("git rev-parse $remoteName/$autoMergeBranch", workingPath)
        def targetBranchRev = cmd.execute("git rev-parse $remoteName/$mergeToBranch", workingPath)
        
        logger.info("Push successful.")
        if (!(pushResults ==~ /[\s\S]*Everything up-to-date[\s\S]*/) && !autoMergeRev.equals(targetBranchRev)) {
            try {
                stash.postPullRequest(autoMergeBranch, mergeToRepo, mergeToBranch, "Auto merge $pullFromBranch to $mergeToBranch", mergeMessage)
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
            if(pathFile.exists() && !pathFile.deleteDir()) { // this could return false or throw an exception
                failTask("Could not delete git clone directory used for merging : ${pathFile.toString()}")
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
