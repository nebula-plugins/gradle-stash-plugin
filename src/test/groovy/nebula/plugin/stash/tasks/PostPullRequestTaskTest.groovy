package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class PostPullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        assertTrue(project.tasks.postPullRequest instanceof PostPullRequestTask)
    }
    
    @Test
    public void canConfigurePrFromBranch() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prFromBranch = "bar"
        assertEquals("bar", task.prFromBranch)
    }
    
    @Test
    public void canConfigurePrToRepo() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prToRepo = "mine"
        assertEquals("mine", task.prToRepo)
    }

    @Test
    public void canConfigurePrToBranch() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prToBranch = "mine"
        assertEquals("mine", task.prToBranch)
    }

    @Test
    public void canConfigurePrTitle() {
        setDummyStashTaskPropertyValues(project)
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prTitle = "title"
        assertEquals("title", task.prTitle)
    }
    
    @Test
    public void canConfigurePrDescription() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prDescription = "description"
        assertEquals("description", task.prDescription)
    }
    
    @Test
    public void failsIfPrFromBranchNotProvided() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prToBranch = "branch2"
        task.prDescription = "description"
        task.prTitle = "title"
        runTaskExpectFail(task, "prFromBranch")
    }
    
    @Test
    public void failsIfprToBranchNotProvided() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prFromBranch = "branch"
        task.prDescription = "description"
        task.prTitle = "title"
        runTaskExpectFail(task, "prToBranch")
    }
    
    @Test
    public void failsIfprDescriptionNotProvided() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prFromBranch = "branch"
        task.prToBranch = "branch2"
        task.prTitle = "title"
        runTaskExpectFail(task, "prDescription")
    }
    
    @Test
    public void failsIfPrTitleNotProvided() {
        PostPullRequestTask task = project.tasks.postPullRequest
        task.prFromBranch = "branch"
        task.prToBranch = "branch2"
        task.prDescription = "description"
        runTaskExpectFail(task, "prTitle")
    }
}

class PostPullRequestTaskFunctionalTest {    
    StashRestApi mockStash
    Project project
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(PostPullRequestTask) {
            prFromBranch = "source-branch"
            prToRepo = "target-repo"
            prToBranch = "target-branch"
            prTitle = "title"
            prDescription = "description"
        }
        
        mockStash = mock(StashRestApi.class)
        project.tasks.postPullRequest.stash = mockStash
    }
    
    @Test
    public void postPullRequest() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123"], toRef: [latestChangeset: "def456"]]
        when(mockStash.postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(pr)
        project.tasks.postPullRequest.execute()
        verify(mockStash).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test(expected = GradleException.class)
    public void postPullRequestFails() {
        def pr = [id:1, version: 0, fromRef: [latestChangeset: "abc123"], toRef: [latestChangeset: "def456"]]
        when(mockStash.postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new GradleException("mock exception"))
        project.tasks.postPullRequest.execute()
    }    
}

