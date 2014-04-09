package nebula.plugin.stash.util

public interface ExternalProcess {
    public String execute(String command, String workingDirStr)
    public String execute(String command, String workingDirStr, boolean ignoreExitCode)
}