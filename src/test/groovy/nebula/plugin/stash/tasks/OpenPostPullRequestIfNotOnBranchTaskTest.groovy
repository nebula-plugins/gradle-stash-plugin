package nebula.plugin.stash.tasks
import nebula.plugin.stash.StashRestApi
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
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
/**
 * Created by dzapata on 11/17/14.
 */
public class OpenPostPullRequestIfNotOnBranchTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        OpenPostPullRequestIfNotOnBranchTask task = project.tasks.openPostPullRequestIfNotOnBranchTask
        assertTrue(task instanceof OpenPostPullRequestIfNotOnBranchTask)
    }

    @Test
    public void canConfigureParams() {
        OpenPostPullRequestIfNotOnBranchTask task = project.tasks.openPostPullRequestIfNotOnBranchTask
        task.prCommit = "prCommit"
        task.prToBranch = "prToBranch"
        task.prTitle = "prTitle"
        task.prDescription = "prDescription"
        assertEquals("prCommit", task.prCommit)
        assertEquals("prToBranch", task.prToBranch)
        assertEquals("prTitle", task.prTitle)
        assertEquals("prDescription", task.prDescription)
    }

    @Test
    public void failsIfPrCommitNotProvided() {
        OpenPostPullRequestIfNotOnBranchTask task = project.tasks.openPostPullRequestIfNotOnBranchTask
        task.prToBranch = "prToBranch"
        task.prTitle = "prTitle"
        task.prDescription = "prDescription"
        runTaskExpectFail(task, "prCommit")
    }

    @Test
    public void failsIfPrToBranchNotProvided() {
        OpenPostPullRequestIfNotOnBranchTask task = project.tasks.openPostPullRequestIfNotOnBranchTask
        task.prCommit = "prCommit"
        task.prTitle = "prTitle"
        task.prDescription = "prDescription"
        runTaskExpectFail(task, "prToBranch")
    }

    @Test
    public void failsIfFPrTitleNotProvided() {
        OpenPostPullRequestIfNotOnBranchTask task = project.tasks.openPostPullRequestIfNotOnBranchTask
        task.prCommit = "prCommit"
        task.prToBranch = "prToBranch"
        task.prDescription = "prDescription"
        runTaskExpectFail(task, "prTitle")
    }

    @Test
    public void failsIfPrDescriptionNotProvided() {
        OpenPostPullRequestIfNotOnBranchTask task = project.tasks.openPostPullRequestIfNotOnBranchTask
        task.prCommit = "prCommit"
        task.prToBranch = "prToBranch"
        task.prTitle = "prTitle"
        runTaskExpectFail(task, "prDescription")
    }
}

