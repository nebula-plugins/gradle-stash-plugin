package nebula.plugin.stash

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.logging.Logger

import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*

public interface StashRestApi {
    static final String RPM_BUILD_KEY = "Family_Build"
    static final String INSTALL_BUILD_KEY = "Install_RPM"
    static final String BAKE_BUILD_KEY = "Bake_AMI"
    static final String SMOKE_BUILD_KEY = "Smoke_Test"
    static final String NIGHTLY_BUILD_KEY = "Nightly_Test"
    static final List<String> ALL_BUILD_KEYS = new ArrayList<String>()
    static final String SUCCESSFUL_BUILD_STATE = "SUCCESSFUL"
    static final String INPROGRESS_BUILD_STATE = "INPROGRESS"
    static final String FAILED_BUILD_STATE = "FAILED"
    public Map postBuildStatus(String changeSet, HashMap body)
    public Map commentPullRequest(int prId, String comment)
    public Map postPullRequest(fromBranch, toBranch, title, description)
    public Map mergePullRequest(HashMap pullRequest)
    public Map declinePullRequest(HashMap pullRequest)
    public List<Map> getPullRequests(String branch)
    public Map getPullRequest(int id)
    public void deleteBranch(String branchName)
    public List<Map> getBuilds(String changeSet)
    public Map getBuildStats(String changeSet)
    public Logger setLogger(Logger logger)
}

public class StashRestApiImpl implements StashRestApi {

    private String stashHost
    private String userName
    private String password
    private String project
    private String repo

    public Logger logger
    private static String MESSAGE_CONFLICTED = "Build was successful but unable to merge pull request. Most likely the pull request was modified during the build (new commits or changing status)."

    public StashRestApiImpl(String repo, String project, String stashHost, String userName, String password) {
        if(repo) {
            this.repo = repo
        } else {
            throw new Exception("missing repo parameter")
        }
        
        if(project) {
            this.project = project
        } else {
            throw new Exception("missing project parameter")
        }
        
        if(stashHost) {
            this.stashHost = stashHost
        } else {
            throw new Exception("missing stashHost parameter")
        }
        
        if(userName) {
            this.userName = userName
        } else {
            throw new Exception("missing userName parameter")
        }
        
        if(password) {
            this.password = password
        } else {
            throw new Exception("missing password parameter")
        }
    }

    private GString getRestPath() {
        "/rest/api/1.0/projects/${project}/repos/${repo}/"
    }

    private String getBasicAuthHeader() {
        "Basic " + "$userName:$password".getBytes('iso-8859-1').encodeBase64()
    }

    private Map stashGetJson(String path, HashMap queryParams = [])
    {
        log "GET: \n$path"
        return httpRequest(GET, JSON, path, queryParams)
    }

    private Map stashPostJson(String path, HashMap postBody = [], HashMap queryParams = [])
    {
        log "POST: \n$path \n$postBody"        
        def builder = new groovy.json.JsonBuilder()
        def root = builder { postBody }
        log "builder : " + builder.toString()
//        return httpRequest(POST, JSON, path, queryParams, JSONUtility.jsonFromMap(postBody))
        return httpRequest(POST, JSON, path, queryParams, builder.toString())
    }

    private Map stashDeleteJson(String path, HashMap postBody = [], HashMap queryParams = [])
    {
        log "DELETE: \n$path \n$postBody"        
        def builder = new groovy.json.JsonBuilder()
        def root = builder { postBody }
        log "builder : " + builder.toString()
//        return httpRequest(POST, JSON, path, queryParams, JSONUtility.jsonFromMap(postBody))
        return httpRequest(DELETE, JSON, path, queryParams, builder.toString())
    }

    private Map httpRequest(Method method, ContentType contentType, String path, HashMap queryParams, String requestBody = '') {        
        new HTTPBuilder(stashHost).request(method, contentType) { req ->
           uri.path = path
           uri.query = queryParams
           if (method != GET)
               body = requestBody
           headers.'Authorization' = getBasicAuthHeader()
           response.success = { resp, json ->
               return json
           }

           response.failure = { resp ->
               log "Failing"
               throw new Exception("Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase} \n" +
                   "| request data: ${requestBody}\n" +
                   "| response data: ${resp.data}")
               }
           }
    }

    public Map postBuildStatus(String changeSet, HashMap body) {
        changeSet = changeSet.trim()
        def path = "/rest/build-status/1.0/commits/${changeSet}"
        validateKeys(body, ["state", "key", "url"])
        return stashPostJson(path, body)
    }

    public Map commentPullRequest(int prId, String comment) {
        def path = getRestPath() + "pull-requests/$prId/comments"
        return stashPostJson(path, ['text': comment])
    }

    public Map postPullRequest(fromBranch, toBranch, title, description)
    {
        fromBranch = fromBranch.trim()
        toBranch = toBranch.trim()

        def path = getRestPath() + "pull-requests/"
        def repositoryMap = [
                slug: repo,
                name: null,
                project: [
                        key: project
                ]
        ]
        def body = [
                title: title,
                description: description,
                state: "OPEN",
                fromRef: [
                        id: "refs/heads/$fromBranch",
                        repository: repositoryMap
                ],
                toRef: [
                        id: "refs/heads/$toBranch",
                        repository: repositoryMap
                ],
                reviewers: []
        ]
        return stashPostJson(path, body)
    }

    public Map mergePullRequest(HashMap pullRequest)
    {        
        def path = getRestPath() + "pull-requests/${pullRequest.id}/merge"
        validateKeys(pullRequest, ["id", "version"])
        return stashPostJson(path, [:], [version: pullRequest.version])
    }

    public Map declinePullRequest(HashMap pullRequest)
    {
        def path = getRestPath() + "pull-requests/${pullRequest.id}/decline"
        validateKeys(pullRequest, ["id", "version"])
        return stashPostJson(path, [:],[version: pullRequest.version])
    }

    @Override
    public List<Map> getPullRequests(String branch)
    {
        branch = branch.trim()
        def path = getRestPath() + "pull-requests/"
        def prs = []
        stashGetJson(path, [at:"refs/heads/$branch"]).each(prs << it.values)
        return prs
    }

    public Map getPullRequest(int id)
    {
        def path = getRestPath() + "pull-requests/$id"
        return stashGetJson(path)
    }

    @Override
    public void deleteBranch(String branchName) 
    {
        return stashDeleteJson(path, [name:"/refs/heads/$branchName", dryRun:false], [:])
    }

    public List<Map> getBuilds(String changeSet)
    {
        changeSet = changeSet.trim()
        def path = "/rest/build-status/1.0/commits/${changeSet}"
        def builds = []
        stashGetJson(path).each({builds << it.values})
        return builds
    }

    public Map getBuildStats(String changeSet)
    {
        changeSet = changeSet.trim()
        def path = "/rest/build-status/1.0/commits/stats/${changeSet}"
        return stashGetJson(path)
    }

    private static void validateKeys(HashMap body, keys) {
        for(key in keys)
            if (!body.containsKey(key) || body[key] == null)
                throw new Exception("Body missing required keys: $key")
    }

    private log(msg){
        if (this.logger != null)
            logger.info msg
    }

    @Override
    public Logger setLogger(Logger logger) {
        this.logger = logger
    }
}

