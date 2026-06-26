package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class BRouterSegmentDirectoryCandidatesTest {
    private static final String PRIMARY_ROOT = "primary:";
    private static final String MEDIA_SEGMENTS_DIR_ID =
            PRIMARY_ROOT + "Android/media/btools.routingapp/brouter/segments4";
    private static final String LEGACY_SEGMENTS_DIR_ID =
            PRIMARY_ROOT + "Android/data/btools.routingapp/files/brouter/segments4";

    @Test
    public void buildLikelyDocumentIdCandidates_usesProfilesDiscoveryRootOrderForSegments4() {
        List<String> candidates = BRouterSegmentDirectoryCandidates.buildLikelyDocumentIdCandidates(
                Collections.singletonList("B4DD-C8AC")
        );

        assertEquals("B4DD-C8AC:" + MEDIA_SEGMENTS_DIR_ID.substring(PRIMARY_ROOT.length()), candidates.get(0));
        assertEquals(MEDIA_SEGMENTS_DIR_ID, candidates.get(1));
        assertEquals("B4DD-C8AC:" + LEGACY_SEGMENTS_DIR_ID.substring(PRIMARY_ROOT.length()), candidates.get(2));
        assertEquals(LEGACY_SEGMENTS_DIR_ID, candidates.get(3));
    }
}