class OpenPostPullRequestIfNotOnBranchTaskFunctionalTest {
    StashRestApi mockStash
    Project project
    final String testCommit = "commit"
    final String testRepo = "foo-repo"
    final String testBranch = "foo-branch"
    final String testTitle = "title"
    final String testDescription = "description"
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(OpenPostPullRequestIfNotOnBranchTask) {
            prCommit = testCommit
            prToBranch = testBranch
            prTitle = testTitle
            prDescription = testDescription
        }
        mockStash = mock(StashRestApi.class)
    }

    @Test
    public void commitAlreadyMerged() {
        project.tasks.openPostPullRequestIfNotOnBranchTask.stash = mockStash
        def branchInfoResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a"],
                                   [id : "refs/heads/branch-a", displayId : testBranch] ]

        when(mockStash.getBranchInfo(testCommit)).thenReturn(branchInfoResponse)
        project.tasks.openPostPullRequestIfNotOnBranchTask.execute()

        verify(mockStash, never()).getBranchesMatching()
        verify(mockStash, never()).getPullRequests(anyString(), anyString(), anyString())
        verify(mockStash, never()).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test
    public void commitNotOnAnyBranch() {
        project.tasks.openPostPullRequestIfNotOnBranchTask.stash = mockStash

        when(mockStash.getBranchInfo(testCommit)).thenThrow(new Exception("commit doesn't exist on any branch"))
        try {
            project.tasks.openPostPullRequestIfNotOnBranchTask.execute()
        } catch(Exception e) {
            assertTrue(e.cause.message.contains("commit doesn't exist on any branch"))
        }

        verify(mockStash, never()).getBranchesMatching()
        verify(mockStash, never()).getPullRequests(anyString(), anyString(), anyString())
        verify(mockStash, never()).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test
    public void commitOnMultipleBranches() {
        project.tasks.openPostPullRequestIfNotOnBranchTask.stash = mockStash
        def branchInfoResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a"],
                                   [id : "refs/heads/branch-a", displayId : "branch-b"] ]

        when(mockStash.getBranchInfo(testCommit)).thenReturn(branchInfoResponse)
        try {
            project.tasks.openPostPullRequestIfNotOnBranchTask.execute()
        } catch(RuntimeException e) {
            assertTrue(e.cause.message.contains("multiple (non-${testBranch}) branches have this commit : ${testCommit}"))
        }

        verify(mockStash, never()).getBranchesMatching()
        verify(mockStash, never()).getPullRequests(anyString(), anyString(), anyString())
        verify(mockStash, never()).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test
    public void branchAlreadyHasAPullRequest() {
        project.tasks.openPostPullRequestIfNotOnBranchTask.stash = mockStash
        def branchInfoResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a"] ]
        def matchingBranchResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a", latestChangeset : testCommit] ]
        def openPrsResponse = [ [fromRef : [id: "refs/heads/branch-a"], toRef : [id : "refs/heads/${testBranch}"]] ]

        when(mockStash.getBranchInfo(testCommit)).thenReturn(branchInfoResponse)
        when(mockStash.getBranchesMatching("branch-a")).thenReturn(matchingBranchResponse)
        when(mockStash.getPullRequests(testBranch, "OPEN", null)).thenReturn(openPrsResponse)

        project.tasks.openPostPullRequestIfNotOnBranchTask.execute()

        verify(mockStash, never()).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test
    public void commitNotHeadOfSourceBranch() {
        project.tasks.openPostPullRequestIfNotOnBranchTask.stash = mockStash
        def branchInfoResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a"] ]
        def matchingBranchResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a", latestChangeset : "notExpectedCommit"] ]
        def openPrsResponse = []
        def postPullRequestResponse = [ link : [url : "http://foo.com/123"] ]

        when(mockStash.getBranchInfo(testCommit)).thenReturn(branchInfoResponse)
        when(mockStash.getBranchesMatching("branch-a")).thenReturn(matchingBranchResponse)

        try {
            project.tasks.openPostPullRequestIfNotOnBranchTask.execute()
        } catch(Exception e) {
            assertTrue(e.cause.message.contains("assert"))
        }
        verify(mockStash, never()).getPullRequests(anyString(), anyString(), anyString())
        verify(mockStash, never()).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test
    public void successfullyOpenPullRequest() {
        project.tasks.openPostPullRequestIfNotOnBranchTask.stash = mockStash
        def branchInfoResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a"] ]
        def matchingBranchResponse = [ [id : "refs/heads/branch-a", displayId : "branch-a", latestChangeset : testCommit] ]
        def openPrsResponse = []
        def postPullRequestResponse = [ link : [url : "http://foo.com/123"] ]

        when(mockStash.getBranchInfo(testCommit)).thenReturn(branchInfoResponse)
        when(mockStash.getBranchesMatching("branch-a")).thenReturn(matchingBranchResponse)
        when(mockStash.getPullRequests(testBranch, "OPEN", null)).thenReturn(openPrsResponse)
        when(mockStash.postPullRequest(eq("branch-a"), anyString(), eq(testBranch), eq(testTitle), eq(testDescription))).thenReturn(postPullRequestResponse)

        project.tasks.openPostPullRequestIfNotOnBranchTask.execute()
    }
}
