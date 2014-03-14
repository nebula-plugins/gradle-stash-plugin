package netflix.nebula.scm.stash.util

public interface ExternalProcess {
    public String execute(command, workingDirStr)
    public String execute(command, workingDirStr, ignoreExitCode)
}