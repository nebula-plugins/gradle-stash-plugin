package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.tasks.ClosePullRequestAfterBuildTask;

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.Before
import org.slf4j.Logger

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class ClosePullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.repo = project.projectName = project.user = project.password = "foo"
        project.apply plugin: 'nebula-stash'
        assertTrue(project.tasks.closePullRequest instanceof ClosePullRequestAfterBuildTask)
    }
    
    @Test
    public void canConfigurePullRequestVersion() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullRequestVersion = "1"
        project.apply plugin: 'nebula-stash'
        
        assertEquals("1", project.tasks.closePullRequest.pullRequestVersion)
    }
    
    @Test
    public void canConfigurePullRequestId() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullRequestId = "10"
        project.apply plugin: 'nebula-stash'
        
        assertEquals("10", project.tasks.closePullRequest.pullRequestId)
    }

    @Test
    public void failsIfPullRequestIdNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullRequestVersion = "1"
        runTaskExpectFail("pullRequestId")
    }
    
    @Test
    public void failsIfPullRequestVersionNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullRequestId = "1"
        runTaskExpectFail("pullRequestVersion")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'nebula-stash'
            project.closePullRequest.execute()
            fail("should have thrown a GradleException")
        } catch (org.gradle.api.tasks.TaskValidationException e) {
            assertTrue(e.cause.message ==~ ".*$missingParam.*")
        }
    }
}

class ClosePullRequestTaskFunctionalTest {
    StashRestApi mockStash
    Project project
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullRequestVersion = 1
        project.ext.pullRequestId = 2
        mockStash = mock(StashRestApi.class)
    }
    
    @Test
    public void closePullRequestAfterBuild() {
        project.apply plugin: 'nebula-stash'
        project.tasks.closePullRequest.stash = mockStash
        
        def pr = [id:project.ext.pullRequestId, version:  project.ext.pullRequestVersion]
        def prResponse = [toRef : [displayId : "my-branch"], fromRef : [displayId : "master"]]
        
        when(mockStash.mergePullRequest([pr])).thenReturn(prResponse)
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)
        
        project.tasks.closePullRequest.execute()
        
        verify(mockStash).mergePullRequest(eq([id: pr.id, version: pr.version]))
        verify(mockStash).commentPullRequest(eq(project.ext.pullRequestId), eq(ClosePullRequestAfterBuildTask.MESSAGE_SUCCESSFUL))   
    }
    
    @Test
    public void closePullRequestThrowException() {
        project.apply plugin: 'nebula-stash'
        project.tasks.closePullRequest.stash = mockStash
        
        def pr = [id:project.ext.pullRequestId, version:  project.ext.pullRequestVersion]
        def prResponse = [toRef : [displayId : "my-branch"], fromRef : [displayId : "master"]]
        
        when(mockStash.mergePullRequest([pr])).thenThrow(new GradleException("mock exception"))
        when(mockStash.commentPullRequest(eq(pr.id), anyString())).thenReturn(null)
        
        try {
            project.tasks.closePullRequest.execute()
            fail("did not throw expected GradleException")
        } catch(GradleException e) {
            verify(mockStash).commentPullRequest(eq(project.ext.pullRequestId), eq(ClosePullRequestAfterBuildTask.MESSAGE_CONFLICTED))   
        }
    }
}
