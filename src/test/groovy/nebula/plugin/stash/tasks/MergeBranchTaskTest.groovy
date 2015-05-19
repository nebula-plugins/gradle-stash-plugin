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

class MergeBranchTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'gradle-stash'
        setDummyStashTaskPropertyValues(project)
    }

    @Test
    public void createsTheRightClass() {
        assertTrue(project.tasks.mergeBranch instanceof MergeBranchTask)
    }
    
    @Test
    public void canConfigurePullFromBranch() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.pullFromBranch = "bar"
        assertEquals("bar", task.pullFromBranch)
    }
    
    @Test
    public void canConfigureMergeToBranch() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.mergeToBranch = "mine"
        assertEquals("mine", task.mergeToBranch)
    }
    @Test
    public void canConfigureRemoteName() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.remoteName = "remote"
        assertEquals("remote", task.remoteName)
    }
    @Test
    public void canConfigureRepoUrl() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.repoUrl = "http://builds/mine"
        assertEquals("http://builds/mine", task.repoUrl)
    }
    @Test
    public void canConfigureWorkingPath() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.workingPath = "/working/path"
        assertEquals("/working/path", task.workingPath)
    }
    @Test
    public void canConfigureAutoMergeBranch() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.autoMergeBranch = "auto-merge"
        assertEquals("auto-merge", task.autoMergeBranch)
    }
    
    @Test
    public void canConfigurMergeMessage() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.mergeMessage = "merge message"
        assertEquals("merge message", task.mergeMessage)
    }
    
    @Test
    public void canConfigureRepoName() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.repoName = "stashRepo"
        assertEquals("stashRepo", task.repoName)
    }
    
    @Test
    public void failsIfPullFromBranchNotProvided() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.mergeToBranch = "branch"
        task.repoUrl = "http://foo/bar"
        task.workingPath = "/foo/bar"
        runTaskExpectFail(task, "pullFromBranch")
    }
    
    @Test
    public void failsIfMergeToBranchNotProvided() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.pullFromBranch = "branch"
        task.repoUrl = "http://foo/bar"
        task.workingPath = "/foo/bar"
        runTaskExpectFail(task, "mergeToBranch")
    }
    
    @Test
    public void failsIfstashRepoUrlNotProvided() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.pullFromBranch = "branch"
        task.mergeToBranch = "branch"
        task.workingPath = "/foo/bar"
        runTaskExpectFail(task, "repoUrl")
    }
    
    @Test
    public void failsIfWorkingPathNotProvided() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.pullFromBranch = "branch"
        task.mergeToBranch = "branch"
        task.repoUrl = "http://foo/bar"
        runTaskExpectFail(task, "workingPath")
    }
}

class MergeBranchTaskFunctionalTest {
    ExternalProcess cmd
    Project project
    MergeBranchTask task
    StashRestApi mockStash
    File mockFile
    File mockClonePath
    
    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        setDummyStashTaskPropertyValues(project)

        project.tasks.withType(MergeBranchTask) {
            pullFromBranch = "source-branch"
            mergeToBranch = "target-branch"
            repoUrl = "http://stashRepo"
            workingPath = System.getProperty("user.dir")
        }

        mockStash = mock(StashRestApi.class)
        mockFile = mock(File.class)
        mockClonePath = mock(File.class)
        project.apply plugin: 'gradle-stash'
        task = project.tasks.mergeBranch
        cmd = task.cmd = mock(ExternalProcess.class)
    }
    
    @Test
    public void mergeBranchTask() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.pathFile = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true, false)
        when(cmd.execute(anyString(), anyString())).thenReturn(null)
        // cant seem to be able to mock this groovy extension method
        //when(mockFile.deleteDir()).thenReturn(true)
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])
        task.execute()
    }
    
    @Test
    public void mergeBranchTaskClonePathExists() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.stash = mockStash
        task.clonePath = mockClonePath
        
        when(mockClonePath.exists()).thenReturn(true)
        try {
            task.execute()
            fail("should have thrown an exception")
        } catch (GradleException e) {
            verify(mockClonePath).exists()
        }
    }
    
    @Test(expected=org.gradle.api.GradleException.class)
    public void mergeBranchTaskFail() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.stash = mockStash
        task.pathFile = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true)
        when(cmd.execute(anyString(), anyString())).thenReturn("Automatic merge failed")
        task.execute()
    }
    
    @Test
    public void mergeBranchTaskPushFails() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.stash = mockStash
        task.pathFile = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true, false)
        when(cmd.execute(not(find('rev-parse')), anyString())).thenReturn("call post pull request")
        when(cmd.execute(find("git rev-parse"), anyString())).thenReturn("ABC", "DEF")       
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject(), anyObject())).thenThrow(new RuntimeException("mock exception"))
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])
        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject(), anyObject())
        }
    }
    
    @Test
    public void mergeBranchTaskDeleteCloneDirReturnsFalse() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.stash = mockStash
        task.pathFile = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true)
        when(cmd.execute(not(find('rev-parse')), anyString())).thenReturn("call post pull request")
        when(cmd.execute(find("git rev-parse"), anyString())).thenReturn("ABC", "DEF")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null)
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])

        try {
            task.execute()
            fail("should have thrown an exception")
        } catch (GradleException e) {
            println (e.dump())
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject(), anyObject())
        }
    }
    
    @Test
    public void mergeBranchTaskDeleteCloneDirThrowsException() {
        MergeBranchTask task = project.tasks.mergeBranch
        task.stash = mockStash
        task.pathFile = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true).thenThrow(new SecurityException("mock security exception") )
        when(cmd.execute(not(find('rev-parse')), anyString())).thenReturn("call post pull request")
        when(cmd.execute(find("git rev-parse"), anyString())).thenReturn("ABC", "DEF")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null)
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])

        try {
            task.execute()
            fail("should have thrown an exception")
        } catch (GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject(), anyObject())
        }
    }
    
    //@Test
    public void testExternalProcess() {
        ExternalProcessImpl e = new ExternalProcessImpl()
        e.execute("git reset --hard @{u}", "/tmp/test/server.git.14-04-09.08:15:28")
    }
    
}


