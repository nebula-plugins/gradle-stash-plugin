package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.util.ExternalProcess
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class SyncNextPullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        assertTrue(project.tasks.syncNextPullRequest instanceof SyncNextPullRequestTask)
    }

    @Test
    public void canConfigureTargetBranch() {
        SyncNextPullRequestTask task = project.tasks.syncNextPullRequest
        task.targetBranch = "bar"

        assertEquals("bar", task.targetBranch)
    }

    @Test
    public void failsIfCheckoutDirNotProvided() {
        SyncNextPullRequestTask task = project.tasks.syncNextPullRequest
        runTaskExpectFail(task, "checkoutDir")
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
        setDummyStashTaskPropertyValues(project)
        project.apply plugin: 'gradle-stash'
        mockStash = mock(StashRestApi.class)
        task = project.tasks.syncNextPullRequest
        task.checkoutDir = "/foo/bar"
        task.stash = mockStash
        cmd = task.cmd = mock(ExternalProcess.class)
    }
    
    @Test
    public void syncNextPullRequest() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]], toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]]]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString())).thenReturn([pr])
        when(mockStash.getPullRequest(anyInt())).thenReturn(pr)
        task.execute()
    }
    
    @Test(expected=GradleException.class)
    public void syncNextPullRequestGetPrsFails() {
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString())).thenThrow(new GradleException("mock exception"))
        task.execute()
    }
    
    @Test
    public void syncNextPullRequestInvalidPr() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]], toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.INPROGRESS_BUILD_STATE, url: "http://netflix.com/"]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getBuilds(anyString())).thenReturn([build])
        when(mockStash.getPullRequests(anyString())).thenReturn([pr])
        task.execute()
        verify(mockStash).getBuilds(anyString())
    }
    
    @Test(expected=GradleException.class)
    public void syncNextPullRequestUnableToMerge() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]], toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]]]
        when(cmd.execute(anyString(), anyString())).thenReturn("Automatic merge failed")
        when(mockStash.getPullRequests(anyString())).thenReturn([pr])
        task.execute()
    }
}


