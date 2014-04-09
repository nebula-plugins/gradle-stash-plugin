package nebula.plugin.stash.tasks

import nebula.plugin.stash.StashRestApi
import nebula.plugin.stash.tasks.MergeBranchTask;
import nebula.plugin.stash.util.ExternalProcess;

import org.gradle.api.Project
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.Before
import org.slf4j.Logger

import static org.junit.Assert.*
import static org.mockito.Mockito.*
import static org.mockito.AdditionalMatchers.*

class MergeBranchTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.apply plugin: 'gradle-stash'
        assertTrue(project.tasks.mergeBranch instanceof MergeBranchTask)
    }
    
    @Test
    public void canConfigurePullFromBranch() {       
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.pullFromBranch = "bar"
        project.apply plugin: 'gradle-stash'
        assertEquals("bar", project.tasks.mergeBranch.pullFromBranch)
    }
    
    @Test
    public void canConfigureMergeToBranch() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.mergeToBranch = "mine"
        project.apply plugin: 'gradle-stash'
        assertEquals("mine", project.tasks.mergeBranch.mergeToBranch)
    }
    @Test
    public void canConfigureRemoteName() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.remoteName = "remote"
        project.apply plugin: 'gradle-stash'
        assertEquals("remote", project.tasks.mergeBranch.remoteName)
    }
    @Test
    public void canConfigureRepoUrl() {       
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.repoUrl = "http://builds/mine"
        project.apply plugin: 'gradle-stash'
        assertEquals("http://builds/mine", project.tasks.mergeBranch.repoUrl)
    }
    @Test
    public void canConfigureWorkingPath() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.workingPath = "/working/path"
        project.apply plugin: 'gradle-stash'
        assertEquals("/working/path", project.tasks.mergeBranch.workingPath)
    }
    @Test
    public void canConfigureAutoMergeBranch() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.autoMergeBranch = "auto-merge"
        project.apply plugin: 'gradle-stash'        
        assertEquals("auto-merge", project.tasks.mergeBranch.autoMergeBranch)
    }
    
    @Test
    public void canConfigurMergeMessage() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.mergeMessage = "merge message"
        project.apply plugin: 'gradle-stash'
        assertEquals("merge message", project.tasks.mergeBranch.mergeMessage)
    }
    
    @Test
    public void canConfigureRepoName() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.repoName = "stashRepo"
        project.apply plugin: 'gradle-stash'
        
        assertEquals("stashRepo", project.tasks.mergeBranch.repoName)
    }
    
    @Test
    public void failsIfPullFromBranchNotProvided() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.mergeToBranch = "branch"
        project.ext.stashRepoUrl = "http://foo/bar"
        project.ext.workingPath = "/foo/bar"
        runTaskExpectFail("pullFromBranch")
    }
    
    @Test
    public void failsIfMergeToBranchNotProvided() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.pullFromBranch = "branch"
        project.ext.stashRepoUrl = "http://foo/bar"
        project.ext.workingPath = "/foo/bar"
        runTaskExpectFail("mergeToBranch")
    }
    
    @Test
    public void failsIfstashRepoUrlNotProvided() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.pullFromBranch = "branch"
        project.ext.mergeToBranch = "branch"
        project.ext.workingPath = "/foo/bar"
        runTaskExpectFail("repoUrl")
    }
    
    @Test
    public void failsIfWorkingPathNotProvided() {
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.pullFromBranch = "branch"
        project.ext.mergeToBranch = "branch"
        project.ext.repoUrl = "http://foo/bar"
        runTaskExpectFail("workingPath")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'gradle-stash'
            project.mergeBranch.execute()
            fail("should have thrown a GradleException")
        } catch (org.gradle.api.tasks.TaskValidationException e) {
            assertTrue(e.cause.message ==~ ".*$missingParam.*")
        }
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
        project.ext.stashRepo = project.ext.stashProject = project.ext.stashUser = project.ext.stashPassword = project.ext.stashHost = "foo"
        project.ext.pullFromBranch = "source-branch"
        project.ext.mergeToBranch = "target-branch"
        project.ext.repoUrl = "http://stashRepo"
        project.ext.workingPath = System.getProperty("user.dir")
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
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true, false)
        when(cmd.execute(anyString(), anyString())).thenReturn(null)
        // cant seem to be able to mock this groovy extension method
        //when(mockFile.deleteDir()).thenReturn(true)
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])
        project.tasks.mergeBranch.execute()
    }
    
    @Test
    public void mergeBranchTaskClonePathExists() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.clonePath = mockClonePath
        
        when(mockClonePath.exists()).thenReturn(true)
        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (org.gradle.api.GradleException e) {
            verify(mockClonePath).exists()
        }
    }
    
    @Test(expected=org.gradle.api.GradleException.class)
    public void mergeBranchTaskFail() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true)
        when(cmd.execute(anyString(), anyString())).thenReturn("Automatic merge failed")
        project.tasks.mergeBranch.execute()
    }
    
    @Test
    public void mergeBranchTaskPushFails() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true, false)
        when(cmd.execute(not(find('rev-parse')), anyString())).thenReturn("call post pull request")
        when(cmd.execute(eq("git rev-parse --branches=source-branch --verify HEAD"), anyString())).thenReturn("ABC")
        when(cmd.execute(eq("git rev-parse --branches=target-branch --verify HEAD"), anyString())).thenReturn("DEF")
        
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())).thenThrow(new RuntimeException("mock exception"))
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])
        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (org.gradle.api.GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())
        }
    }
    
    @Test
    public void mergeBranchTaskDeleteCloneDirReturnsFalse() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true)
        when(cmd.execute(not(find('rev-parse')), anyString())).thenReturn("call post pull request")
        when(cmd.execute(eq("git rev-parse --branches=source-branch --verify HEAD"), anyString())).thenReturn("ABC")
        when(cmd.execute(eq("git rev-parse --branches=source-branch --verify HEAD"), anyString())).thenReturn("ABC")
        when(cmd.execute(eq("git rev-parse --branches=target-branch --verify HEAD"), anyString())).thenReturn("DEF")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null)
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])

        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (org.gradle.api.GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())
        }
    }
    
    @Test
    public void mergeBranchTaskDeleteCloneDirThrowsException() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true).thenThrow(new SecurityException("mock security exception") )
        when(cmd.execute(not(find('rev-parse')), anyString())).thenReturn("call post pull request")
        when(cmd.execute(eq("git rev-parse --branches=source-branch --verify HEAD"), anyString())).thenReturn("ABC")
        when(cmd.execute(eq("git rev-parse --branches=target-branch --verify HEAD"), anyString())).thenReturn("DEF")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null)
        when(mockStash.getBranchesMatching(anyString())).thenReturn([[foo : "bar"]])

        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (org.gradle.api.GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())
        }
    }
}


