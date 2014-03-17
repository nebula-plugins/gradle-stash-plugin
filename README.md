gradle-stash-plugin
=============================

Cloudbees Jenkins release: [![Build Status](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-release/badge/icon)](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-release/)

Cloudbees Jenkins snapshot: [![Build Status](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-snapshot/badge/icon)](https://netflixoss.ci.cloudbees.com/job/nebula-plugins/job/gradle-stash-plugin-snapshot/)

A plugin to run Stash SCM tasks.

Most of the tasks are run against the the Stash REST API, but some of them also require running git commands in the command line.

## Usage

### Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
      repositories { jcenter() }

      dependencies {
        classpath 'com.netflix.nebula:gradle-stash-plugin:0.9.0'
      }
    }

    apply plugin: 'gradle-stash'

### Tasks Provided

* mergeBuiltPullRequests - Any pending Pull Request that has been built prior will be merged or declined automatically
** targetBranch - Only open pull requests from <targetBranch> will be considered
* syncNextPullRequest - Update a git directory to the branch where the next pull request originates from and apply any merge from master as necessary
** checkoutDir - The directory to run the git commands in.  You should already have you repo cloned in <checkoutDir>
** targetBranch - (Optional, defaults to master) Only pull requests from <targetBranch> will be considered
* closePullRequest - After a build this task should be run to apply comments and merge the pull request
** pullRequestId - The pull request id to close
** pullRequestVersion - The pull request version to close.  This must match the latest version on the Stash server or the pull request won't close
* addBuildStatus - Add a build status to a commit
** buildCommit - The build status will be added to this commit
** buildState - The build state to set.  Must be one of SUCCESSFUL, INPROGRESS, or FAILED
** buildKey - The build key to set
** buildName - The build name to set
** buildUrl - The build url to set
** buildDescription - The build description to set
* postPullRequest - Post a new pull request
** prFromBranch - The source branch to merge from
** prToBranch - The target branch to merge to
** prTitle - The pull request title
** prDescription - The pull request description
* mergeBranch - Merge any changes from one branch into another.  This is done by first attempting the merge on an intermediate branch, then posting a pull request from the intermediate branch to the target branch.
** pullFromBranch - The source branch to merge from
** mergeToBranch - The target branch to merge to
** remoteName - (Optional, defaults to origin) The Stash remote name
** repoUrl - The Stash URL to clone
** workingPath - The target directory to clone to
** autoMergeBranch - (Optional, defaults to automerge-<pullFromBranch>-to-<mergeToBranch>)  The intermediate branch to use for the merge.  If the merge fails, this is the branch you can check out to manually fix.
** mergeMessage - (Optional, defaults to : Down-merged branch '<pullFromBranch>' into '<mergeToBranch>' (<autoMergeBranch>)).  The message to add to the commit
** repoName - (Optional, defaults to a name inferred by <repoUrl>)  The subdir to clone to

### Extensions Provided
These are meant to be passed in on the command line so you don't hardcode credentials in your build.gradle file, via -P<param>=<value>.

* repo - The Stash repository
* projectName - Stash project name
* user - Stash user name
* password - Stash password

### Properties that Effect the Plugin

## Example

*build.gradle*

    buildscript {
      repositories { jcenter() }
      dependencies {
        classpath 'com.netflix.nebula:gradle-stash-plugin:0.9.0'
      }
    }

    apply plugin: 'java'
    apply plugin: 'gradle-stash'

    repositories {
      mavenCentral()
    }

    dependencies {
    ...
    }

