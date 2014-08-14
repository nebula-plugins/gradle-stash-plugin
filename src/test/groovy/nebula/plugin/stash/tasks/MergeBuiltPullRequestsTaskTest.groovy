package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.util.ExternalProcess
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.anyString
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class MergeBuiltPullRequestsTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        MergeBuiltPullRequestsTask task = project.tasks.mergeBuiltPullRequests
        assertTrue(task instanceof MergeBuiltPullRequestsTask)
    }

    @Test
    public void canConfigureTargetBranch() {
        MergeBuiltPullRequestsTask task = project.tasks.mergeBuiltPullRequests
        task.targetBranch = "bar"

        assertEquals("bar", task.targetBranch)
    }

    @Test
    public void failsIfTargetBranchNotProvided() {
        MergeBuiltPullRequestsTask task = project.tasks.mergeBuiltPullRequests
        runTaskExpectFail(task, "targetBranch")
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
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(MergeBuiltPullRequestsTask) {
            targetBranch = "bar"
        }

        mockStash = mock(StashRestApi.class)
        task = project.tasks.mergeBuiltPullRequests
        task.stash = mockStash
        cmd = task.cmd = mock(ExternalProcess.class)
    }

    @Test
    public void mergeBuiltPullRequest() {
        def pr = [id:1L, version: 0, fromRef: [latestChangeset: "abc123"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch)).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestChangeset)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestChangeset))
        verify(mockStash).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }

    @Test
    public void dontDeclineFailedBuildPullRequest() { // EDGE-1738 : don't decline reopened PRs
        def pr = [id:1L, version: 0, fromRef: [latestChangeset: "abc123"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.FAILED_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch)).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestChangeset)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestChangeset))
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
        verify(mockStash, never()).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).mergePullRequest([id: pr.id, version: pr.version])
    }
}

