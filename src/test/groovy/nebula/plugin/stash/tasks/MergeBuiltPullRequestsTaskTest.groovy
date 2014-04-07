package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.tasks.MergeBuiltPullRequestsTask;
import nebula.plugin.stash.util.ExternalProcess

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.Before
import org.slf4j.Logger

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class MergeBuiltPullRequestsTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.targetBranch = "bar"
        project.apply plugin: 'gradle-stash'
        assertTrue(project.tasks.mergeBuiltPullRequests instanceof MergeBuiltPullRequestsTask)
    }

    @Test
    public void failsIfStashRepoNotProvided() {
        project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = "foo"
        project.ext.targetBranch = "bar"
        runTaskExpectFail("stashRepo")
    }

    @Test
    public void failsIfStashProjectNotProvided() {
        project.ext.stashRepo = project.ext.stashHost = project.ext.stashUser = project.ext.stashPassword = "foo"
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
    public void failsIfTargetBranchNotProvided() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        runTaskExpectFail("targetBranch")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'gradle-stash'
            project.mergeBuiltPullRequests.execute()
            fail("should have thrown a GradleException")
        } catch (Exception e) {
            assertTrue(e.message ==~ ".*$missingParam.*" || e.cause.message ==~ ".*$missingParam.*")
        }
    }
}

class MergeBuiltPullRequestsTaskFuncTest {
    StashRestApi mockStash
    Project project
    MergeBuiltPullRequestsTask task
    ExternalProcess cmd

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.targetBranch = "bar"
        project.apply plugin: 'gradle-stash'
        mockStash = mock(StashRestApi.class)
        task = project.tasks.mergeBuiltPullRequests
        task.stash = mockStash
        cmd = task.cmd = mock(ExternalProcess.class)
    }

    @Test
    public void mergeBuiltPullRequest() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(project.mergeBuiltPullRequests.targetBranch)).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestChangeset)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        project.tasks.mergeBuiltPullRequests.execute()

        verify(mockStash).getPullRequests(eq(project.mergeBuiltPullRequests.targetBranch))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestChangeset))
        verify(mockStash).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }

    @Test
    public void declineBuiltPullRequest() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.FAILED_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(project.mergeBuiltPullRequests.targetBranch)).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestChangeset)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        project.tasks.mergeBuiltPullRequests.execute()

        verify(mockStash).getPullRequests(eq(project.mergeBuiltPullRequests.targetBranch))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestChangeset))
        verify(mockStash).declinePullRequest([id: pr.id, version: pr.version])
        verify(mockStash).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).mergePullRequest([id: pr.id, version: pr.version])
    }
}

