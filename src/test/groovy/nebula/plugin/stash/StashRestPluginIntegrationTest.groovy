package nebula.plugin.stash

import nebula.plugin.stash.StashRestApi;
import nebula.plugin.stash.StashRestApiImpl
import nebula.test.ProjectSpec;
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

class StashRestPluginIntegrationTest extends ProjectSpec {

    //@Test
    public void closeSuccessfullyBuiltPullRequest() {
        def stash = new StashRestApiImpl()
        //def failedBuild = [state:"FAILED", key:StashRestApi.RPM_BUILD_KEY, name:StashRestApi.RPM_BUILD_KEY + "-45", url:"http://google.com", description:"build failed"]
        //stash.postBuildStatus("1f98fba51eafd5d3184e10ccdbb4a615545d5464", failedBuild)
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.mergeBuiltPullRequests.execute()
    }

    //@Test
    public void mergePullReqAfterBuild() {
        project.targetBranch = "master2"
        project.checkoutDir = "/Users/atull/dev/git/server-fork"
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.syncNextPullRequest.execute()
        project.ext.set("buildState", "INPROGRESS")
        project.ext.set("key", StashRestApi.RPM_BUILD_KEY)
        project.ext.set("url", "http://google.com")
        project.tasks.addBuildStatus.execute()
        // build (imagine here)
        project.ext.set("buildState", "SUCCESSFUL")
        project.tasks.addBuildStatus.execute()
    }

/*
	@Test
	public void addMergeBuiltPullRequestsTaskToProject() {
		project.apply plugin: 'nebula.gradle-stash'
		assertTrue(project.tasks.mergeBuiltPullRequests instanceof MergeBuiltPullRequestsTask)
	}
    
    @Test
    public void executeMergeBuiltPullRequestsTask() {
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.mergeBuiltPullRequests.execute()
    }
    
    @Test
    public void addSyncNextPullRequestTaskToProject() {
        project.apply plugin: 'nebula.gradle-stash'
        assertTrue(project.tasks.syncNextPullRequest instanceof SyncNextPullRequestTask)
    }

    @Test
    public void executeSyncNextPullRequestTaskMissingCheckoutDir() {
        assertFalse(project.hasProperty("checkoutDir"))
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.syncNextPullRequest.execute()
    }
    
    @Test
    public void executeSyncNextPullRequestTask() {
        project.checkoutDir = "/Users/dzapata/code/server-fork"
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.syncNextPullRequest.execute()
    }
    
    @Test
    public void addPostNewDeploymentPullRequestTaskToProject() {
        project.apply plugin: 'nebula.gradle-stash'
        assertTrue(project.tasks.postNewDeploymentPullRequest instanceof PostNewDeploymentPullRequestTask)
    }

    @Test
    public void executePostNewDeploymentPullRequestTask() {
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.postNewDeploymentPullRequest.execute()
    }
    */
    /*
    @Test
    public void addAddBuildStatusTaskToProject() {
        project.apply plugin: 'nebula.gradle-stash'
        assertTrue(project.tasks.addBuildStatus instanceof AddBuildStatus)
    }

*/                                     /*
    //@Test
    public void executeAddBuildStatusTaskWithCurrentCommit() {
        project.apply plugin: 'nebula.gradle-stash'
        project.buildState = 'SUCCESSFUL'
        project.buildKey = StashRestApi.RPM_BUILD_KEY
        project.buildName = "FOO BAR BUILD NAME"
        project.buildUrl = "http://builds.netflix.com/job/EDGE-Master-FMLY/419/myConsole"
        project.buildDescription = "My build description"
        project.stashRepo = "atf-gradle-plugin"
        project.stashProject = "EDGE"
        try {
            project.tasks.addBuildStatus.execute()
        } catch (Exception e) {
            fail("this should have worked, maybe unable to find the current commit sha, or working dir is not a stash repo : ${e.dump()}")
        }
    }
    
    //@Test
    public void executeAddBuildStatusTaskWithCommitDetection() {
        project.apply plugin: 'nebula.gradle-stash'
        project.buildCommit = "f18e86bbd850806114035fd498c0f317e4e89a4f"
        project.buildState = 'SUCCESSFUL'
        project.buildKey = StashRestApi.RPM_BUILD_KEY
        project.buildName = "FOO BAR BUILD NAME"
        project.buildUrl = "http://builds.netflix.com/job/EDGE-Master-FMLY/419/myConsole"
        project.buildDescription = "My build description"
        project.stashRepo = "atf-gradle-plugin"
        project.stashProject = "EDGE"
        try {
            project.tasks.addBuildStatus.execute()
        } catch (Exception e) {
            fail("expected task to pass since we provide a proper commit hash ${e.dump()}")
        }
    }
    
    //@Test
    public void executeAddBuildStatusTaskFail() {
        project.apply plugin: 'nebula.gradle-stash'
        project.buildCommit = "FOOBAR"
        project.buildState = 'SUCCESSFUL'
        project.buildKey = StashRestApi.RPM_BUILD_KEY
        project.buildName = "FOO BAR BUILD NAME"
        project.buildUrl = "http://builds.netflix.com/job/EDGE-Master-FMLY/419/myConsole"
        project.buildDescription = "My build description"
        try {
            project.tasks.addBuildStatus.execute()
            fail("expected task to fail since the commit hash is the short version")
        } catch (Exception e) {
        
        }
    }
    
    @Test
    public void addPostPullRequestTaskToProject() {
        project.apply plugin: 'nebula.gradle-stash'
        assertTrue(project.tasks.postPullRequest instanceof PostNewPullRequestTask)
    }

    @Test
    public void executePostPullRequestTask() {
        project.apply plugin: 'nebula.gradle-stash'
        project.stashRepo = "server-pipeline"
        project.stashProject = "EDGE"
        project.prFromBranch = "dzapata"
        project.prToBranch = "master"
        project.prTitle = "title"
        project.prDescription = "description"
        project.tasks.postPullRequest.execute()
    }
 */
    //@Test
    public void mergeBranchTask() {
        project.apply plugin: 'nebula.gradle-stash'

        project.ext.set("pullFromBranch", "a")
        project.ext.set("mergeToBranch", "master")
        project.ext.set("remoteName", "origin")
        project.ext.set("repoUrl", "/Users/atull/gitTest")
        project.ext.set("workingPath", "/Users/atull/tmp")

        project.tasks.mergeBranches.execute()
    }

}