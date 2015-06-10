package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.util.ExternalProcess
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
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
        Assert.assertTrue(project.tasks.syncNextPullRequest instanceof SyncNextPullRequestTask)
    }

    @Test
    public void canConfigureTargetBranch() {
        SyncNextPullRequestTask task = project.tasks.syncNextPullRequest
        task.targetBranch = "bar"

        Assert.assertEquals("bar", task.targetBranch)
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
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        when(mockStash.getPullRequest(anyInt())).thenReturn(pr)
        task.execute()
    }
    
    @Test(expected=GradleException.class)
    public void syncNextPullRequestGetPrsFails() {
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenThrow(new GradleException("mock exception"))
        task.execute()
    }

    @Test
    public void syncNextPullRequestInvalidPr() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]], toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]]]
        def build = [key: StashRestApi.RPM_BUILD_KEY, state: StashRestApi.INPROGRESS_BUILD_STATE, url: "http://netflix.com/"]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getBuilds(anyString())).thenReturn([build])
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.execute()
        verify(mockStash).getBuilds(anyString())
    }
    
    @Test
    public void syncNextPullRequestUnableToMerge() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]], toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]]]
        when(cmd.execute(anyString(), anyString())).thenReturn("Automatic merge failed")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        try {
            task.execute()
            fail("should have thrown a GradleException")
        } catch (GradleException e) {
            // pass
            verify(mockStash).commentPullRequest(anyLong(), anyString())
            verify(mockStash).declinePullRequest(anyMap())
        }
    }

    @Test
    public void syncNextPullRequestWithSingleReviewerNotApproved() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                    reviewers : [[approved : false, user : [displayName : "Bob Reviewer"]]]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.execute()
        Assert.assertFalse(project.hasProperty("pullRequestId"))
        Assert.assertFalse(project.hasProperty("pullRequestVersion"))
        Assert.assertFalse(project.hasProperty("buildCommit"))
    }

    @Test
    public void syncNextPullRequestWithMultipleReviewersNotApproved() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]], [approved : false, user : [displayName : "Joe Reviewer"]]
                           ]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.execute()
        Assert.assertFalse(project.hasProperty("pullRequestId"))
        Assert.assertFalse(project.hasProperty("pullRequestVersion"))
        Assert.assertFalse(project.hasProperty("buildCommit"))
    }

    @Test
    public void syncNextPullRequestWithSingleReviewerApproved() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]]]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.execute()
        Assert.assertTrue(project.hasProperty("pullRequestId"))
        Assert.assertTrue(project.hasProperty("pullRequestVersion"))
        Assert.assertTrue(project.hasProperty("buildCommit"))
    }

    @Test
    public void syncNextPullRequestWithMultipleReviewersApproved() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]], [approved : true, user : [displayName : "Joe Reviewer"]]
                  ]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.execute()
        Assert.assertTrue(project.hasProperty("pullRequestId"))
        Assert.assertTrue(project.hasProperty("pullRequestVersion"))
        Assert.assertTrue(project.hasProperty("buildCommit"))
    }

    @Test
    public void syncNextPullRequestWithOnlyOneReviewersApproved() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]], [approved : false, user : [displayName : "Joe Reviewer"]]
                  ]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.requireOnlyOneApprover = true
        task.execute()
        Assert.assertTrue(project.hasProperty("pullRequestId"))
        Assert.assertTrue(project.hasProperty("pullRequestVersion"))
        Assert.assertTrue(project.hasProperty("buildCommit"))
    }

    @Test
    public void syncNextPullRequestWithAllReviewersApproved() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : true, user : [displayName : "Bob Reviewer"]], [approved : true, user : [displayName : "Joe Reviewer"]]
                  ]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.requireOnlyOneApprover = true
        task.execute()
        Assert.assertTrue(project.hasProperty("pullRequestId"))
        Assert.assertTrue(project.hasProperty("pullRequestVersion"))
        Assert.assertTrue(project.hasProperty("buildCommit"))
    }

    @Test
    public void syncNextPullRequestWithMultipleReviewersNotApprovedRequireOne() { //nothing should get processed, task should pass
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123", displayId: "fromDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  toRef: [latestChangeset: "def456", displayId: "toDisplayId", repository: [cloneUrl: "abc.com/stash"]],
                  reviewers : [[approved : false, user : [displayName : "Bob Reviewer"]], [approved : false, user : [displayName : "Joe Reviewer"]]
                  ]
        ]
        when(cmd.execute(anyString(), anyString())).thenReturn("abc123")
        when(mockStash.getPullRequests(anyString(), anyString(), anyString())).thenReturn([pr])
        task.requireOnlyOneApprover = true
        task.execute()
        Assert.assertFalse(project.hasProperty("pullRequestId"))
        Assert.assertFalse(project.hasProperty("pullRequestVersion"))
        Assert.assertFalse(project.hasProperty("buildCommit"))
    }
}
