package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.util.ExternalProcess
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class SyncNextPullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.apply plugin: 'gradle-stash'
        assertTrue(project.tasks.syncNextPullRequest instanceof SyncNextPullRequestTask)
    }

    @Test
    public void failsIfStashRepoNotProvided() {
        project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.checkoutDir = "foo"
        project.ext.targetBranch = "bar"
        runTaskExpectFail("stashRepo")
    }

    @Test
    public void failsIfStashProjectNameNotProvided() {
        project.ext.stashRepo = project.ext.stashUser = project.ext.stashPassword = project.ext.checkoutDir = "foo"
        project.ext.targetBranch = "foo"
        runTaskExpectFail("stashProject")
   }

    @Test
    public void canConfigureTargetBranch() {       
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.targetBranch = "bar"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("bar", project.tasks.mergeBuiltPullRequests.targetBranch)
    }

    @Test
    public void failsIfCheckoutDirNotProvided() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        runTaskExpectFail("checkoutDir")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'gradle-stash'
            project.syncNextPullRequest.execute()
            fail("should have thrown a GradleException")
        } catch (Exception e) {
            assertEquals("No value has been specified for property '$missingParam'.".toString(), e.cause.message)
        }
    }
}

class SyncNextPullRequestTaskFunctionalTest {
    StashRestApi mockStash
    Project project
    SyncNextPullRequestTask task
    ExternalProcess cmd
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.extensions.checkoutDir = "/foo/bar"
        project.apply plugin: 'gradle-stash'
        mockStash = mock(StashRestApi.class)
        task = project.tasks.syncNextPullRequest
        task.stash = mockStash
        cmd = task.cmd = mock(ExternalProcess.class)
    }
    
    @Test
    public void syncNextPullRequest() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId"], toRef: [latestChangeset: "def456", displayId: "toDisplayId"]]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString())).thenReturn([pr])
        when(mockStash.getPullRequest(anyInt())).thenReturn(pr)
        task.execute()
    }
    
    @Test(expected=GradleException.class)
    public void syncNextPullRequestGetPrsFails() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId"], toRef: [latestChangeset: "def456", displayId: "toDisplayId"]]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString())).thenThrow(new GradleException("mock exception"))
        task.execute()
    }
    
    @Test
    public void syncNextPullRequestInvalidPr() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId"], toRef: [latestChangeset: "def456", displayId: "toDisplayId"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.INPROGRESS_BUILD_STATE, url: "http://netflix.com/"]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getBuilds(anyString())).thenReturn([build])
        when(mockStash.getPullRequests(anyString())).thenReturn([pr])
        task.execute()
        verify(mockStash).getBuilds(anyString())
    }
    
    @Test(expected=GradleException.class)
    public void syncNextPullRequestUnableToMerge() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId"], toRef: [latestChangeset: "def456", displayId: "toDisplayId"]]
        when(cmd.execute(anyString(), anyString())).thenReturn("Automatic merge failed")
        when(mockStash.getPullRequests(anyString())).thenReturn([pr])
        task.execute()
    }
}


