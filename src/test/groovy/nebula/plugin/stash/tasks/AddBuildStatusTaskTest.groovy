package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.util.ExternalProcess
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static nebula.plugin.stash.StashPluginFixture.setDummyStashTaskPropertyValues
import static nebula.plugin.stash.StashTaskAssertion.runTaskExpectFail
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class AddBuildStatusTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'nebula.gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        assertTrue(project.tasks.addBuildStatus instanceof AddBuildStatusTask)
    }

    @Test
    public void canConfigureBuildState() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildState = "INPROGRESS"
        assertEquals("INPROGRESS", task.buildState)
    }

    @Test
    public void canConfigureBuildKey() {
        setDummyStashTaskPropertyValues(project)
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildKey = "121"
        assertEquals("121", task.buildKey)
    }
    @Test
    public void canConfigureBuildName() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildName = "My Build"
        assertEquals("My Build", task.buildName)
    }
    @Test
    public void canConfigureBuildUrl() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildUrl = "http://builds/mine"
        assertEquals("http://builds/mine", task.buildUrl)
    }
    @Test
    public void canConfigureBuildDescription() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildDescription = "Build Description"
        assertEquals("Build Description", task.buildDescription)
    }
    @Test
    public void canConfigureBuildCommit() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildCommit = "AEAEAEAE"
        assertEquals("AEAEAEAE", task.buildCommit)
    }
    @Test
    public void failsIfBuildStateNotProvided() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildKey = "121"
        task.buildName = "My Build"
        task.buildUrl = "http://builds/mine"
        task.buildDescription = "Build Description"
        task.buildCommit = "AEAEAEAE"
        runTaskExpectFail(task, "buildState")
    }

    @Test
    public void failsIfBuildKeyNotProvided() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildState = "INPROGRESS"
        task.buildName = "My Build"
        task.buildUrl = "http://builds/mine"
        task.buildDescription = "Build Description"
        task.buildCommit = "AEAEAEAE"
        runTaskExpectFail(task, "buildKey")
    }

    @Test
    public void failsIfBuildNameNotProvided() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildState = "INPROGRESS"
        task.buildKey = "121"
        task.buildUrl = "http://builds/mine"
        task.buildDescription = "Build Description"
        task.buildCommit = "AEAEAEAE"
        runTaskExpectFail(task, "buildName")
    }

    @Test
    public void failsIfBuildUrlNotProvided() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildState = "INPROGRESS"
        task.buildKey = "121"
        task.buildName = "My Build"
        task.buildDescription = "Build Description"
        task.buildCommit = "AEAEAEAE"
        runTaskExpectFail(task, "buildUrl")
    }

    @Test
    public void failsIfBuildDescriptionNotProvided() {
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildState = "INPROGRESS"
        task.buildKey = "121"
        task.buildName = "My Build"
        task.buildUrl = "http://builds/mine"
        task.buildCommit = "AEAEAEAE"
        runTaskExpectFail(task, "buildDescription")
    }
}

class AddBuildStatusTaskFuncTest {
    StashRestApi mockStash
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(AddBuildStatusTask) {
            buildState = "SUCCESSFUL"
            buildKey = "Smoke_Test"
            buildName = "EDGE-Master-Smoke-Test #503"
            buildUrl = "http://builds.netflix.com/job/EDGE-Master-Smoke-Test/503/"
            buildDescription = "Smoke Test"
        }

        mockStash = mock(StashRestApi.class)
    }

    @Test
    public void addBuildStatusProvidedCommit() {
        // SUCCESSFUL, FAILED and INPROGRESS
        project.apply plugin: 'nebula.gradle-stash'
        AddBuildStatusTask task = project.tasks.addBuildStatus
        task.buildCommit = "ABCD"
        ExternalProcess cmd = task.cmd = mock(ExternalProcess.class)
        project.tasks.addBuildStatus.stash = mockStash
        when(mockStash.postBuildStatus(task.buildCommit, [state:task.buildState, key:task.buildKey, name:task.buildName, url:task.buildUrl, description:task.buildDescription])).thenReturn(null)
        project.tasks.addBuildStatus.execute()
        // nothing to verify since postBuildStatus returns an empty result
    }

    @Test
    public void addBuildStatusCalculatedCommit() {
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.addBuildStatus.stash = mockStash
        AddBuildStatusTask task = project.tasks.addBuildStatus
        ExternalProcess cmd = task.cmd = mock(ExternalProcess.class)
        when(cmd.execute("git rev-parse HEAD", System.getProperty("user.dir"))).thenReturn("FEDCBA\n")
        when(mockStash.postBuildStatus("FEDCBA", [state:task.buildState, key:task.buildKey, name:task.buildName, url:task.buildUrl, description:task.buildDescription])).thenReturn(null)
        project.tasks.addBuildStatus.execute()
        // nothing to verify since postBuildStatus returns an empty result 
    }

    @Test(expected=GradleException.class)
    public void addBuildStatusCantCalculateCommit() {
        project.apply plugin: 'nebula.gradle-stash'
        project.tasks.addBuildStatus.stash = mockStash
        AddBuildStatusTask task = project.tasks.addBuildStatus
        ExternalProcess cmd = task.cmd = mock(ExternalProcess.class)
        when(cmd.execute("git rev-parse HEAD", System.getProperty("user.dir"))).thenReturn(null)
        when(mockStash.postBuildStatus("FEDCBA", [state:task.buildState, key:task.buildKey, name:task.buildName, url:task.buildUrl, description:task.buildDescription])).thenReturn(null)
        project.tasks.addBuildStatus.execute()
    }
}
