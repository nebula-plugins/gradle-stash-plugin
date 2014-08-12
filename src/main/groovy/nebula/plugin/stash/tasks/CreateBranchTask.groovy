package nebula.plugin.stash.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.text.SimpleDateFormat

public class CreateBranchTask extends StashTask {
    @Input String branchName
    @Input String startPoint
    @Input @Optional Boolean ignoreIfExists

    @Override
    void executeStashCommand() {
        if (ignoreIfExists) {
            def existingBranch = stash.getBranchesMatching(branchName)
            if (existingBranch.size > 0) return
        }

        stash.createBranch(branchName, startPoint)
    }
}