package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.util.ExternalProcess
import nebula.plugin.stash.util.ExternalProcessImpl
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
import static org.junit.Assert.*
import static org.mockito.AdditionalMatchers.find
import static org.mockito.AdditionalMatchers.not
import static org.mockito.Matchers.anyObject
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class CreateBranchTaskTest {
    Project project
    
    @Before 
    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        assertTrue(project.tasks.createBranch instanceof CreateBranchTask)
    }

    @Test
    public void canConfigureBranchName() {
        CreateBranchTask task = project.tasks.createBranch
        task.branchName = "bar"
        assertEquals("bar", task.branchName)
    }

    @Test
    public void canConfigureStartPoint() {
        CreateBranchTask task = project.tasks.createBranch
        task.startPoint = "bar"
        assertEquals("bar", task.startPoint)
    }

    @Test
    public void canConfigureIgnoreIfExists() {
        CreateBranchTask task = project.tasks.createBranch
        task.ignoreIfExists = true
        assertTrue(task.ignoreIfExists)
    }

    @Test
    public void failsIfBranchNameNotProvided() {
        CreateBranchTask task = project.tasks.createBranch
        task.startPoint = 'refs/heads/master'
        runTaskExpectFail(task, 'branchName')
    }

    @Test
    public void failsIfStartPointNotProvided() {
        CreateBranchTask task = project.tasks.createBranch
        task.branchName = 'bar'
        runTaskExpectFail(task, 'startPoint')
    }
}

class CreateBranchTaskFunctionalTest {
    Project project
    CreateBranchTask task
    StashRestApi mockStash
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(CreateBranchTask) {
            branchName = "new-branch"
            startPoint = "refs/heads/master"
            ignoreIfExists = false
        }

        mockStash = mock(StashRestApi.class)
        project.apply plugin: 'gradle-stash'
        task = project.tasks.createBranch
    }

    @Test
    public void createBranchTask() {
        project.tasks.createBranch.stash = mockStash
        project.tasks.createBranch.execute()

        verify(mockStash).createBranch(eq('new-branch'), eq('refs/heads/master'))
    }

    @Test
    public void createBranchTaskIgnoreIfExists() {
        project.tasks.createBranch.stash = mockStash
        project.tasks.createBranch.ignoreIfExists = true

        when(mockStash.getBranchesMatching(anyString())).thenReturn([[displayId : "new-branch"]])
        project.tasks.createBranch.execute()

        verify(mockStash, never()).createBranch(anyString(), anyString())
    }
}