package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
import static org.junit.Assert.*
import static org.mockito.Matchers.anyString
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class ClosePullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
    }

    @Test
    public void createsTheRightClass() {
        setDummyStashTaskPropertyValues(project)
        assertTrue(project.tasks.closePullRequest instanceof ClosePullRequestAfterBuildTask)
    }
    
    @Test
    public void canConfigurePullRequestVersion() {
        setDummyStashTaskPropertyValues(project)
        ClosePullRequestAfterBuildTask task = project.tasks.closePullRequest
        task.pullRequestVersion = 1L

        assertEquals(1, project.tasks.closePullRequest.pullRequestVersion)
    }
    
    @Test
    public void canConfigurePullRequestId() {
        setDummyStashTaskPropertyValues(project)
        ClosePullRequestAfterBuildTask task = project.tasks.closePullRequest
        task.pullRequestId = 10L

        assertEquals(10, project.tasks.closePullRequest.pullRequestId)
    }

    @Test
    public void failsIfPullRequestIdNotProvided() {
        setDummyStashTaskPropertyValues(project)
        ClosePullRequestAfterBuildTask task = project.tasks.closePullRequest
        task.pullRequestVersion = 1L
        runTaskExpectFail(task, "pullRequestId")
    }
    
    @Test
    public void failsIfPullRequestVersionNotProvided() {
        setDummyStashTaskPropertyValues(project)
        ClosePullRequestAfterBuildTask task = project.tasks.closePullRequest
        task.pullRequestId = 1L
        runTaskExpectFail(task, "pullRequestVersion")
    }
}

class ClosePullRequestTaskFunctionalTest {
    StashRestApi mockStash
    Project project
    final Long givenPullRequestVersion = 1L
    final Long givenPullRequestId = 2L
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(ClosePullRequestAfterBuildTask) {
            pullRequestVersion = givenPullRequestVersion
            pullRequestId = givenPullRequestId
        }

        mockStash = mock(StashRestApi.class)
    }
    
    @Test
    public void closePullRequestAfterBuild() {
        project.tasks.closePullRequest.stash = mockStash
        
        def pr = [id: givenPullRequestId, version: givenPullRequestVersion]
        def prResponse = [toRef : [displayId : "my-branch"], fromRef : [displayId : "master"]]
        
        when(mockStash.mergePullRequest([pr])).thenReturn(prResponse)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)
        
        project.tasks.closePullRequest.execute()
        
        verify(mockStash).mergePullRequest(eq([id: pr.id, version: pr.version]))
        verify(mockStash).commentPullRequest(eq(givenPullRequestId), eq(ClosePullRequestAfterBuildTask.MESSAGE_SUCCESSFUL))
    }
    
    @Test
    public void closePullRequestThrowException() {
        project.tasks.closePullRequest.stash = mockStash
        
        def pr = [id: givenPullRequestId, version: givenPullRequestVersion]

        when(mockStash.mergePullRequest([pr])).thenThrow(new GradleException("mock exception"))
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)
        
        try {
            project.tasks.closePullRequest.execute()
            fail("did not throw expected GradleException")
        } catch(GradleException e) {
            verify(mockStash).commentPullRequest(eq(givenPullRequestId), eq(ClosePullRequestAfterBuildTask.MESSAGE_CONFLICTED))
        }
    }
}
