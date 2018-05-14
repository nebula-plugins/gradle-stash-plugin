package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class PostPullRequestTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'nebula.gradle-stash'
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
}

class PostPullRequestTaskFunctionalTest {    
    StashRestApi mockStash
    Project project
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'nebula.gradle-stash'
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
        def pr = [id:1, version: 0, fromRef: [latestCommit: "abc123"], toRef: [latestCommit: "def456"]]
        when(mockStash.postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(pr)
        project.tasks.postPullRequest.runAction()
        verify(mockStash).postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())
    }

    @Test(expected = GradleException.class)
    public void postPullRequestFails() {
        def pr = [id:1, version: 0, fromRef: [latestCommit: "abc123"], toRef: [latestCommit: "def456"]]
        when(mockStash.postPullRequest(anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new GradleException("mock exception"))
        project.tasks.postPullRequest.runAction()
    }    
}

