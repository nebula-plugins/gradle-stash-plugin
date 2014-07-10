package nebula.plugin.stash.util

interface ExternalProcess {
    String execute(String command, String workingDirStr)
    String execute(String command, String workingDirStr, boolean ignoreExitCode)
}