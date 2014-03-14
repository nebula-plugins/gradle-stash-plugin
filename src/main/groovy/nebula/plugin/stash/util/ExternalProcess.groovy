package nebula.plugin.stash.util

public interface ExternalProcess {
    public String execute(command, workingDirStr)
    public String execute(command, workingDirStr, ignoreExitCode)
}