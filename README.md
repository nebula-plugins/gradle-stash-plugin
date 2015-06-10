gradle-stash-plugin
=============================

Cloudbees Jenkins release: [![Build Status](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-release/badge/icon)](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-release/)

Cloudbees Jenkins snapshot: [![Build Status](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-snapshot/badge/icon)](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-snapshot/)

A plugin to run Stash SCM tasks.

Most of the tasks are run against the Stash REST API, but some of them also require running git commands in the command line.

## Usage

### Adding the plugin binary to the build

To include, add the following to your build.gradle

    buildscript {
        repositories { jcenter() }

        dependencies {
            classpath 'com.netflix.nebula:gradle-stash-plugin:1.12.0'
        }
    }

### Provided plugins

The JAR file comes with two plugins:

<table>
    <tr>
        <th>Plugin Identifier</th>
        <th>Depends On</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>gradle-stash-base</td>
        <td>-</td>
        <td>StashBasePlugin</td>
        <td>Provides Stash custom task types and exposes extension for configuration.</td>
    </tr>
    <tr>
        <td>gradle-stash</td>
        <td>gradle-stash-base</td>
        <td>StashPlugin</td>
        <td>Provides a set of default Stash tasks.</td>
    </tr>
</table>

The `gradle-stash` plugin helps you get started quickly. To use the Stash plugin, include the following code snippet
in your build script:

    apply plugin: 'gradle-stash'

If you need full control over the creation of your tasks, you will want to use the `gradle-stash-base` plugin. The downside is that each task
has to be configured individually in your build script. To use the Stash base plugin, include the following code snippet
in your build script:

    apply plugin: 'gradle-stash-base'

### Tasks provided by `gradle-stash`

* mergeBuiltPullRequests - Any pending Pull Request that has been built prior will be merged or declined automatically
    * targetBranch - Only open pull requests from <targetBranch> will be considered
* syncNextPullRequest - Update a git directory to the branch where the next pull request originates from and apply any merge from master as necessary
    * checkoutDir - The directory to run the git commands in.  You should already have you repo cloned in <checkoutDir>
    * targetBranch - (Optional, defaults to master) Only pull requests from <targetBranch> will be considered
    * requireOnlyOneApprover - (Optional, defaults to false) Only require one reviewer to approve in order to sync the PR (vs all reviewers to approve)
* closePullRequest - After a build this task should be run to apply comments and merge the pull request
    * pullRequestId - The pull request id to close
    * pullRequestVersion - The pull request version to close.  This must match the latest version on the Stash server or the pull request won't close
* addBuildStatus - Add a build status to a commit
    * buildCommit - The build status will be added to this commit
    * buildState - The build state to set.  Must be one of SUCCESSFUL, INPROGRESS, or FAILED
    * buildKey - The build key to set
    * buildName - The build name to set
    * buildUrl - The build url to set
    * buildDescription - The build description to set
* postPullRequest - Post a new pull request
    * prFromBranch - The source branch to merge from
    * prToBranch - The target branch to merge to
    * prTitle - The pull request title
    * prDescription - The pull request description
* mergeBranch - Merge any changes from one branch into another.  This is done by first attempting the merge on an intermediate branch, then posting a pull request from the intermediate branch to the target branch.
    * pullFromBranch - The source branch to merge from
    * mergeToBranch - The target branch to merge to
    * remoteName - (Optional, defaults to origin) The Stash remote name
    * repoUrl - The Stash URL to clone
    * workingPath - The target directory to clone to
    * autoMergeBranch - (Optional, defaults to automerge-<pullFromBranch>-to-<mergeToBranch>)  The intermediate branch to use for the merge.  If the merge fails, this is the branch you can check out to manually fix.
    * mergeMessage - (Optional, defaults to : Down-merged branch '<pullFromBranch>' into '<mergeToBranch>' (<autoMergeBranch>)).  The message to add to the commit
    * repoName - (Optional, defaults to a name inferred by <repoUrl>)  The subdir to clone to
* openPostPullRequestIfNotOnBranchTask - Open a pull request if a specified commit is not on the target branch if: the commit is the head of a branch and a pull request isn't already open for the same source and target branch.  This is useful as a post-push task to make sure that code you just released is on the main line (master).
    * prCommit - The source commit
    * prToBranch - The target branch to merge to
    * prTitle - The pull request title
    * prDescription - The pull request description

### Extension properties

The plugin exposes an extension with the following properties:

* `stashRepo` - The Stash repository
* `stashProject` - The Stash project name
* `stashHost` - The Stash host name
* `stashUser` - The Stash user name
* `stashPassword` - The Stash password

#### Example

It's recommended to not hardcode credentials in your `build.gradle` file.

    stash {
        stashRepo = 'example-repo'
        stashProject = 'example-project'
        stashHost = 'my-host'
        stashUser = 'foo'
    }

### Setting extension and task properties

Most of the tasks provided by the `gradle-stash` plugin do not provide sensible defaults for their input properties. It's
up to the plugin user to provide and assign values. If you want to conveniently set properties without having to change your
build script, please have a look at the [gradle-override-plugin](https://github.com/nebula-plugins/gradle-override-plugin).

If you are transitioning from a previous version of the plugin, there a various ways to set properties. Here're some
options including an example that demonstrates the use case:

#### Using project properties

On the command line provide a project project via `-PtargetBranch=master`.

In your build script, parse the provided project property and assign it to the task property:

    mergeBuiltPullRequests.targetBranch = project.hasProperty('targetBranch') ? project.getProperty('targetBranch') : null

#### Using system properties

On the command line provide the a project project via `-Dtarget.branch=master`.

In your build script, parse the provided system property and assign it to the task property:

    mergeBuiltPullRequests.targetBranch = System.getProperty('target.branch')

#### Using the override plugin

On the command line provide the path to your project as system property with the prefix `override.` e.g. `-Doverride.mergeBuiltPullRequests.targetBranch=master`.
There's not need to change the build script. The override plugin takes care of resolving the specific property, converting
the value to the correct data type and assigning the value.

