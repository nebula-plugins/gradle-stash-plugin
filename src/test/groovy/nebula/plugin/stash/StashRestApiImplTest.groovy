package nebula.plugin.stash

import spock.lang.Specification
import spock.lang.Unroll

class StashRestApiImplTest extends Specification {
    @Unroll
    def "Fails if constructor parameter #nullParamName is null"() {
        when:
        new StashRestApiImpl(stashRepo, stashProject, stashHost, stashUser, stashPassword)

        then:
        Throwable t = thrown(AssertionError)
        t.message.startsWith("missing $nullParamName parameter")

        where:
        stashRepo | stashProject | stashHost | stashUser | stashPassword | nullParamName
        null      | 'myProject'  | 'myHost'  | 'myUser'  | 'myPassword'  | 'stashRepo'
        'myRepo'  | null         | 'myHost'  | 'myUser'  | 'myPassword'  | 'stashProject'
        'myRepo'  | 'myProject'  | null      | 'myUser'  | 'myPassword'  | 'stashHost'
        'myRepo'  | 'myProject'  | 'myHost'  | null      | 'myPassword'  | 'stashUser'
        'myRepo'  | 'myProject'  | 'myHost'  | 'myUser'  | null          | 'stashPassword'
    }

    def "Can create instance if all constructor parameter are provided"() {
        when:
        new StashRestApiImpl('myRepo', 'myProject', 'myHost', 'myUser', 'myPassword')

        then:
        noExceptionThrown()
    }
}
