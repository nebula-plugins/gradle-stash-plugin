package netflix.nebula.scm.stash

import netflix.nebula.scm.stash.StashRestApiImpl;
import org.junit.Test
import static org.junit.Assert.*


class StashRestApiTest {
    //@Test
    public void addBuildStatusAndQueryItTest() {
        def stash = new StashRestApiImpl()
        def commit = 'c191dc6558f8b05a1b6d869c1257516cfeecc36a'
        def state = "SUCCESSFUL"
        def key = stash.RPM_BUILD_KEY
        def name = "$key-42"
        def url = "http://builds.netflix.com/job/EDGE-Master-FMLY/419/consoleFull"
        def description = "Finished: SUCCESS"
        def post = stash.postBuildStatus(commit, [state:state, key:key, name:name, url:url, description:description])
        def query = stash.getBuilds(commit)
        println JSONUtility.jsonFromMap(post)
        println JSONUtility.jsonFromMap(query)
        assertEquals("Build state should be equal", post.state, query.state)
        assertEquals("Build name should be equal", post.name, query.name)
        assertEquals("Build description should be equal", post.description, query.description)
        assertEquals("Build url should be equal", post.url, query.url)
        assertEquals("Build key should be equal", post.key, query.key)
    }

    //@Test
    public void createPullRequestAndMergeItTest() {
        def stash = new StashRestApiImpl()
        def description = "some short description"
        def title = "EDGE build bot request"
        def postResult = stash.postPullRequest("atullDev2", "master", title, description)
        println "postResult = ${JSONUtility.jsonFromMap(postResult)}"
        assertEquals("Newly created pull request is in OPEN state", "OPEN", postResult.state)
        def mergeResult = stash.mergePullRequest([id: postResult.id, version: postResult.version] )
        println "mergeResult = ${JSONUtility.jsonFromMap($mergeResult)}"
        assertEquals("After merging a pull request it is in MERGED state", "OPEN", mergeResult.state)
    }
}
