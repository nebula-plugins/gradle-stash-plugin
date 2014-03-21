package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.tasks.PostPullRequestTask;

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.Before
import org.slf4j.Logger

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class PostPullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.apply plugin: 'gradle-stash'
        assertTrue(project.tasks.postPullRequest instanceof PostPullRequestTask)
    }
    
    @Test
    public void canConfigurePrFromBranch() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prFromBranch = "bar"
        project.apply plugin: 'gradle-stash'
        assertEquals("bar", project.tasks.postPullRequest.prFromBranch)
    }
    
    @Test
    public void canConfigurePrToBranch() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prToBranch = "mine"
        project.apply plugin: 'gradle-stash'
        assertEquals("mine", project.tasks.postPullRequest.prToBranch)
    }
    
    @Test
    public void canConfigurePrTitle() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prTitle = "title"
        project.apply plugin: 'gradle-stash'
        assertEquals("title", project.tasks.postPullRequest.prTitle)
    }
    
    @Test
    public void canConfigurePrDescription() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prDescription = "description"
        project.apply plugin: 'gradle-stash'
        assertEquals("description", project.tasks.postPullRequest.prDescription)
    }
    
    @Test
    public void failsIfPrFromBranchNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prToBranch = "branch2"
        project.ext.prDescription = "description"
        project.ext.prTitle = "title"
        runTaskExpectFail("prFromBranch")
    }
    
    @Test
    public void failsIfprToBranchNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prFromBranch = "branch"
        project.ext.prDescription = "description"
        project.ext.prTitle = "title"
        runTaskExpectFail("prToBranch")
    }
    
    @Test
    public void failsIfprDescriptionNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prFromBranch = "branch"
        project.ext.prToBranch = "branch2"
        project.ext.prTitle = "title"
        runTaskExpectFail("prDescription")
    }
    
    @Test
    public void failsIfPrTitleNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prFromBranch = "branch"
        project.ext.prToBranch = "branch2"
        project.ext.prDescription = "description"
        runTaskExpectFail("prTitle")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'gradle-stash'
            project.postPullRequest.execute()
            fail("should have thrown a GradleException")
        } catch (org.gradle.api.tasks.TaskValidationException e) {
            assertTrue(e.cause.message ==~ ".*$missingParam.*")
        }
    }
}

class PostPullRequestTaskFunctionalTest {    
    StashRestApi mockStash
    Project project
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = project.ext.host = "foo"
        project.ext.prFromBranch = "source-branch"
        project.ext.prToBranch = "target-branch"
        project.ext.prTitle = "title"
        project.ext.prDescription = "description"
        
        mockStash = mock(StashRestApi.class)
        project.apply plugin: 'gradle-stash'
        project.tasks.postPullRequest.stash = mockStash
    }
    
    @Test
    public void postPullRequest() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123"], toRef: [latestChangeset: "def456"]]
        when(mockStash.postPullRequest(anyString(), anyString(), anyString(), anyString())).thenReturn(pr)
        project.tasks.postPullRequest.execute()
        verify(mockStash).postPullRequest(anyString(), anyString(), anyString(), anyString())
    }

    @Test(expected = GradleException.class)
    public void postPullRequestFails() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123"], toRef: [latestChangeset: "def456"]]
        when(mockStash.postPullRequest(anyString(), anyString(), anyString(), anyString())).thenThrow(new GradleException("mock exception"))
        project.tasks.postPullRequest.execute()
    }    
}

