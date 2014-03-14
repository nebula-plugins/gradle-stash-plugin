package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.tasks.AddBuildStatusTask;
import nebula.plugin.stash.util.ExternalProcess;

import org.gradle.api.Project
import org.gradle.api.tasks.Input;
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.Before
import org.slf4j.Logger
import org.gradle.api.GradleException

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class AddBuildStatusTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.repo = project.projectName = project.user = project.password = "foo"
        project.apply plugin: 'gradle-stash'
        assertTrue(project.tasks.addBuildStatus instanceof AddBuildStatusTask)
    }
    
    @Test
    public void canConfigureBuildState() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildState = "INPROGRESS"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("INPROGRESS", project.tasks.addBuildStatus.buildState)
    }
    
    @Test
    public void canConfigureBuildKey() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildKey = "121"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("121", project.tasks.addBuildStatus.buildKey)
    }
    @Test
    public void canConfigureBuildName() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildName = "My Build"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("My Build", project.tasks.addBuildStatus.buildName)
    }
    @Test
    public void canConfigureBuildUrl() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildUrl = "http://builds/mine"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("http://builds/mine", project.tasks.addBuildStatus.buildUrl)
    }
    @Test
    public void canConfigureBuildDescription() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildDescription = "Build Description"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("Build Description", project.tasks.addBuildStatus.buildDescription)
    }
    @Test
    public void canConfigureBuildCommit() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildCommit = "AEAEAEAE"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("AEAEAEAE", project.tasks.addBuildStatus.buildCommit)
    }
    @Test
    public void failsIfBuildStateNotProvided() {
        def key = "buildState"
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildKey = "121"
        project.ext.buildName = "My Build"
        project.ext.buildUrl = "http://builds/mine"
        project.ext.buildDescription = "Build Description"
        project.ext.buildCommit = "AEAEAEAE"
        runTaskExpectFail("buildState")
    }
    
    @Test
    public void failsIfBuildKeyNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildState = "INPROGRESS"
        project.ext.buildName = "My Build"
        project.ext.buildUrl = "http://builds/mine"
        project.ext.buildDescription = "Build Description"
        project.ext.buildCommit = "AEAEAEAE"
        runTaskExpectFail("buildKey")
    }
    
    @Test
    public void failsIfBuildNameNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildState = "INPROGRESS"
        project.ext.buildKey = "121"
        project.ext.buildUrl = "http://builds/mine"
        project.ext.buildDescription = "Build Description"
        project.ext.buildCommit = "AEAEAEAE"
        runTaskExpectFail("buildName")
    }
    
    @Test
    public void failsIfBuildUrlNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildState = "INPROGRESS"
        project.ext.buildKey = "121"
        project.ext.buildName = "My Build"
        project.ext.buildDescription = "Build Description"
        project.ext.buildCommit = "AEAEAEAE"
        runTaskExpectFail("buildUrl")
    }
    
    @Test
    public void failsIfBuildDescriptionNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildState = "INPROGRESS"
        project.ext.buildKey = "121"
        project.ext.buildName = "My Build"
        project.ext.buildUrl = "http://builds/mine"
        project.ext.buildCommit = "AEAEAEAE"
        runTaskExpectFail("buildDescription")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'gradle-stash'
            project.addBuildStatus.execute()
            fail("should have thrown a GradleException")
        } catch (org.gradle.api.tasks.TaskValidationException e) {
            println e.cause.message
            assertTrue(e.cause.message ==~ ".*$missingParam.*")
        }
    }
}

class AddBuildStatusTaskFuncTest {
    StashRestApi mockStash
    Project project
    ExternalProcess cmd
    AddBuildStatusTask task

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.buildState = "SUCCESSFUL"
        project.ext.buildKey = "Smoke_Test"
        project.ext.buildName = "EDGE-Master-Smoke-Test #503"
        project.ext.buildUrl = "http://builds.netflix.com/job/EDGE-Master-Smoke-Test/503/"
        project.ext.buildDescription = "Smoke Test"
        mockStash = mock(StashRestApi.class)
    }

    @Test
    public void addBuildStatusProvidedCommit() {
        // SUCCESSFUL, FAILED and INPROGRESS
        project.ext.buildCommit = "ABCD"
        project.apply plugin: 'gradle-stash'
        task = project.tasks.addBuildStatus
        cmd = task.cmd = mock(ExternalProcess.class)
        project.tasks.addBuildStatus.stash = mockStash
        when(mockStash.postBuildStatus(project.ext.buildCommit, [state:project.ext.buildState, key:project.ext.buildKey, name:project.ext.buildName, url:project.ext.buildUrl, description:project.project.ext.buildDescription])).thenReturn(null)
        project.tasks.addBuildStatus.execute()
        // nothing to verify since postBuildStatus returns an empty result
    }
    
    @Test
    public void addBuildStatusCalculatedCommit() {
        project.apply plugin: 'gradle-stash'
        project.tasks.addBuildStatus.stash = mockStash
        task = project.tasks.addBuildStatus
        cmd = task.cmd = mock(ExternalProcess.class)
        when(cmd.execute("git rev-parse HEAD", System.getProperty("user.dir"))).thenReturn("FEDCBA\n")
        when(mockStash.postBuildStatus("FEDCBA", [state:project.ext.buildState, key:project.ext.buildKey, name:project.ext.buildName, url:project.ext.buildUrl, description:project.project.ext.buildDescription])).thenReturn(null)
        project.tasks.addBuildStatus.execute()
        // nothing to verify since postBuildStatus returns an empty result 
    }
    
    @Test(expected=GradleException.class)
    public void addBuildStatusCantCalculateCommit() {
        project.apply plugin: 'gradle-stash'
        project.tasks.addBuildStatus.stash = mockStash
        task = project.tasks.addBuildStatus
        cmd = task.cmd = mock(ExternalProcess.class)
        when(cmd.execute("git rev-parse HEAD", System.getProperty("user.dir"))).thenReturn(null)
        when(mockStash.postBuildStatus("FEDCBA", [state:project.ext.buildState, key:project.ext.buildKey, name:project.ext.buildName, url:project.ext.buildUrl, description:project.project.ext.buildDescription])).thenReturn(null)
        project.tasks.addBuildStatus.execute()
    }
}
