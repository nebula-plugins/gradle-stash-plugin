package nebula.plugin.stash

interface StashRestApi {
    static final String RPM_BUILD_KEY = "Family_Build"
    static final String INSTALL_BUILD_KEY = "Install_RPM"
    static final String BAKE_BUILD_KEY = "Bake_AMI"
    static final String SMOKE_BUILD_KEY = "Smoke_Test"
    static final String NIGHTLY_BUILD_KEY = "Nightly_Test"
    static final List<String> ALL_BUILD_KEYS = new ArrayList<String>()
    static final String SUCCESSFUL_BUILD_STATE = "SUCCESSFUL"
    static final String INPROGRESS_BUILD_STATE = "INPROGRESS"
    static final String FAILED_BUILD_STATE = "FAILED"
    Map postBuildStatus(String changeSet, Map body)
    Map commentPullRequest(Long prId, String comment)
    Map postPullRequest(fromBranch, toRepo, toBranch, title, description)
    Map mergePullRequest(Map pullRequest)
    Map declinePullRequest(Map pullRequest)
    List<Map> getBranchInfo(String object) throws Exception
    List<Map> getPullRequests(String at)
    List<Map> getPullRequests(String at, String state, String order)
    Map getPullRequest(Long id)
    void createBranch(String branchName, String startPoint)
    void deleteBranch(String branchName)
    List<Map> getBuilds(String changeSet)
    Map getBuildStats(String changeSet)
    List<Map> getBranchesMatching(String branchName)
}
