package netflix.nebula.scm.stash.util

public class ExternalProcessImpl implements ExternalProcess {
    public String execute(command, workingDirStr, ignoreExitCode = false) {
        def workingDir = new File(workingDirStr)
        println "[CMD] $workingDirStr: $command"
        def p = command.execute(null, workingDir)
        def sb = new StringBuffer()
        p.waitForProcessOutput(sb, sb)
        def output = sb.toString()
        output.split("\n").each({x->println "[CMD] $x"})
        if (!ignoreExitCode && p.exitValue() != 0)
            throw new RuntimeException("External process $command in $workingDirStr failed to return a 0 exit code.")
        output
    }
}

