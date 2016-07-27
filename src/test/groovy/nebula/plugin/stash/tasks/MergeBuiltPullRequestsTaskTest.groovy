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
        project.apply plugin: 'nebula.gradle-stash'
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
        project.apply plugin: 'nebula.gradle-stash'
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
        def pr = [id:1L, version: 0, fromRef: [latestCommit: "abc123"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch, "OPEN", "OLDEST")).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestCommit)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch), eq("OPEN"), eq("OLDEST"))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestCommit))
        verify(mockStash).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }

    @Test
    public void dontDeclineFailedBuildPullRequest() { // EDGE-1738 : don't decline reopened PRs
        def pr = [id:1L, version: 0, fromRef: [latestCommit: "abc123"]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.FAILED_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch, "OPEN", "OLDEST")).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestCommit)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch), eq("OPEN"), eq("OLDEST"))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestCommit))
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
        verify(mockStash, never()).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).mergePullRequest([id: pr.id, version: pr.version])
    }
    
    @Test
    public void mergeBuiltPullRequestWithSingleReviewerNotApproved() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestCommit: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestCommit: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                    reviewers : [[approved : false, user : [displayName : "Bob Reviewer"]]]
        ]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch, "OPEN", "OLDEST")).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestCommit)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch), eq("OPEN"), eq("OLDEST"))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestCommit))
        verify(mockStash, never()).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash, never()).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }

    @Test
    public void syncNextPullRequestWithMultipleReviewersNotApproved() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestCommit: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestCommit: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]], [approved : false, user : [displayName : "Joe Reviewer"]]
                           ]
        ]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch, "OPEN", "OLDEST")).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestCommit)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch), eq("OPEN"), eq("OLDEST"))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestCommit))
        verify(mockStash, never()).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash, never()).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }

    @Test
    public void syncNextPullRequestWithSingleReviewerApproved() { //nothing should get processed, task should pass
        def pr = [id:1L, version: 0, fromRef: [latestCommit: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestCommit: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]]]
        ]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch, "OPEN", "OLDEST")).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestCommit)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch), eq("OPEN"), eq("OLDEST"))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestCommit))
        verify(mockStash).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }

    @Test
    public void syncNextPullRequestWithMultipleReviewersApproved() { //nothing should get processed, task should pass
        def pr = [id:1L, version: 0, fromRef: [latestCommit: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestCommit: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]], [approved : true, user : [displayName : "Joe Reviewer"]]
                  ]
        ]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.SUCCESSFUL_BUILD_STATE, url: "http://netflix.com/"]
        when(mockStash.getPullRequests(task.targetBranch, "OPEN", "OLDEST")).thenReturn([pr])
        when(mockStash.getBuilds(pr.fromRef.latestCommit)).thenReturn([build])
        when(mockStash.mergePullRequest([id: pr.id, version: pr.version])).thenReturn(null)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)

        task.execute()

        verify(mockStash).getPullRequests(eq(task.targetBranch), eq("OPEN"), eq("OLDEST"))
        verify(mockStash).getBuilds(eq(pr.fromRef.latestCommit))
        verify(mockStash).mergePullRequest([id: pr.id, version: pr.version])
        verify(mockStash).commentPullRequest(eq(pr.id), anyString())
        verify(mockStash, never()).declinePullRequest([id: pr.id, version: pr.version])
    }
    
}

