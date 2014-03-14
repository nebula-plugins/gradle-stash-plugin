package netflix.nebula.scm.stash

import netflix.nebula.scm.stash.MergeBranchTask
import netflix.nebula.scm.stash.StashRestApi
import netflix.nebula.scm.stash.util.ExternalProcess;

import org.gradle.api.Project
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.Before
import org.slf4j.Logger

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class MergeBranchTaskTest {
    Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void createsTheRightClass() {
        project.repo = project.projectName = project.user = project.password = "foo"
        project.apply plugin: 'nebula-stash'
        assertTrue(project.tasks.mergeBranch instanceof MergeBranchTask)
    }
    
    @Test
    public void canConfigurePullFromBranch() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullFromBranch = "bar"
        project.apply plugin: 'nebula-stash'
        assertEquals("bar", project.tasks.mergeBranch.pullFromBranch)
    }
    
    @Test
    public void canConfigureMergeToBranch() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.mergeToBranch = "mine"
        project.apply plugin: 'nebula-stash'
        assertEquals("mine", project.tasks.mergeBranch.mergeToBranch)
    }
    @Test
    public void canConfigureRemoteName() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.remoteName = "remote"
        project.apply plugin: 'nebula-stash'
        assertEquals("remote", project.tasks.mergeBranch.remoteName)
    }
    @Test
    public void canConfigureRepoUrl() {       
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.repoUrl = "http://builds/mine"
        project.apply plugin: 'nebula-stash'
        assertEquals("http://builds/mine", project.tasks.mergeBranch.repoUrl)
    }
    @Test
    public void canConfigureWorkingPath() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.workingPath = "/working/path"
        project.apply plugin: 'nebula-stash'
        assertEquals("/working/path", project.tasks.mergeBranch.workingPath)
    }
    @Test
    public void canConfigureAutoMergeBranch() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.autoMergeBranch = "auto-merge"
        project.apply plugin: 'nebula-stash'        
        assertEquals("auto-merge", project.tasks.mergeBranch.autoMergeBranch)
    }
    
    @Test
    public void canConfigurMergeMessage() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.mergeMessage = "merge message"
        project.apply plugin: 'nebula-stash'
        assertEquals("merge message", project.tasks.mergeBranch.mergeMessage)
    }
    
    @Test
    public void canConfigureRepoName() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.repoName = "repo"
        project.apply plugin: 'nebula-stash'
        
        assertEquals("repo", project.tasks.mergeBranch.repoName)
    }
    
    @Test
    public void failsIfPullFromBranchNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.mergeToBranch = "branch"
        project.ext.repoUrl = "http://foo/bar"
        project.ext.workingPath = "/foo/bar"
        runTaskExpectFail("pullFromBranch")
    }
    
    @Test
    public void failsIfMergeToBranchNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullFromBranch = "branch"
        project.ext.repoUrl = "http://foo/bar"
        project.ext.workingPath = "/foo/bar"
        runTaskExpectFail("mergeToBranch")
    }
    
    @Test
    public void failsIfRepoUrlNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullFromBranch = "branch"
        project.ext.mergeToBranch = "branch"
        project.ext.workingPath = "/foo/bar"
        runTaskExpectFail("repoUrl")
    }
    
    @Test
    public void failsIfWorkingPathNotProvided() {
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullFromBranch = "branch"
        project.ext.mergeToBranch = "branch"
        project.ext.repoUrl = "http://foo/bar"
        runTaskExpectFail("workingPath")
    }
    
    private void runTaskExpectFail(String missingParam) {
        try {
            project.apply plugin: 'nebula-stash'
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
        project.ext.repo = project.ext.projectName = project.ext.user = project.ext.password = "foo"
        project.ext.pullFromBranch = "source-branch"
        project.ext.mergeToBranch = "target-branch"
        project.ext.repoUrl = "http://repo"
        project.ext.workingPath = System.getProperty("user.dir")
        mockStash = mock(StashRestApi.class)
        mockFile = mock(File.class)
        mockClonePath = mock(File.class)
        project.apply plugin: 'nebula-stash'
        task = project.tasks.mergeBranch
        cmd = task.cmd = mock(ExternalProcess.class)
    }
    
    @Test
    public void mergeBranchTask() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.delete()).thenReturn(true)
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true)
        when(cmd.execute(anyString(), anyString())).thenReturn(null)        
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
        when(mockFile.exists()).thenReturn(true)
        when(cmd.execute(anyString(), anyString())).thenReturn("call post pull request")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())).thenThrow(new RuntimeException("mock exception"))
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
        when(mockFile.delete()).thenReturn(false)
        when(cmd.execute(anyString(), anyString())).thenReturn("call post pull request")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null)

        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (org.gradle.api.GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())
            verify(mockFile).delete()
        }
    }
    
    @Test
    public void mergeBranchTaskDeleteCloneDirThrowsException() {
        project.tasks.mergeBranch.stash = mockStash
        project.tasks.mergeBranch.path = mockFile
        
        when(mockFile.isDirectory()).thenReturn(true)
        when(mockFile.exists()).thenReturn(true)
        when(mockFile.delete()).thenThrow(new SecurityException("mock security exception") )
        when(cmd.execute(anyString(), anyString())).thenReturn("call post pull request")
        when(mockStash.postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null)

        try {
            project.tasks.mergeBranch.execute()
            fail("should have thrown an exception")
        } catch (org.gradle.api.GradleException e) {
            verify(mockStash).postPullRequest(anyObject(), anyObject(), anyObject(), anyObject())
            verify(mockFile).delete()
        }
    }
}


