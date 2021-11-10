package nebula.plugin.stash

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

class StashRestApiImpl implements StashRestApi {
    private final Logger logger = Logging.getLogger(StashRestApiImpl)
    private final String stashHost
    private final String stashUser
    private final String stashPassword
    private final String stashProject
    private final String stashRepo
    private static String MESSAGE_CONFLICTED = "Build was successful but unable to merge pull request. Most likely the pull request was modified during the build (new commits or changing status)."

    StashRestApiImpl(String stashRepo, String stashProject, String stashHost, String stashUser, String stashPassword) {
        assert stashRepo, "missing stashRepo parameter"
        assert stashProject, "missing stashProject parameter"
        assert stashHost, "missing stashHost parameter"
        assert stashUser, "missing stashUser parameter"
        assert stashPassword, "missing stashPassword parameter"
        this.stashRepo = stashRepo
        this.stashProject = stashProject
        this.stashHost = stashHost
        this.stashUser = stashUser
        this.stashPassword = stashPassword
    }

    private GString getRestPath() {
        "/rest/api/1.0/projects/${stashProject}/repos/${stashRepo}/"
    }

    private GString getBranchUtilsRestPath() {
        "/rest/branch-utils/1.0/projects/${stashProject}/repos/${stashRepo}/"
    }

    private String getBasicAuthHeader() {
        "Basic " + "$stashUser:$stashPassword".getBytes('iso-8859-1').encodeBase64()
    }

    private Map stashGetJson(String path, HashMap queryParams = [:])
    {
        log "GET: \n$path"
        return httpRequest(GET, JSON, path, queryParams)
    }

    private Map stashPostJson(String path, Map postBody = [:], Map queryParams = [:])
    {
        log "POST: \n$path \n$postBody"
        def builder = new groovy.json.JsonBuilder(postBody)
        //def root = builder { postBody }
        //log "root : ${root.dump()}"
        log "builder : ${builder.dump()}"
//        return httpRequest(POST, JSON, pathInRepo, queryParams, JSONUtility.jsonFromMap(postBody))
        return httpRequest(POST, JSON, path, queryParams, builder.toString())
    }

    private void stashDeleteJson(String path, Map queryParams = [:])
    {
        log "DELETE: \n$path"
        httpRequest(DELETE, JSON, path, queryParams)
    }

    private Map httpRequest(Method method, ContentType contentType, String path, Map queryParams, String requestBody = '') throws Exception {
        new HTTPBuilder(stashHost).request(method, contentType) { req ->
            uri.path = path
            uri.query = queryParams
            if (method != GET && method != DELETE)
                body = requestBody
            headers.'Authorization' = getBasicAuthHeader()
            response.success = { resp, json ->
                return json
            }

            response.failure = { resp, reader ->
                log "Error message: ${reader?.errors?.message}"
                throw new Exception("Unexpected error: ${reader?.errors?.message} ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase} \n" +
                        "| request body: ${requestBody}\n")
            }
        }
    }

    @Override
    Map postBuildStatus(String changeSet, Map body) {
        changeSet = changeSet.trim()
        def path = "/rest/build-status/1.0/commits/${changeSet}"
        validateKeys(body, ["state", "key", "url"])
        return stashPostJson(path, body)
    }

    @Override
    Map commentPullRequest(Long prId, String comment) {
        def path = getRestPath() + "pull-requests/$prId/comments"
        return stashPostJson(path, ['text': comment])
    }

    @Override
    Map postPullRequest(fromBranch, toRepo, toBranch, title, description)
    {
        fromBranch = fromBranch.trim()
        toBranch = toBranch.trim()

        def path = getRestPath() + "pull-requests/"
        def body = [
                title: title,
                description: description,
                state: "OPEN",
                open : true,
                closed : false,
                fromRef: [
                        id: "refs/heads/$fromBranch",
                        repository: [
                                slug: stashRepo,
                                name: null,
                                project: [
                                        key: stashProject
                                ]
                        ]
                ],
                toRef: [
                        id: "refs/heads/$toBranch",
                        repository: [
                                slug: stashRepo,
                                name: null,
                                project: [
                                        key: stashProject
                                ]
                        ]
                ],
                reviewers: []
        ]
        return stashPostJson(path, body)
    }

    @Override
    Map mergePullRequest(Map pullRequest)
    {
        def path = getRestPath() + "pull-requests/${pullRequest.id}/merge"
        validateKeys(pullRequest, ["id", "version"])
        return stashPostJson(path, [:], [version: pullRequest.version])
    }

    @Override
    Map declinePullRequest(Map pullRequest)
    {
        def path = getRestPath() + "pull-requests/${pullRequest.id}/decline"
        validateKeys(pullRequest, ["id", "version"])
        return stashPostJson(path, [:],[version: pullRequest.version])
    }

    List<Map> getBranchInfo(String object = null) throws Exception {
        String path = getBranchUtilsRestPath() + "branches/info"
        def branches = []
        if(object) {
            path += "/${object}"
        }
        return stashGetJson(path).values.each {
            branches << it
        }
        return branches
    }

    @Override
    List<Map> getPullRequests(String branch, String state, String order)
    {
        branch = branch.trim()
        def path = getRestPath() + "pull-requests/"
        def prs = []
        HashMap params = [:]
        if(branch) {
            params << [at : "refs/heads/$branch"]
        }
        if(state) {
            params << [state : state]
        }
        if(order) {
            params << [order : order]
        }

        stashGetJson(path, params).values.each {
            prs << it
        }
        return prs
    }

    @Override
    List<Map> getPullRequests(String branch)
    {
        branch = branch.trim()
        def path = getRestPath() + "pull-requests/"
        def prs = []
        HashMap params = [:]
        if(branch) {
            params << [at : "refs/heads/$branch"]
        }
        stashGetJson(path, params).values.each {
            prs << it
        }
        return prs
    }

    @Override
    public Map getPullRequest(Long id)
    {
        def path = getRestPath() + "pull-requests/$id"
        return stashGetJson(path)
    }

    @Override
    public List<Map> getBranchesMatching(String branch)
    {
        branch = branch.trim()
        def path = getRestPath() + "branches/"
        def prs = []
        stashGetJson(path, [filterText:branch]).values.each {
            prs << it
        }
        return prs
    }

    @Override
    void deleteBranch(String branchName)
    {
        def path = getRestPath() + "branches/"
        stashDeleteJson(path, [name:"/refs/heads/$branchName", dryRun:false])
    }

    @Override
    List<Map> getBuilds(String changeSet)
    {
        changeSet = changeSet.trim()
        def path = "/rest/build-status/1.0/commits/${changeSet}"
        def builds = []
        stashGetJson(path).values.each({builds << it})
        return builds
    }

    @Override
    Map getBuildStats(String changeSet)
    {
        changeSet = changeSet.trim()
        def path = "/rest/build-status/1.0/commits/stats/${changeSet}"
        return stashGetJson(path)
    }

    private static void validateKeys(Map body, keys) {
        for(key in keys)
            if (!body.containsKey(key) || body[key] == null)
                throw new Exception("Body missing required keys: $key")
    }

    private log(msg){
        logger.info msg
    }
}
