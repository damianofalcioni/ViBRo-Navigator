package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class BRouterProfileDirectoryCandidatesTest {
    private static final String PRIMARY_ROOT = "primary:";
    private static final String MEDIA_PROFILES_DIR_ID =
            PRIMARY_ROOT + "Android/media/btools.routingapp/brouter/profiles2";
    private static final String LEGACY_PROFILES_DIR_ID =
            PRIMARY_ROOT + "Android/data/btools.routingapp/files/brouter/profiles2";

    @Test
    public void buildLikelyPickerDocumentIdCandidates_checksMediaAndDataForAllRootsWithoutSdkSwitch() {
        List<String> candidates = BRouterProfileDirectoryCandidates.buildLikelyPickerDocumentIdCandidates(
                Collections.singletonList("B4DD-C8AC")
        );

        assertEquals("B4DD-C8AC:" + MEDIA_PROFILES_DIR_ID.substring(PRIMARY_ROOT.length()), candidates.get(0));
        assertEquals(MEDIA_PROFILES_DIR_ID, candidates.get(1));
        assertEquals("B4DD-C8AC:" + LEGACY_PROFILES_DIR_ID.substring(PRIMARY_ROOT.length()), candidates.get(2));
        assertEquals(LEGACY_PROFILES_DIR_ID, candidates.get(3));
    }

    @Test
    public void buildFallbackPickerDocumentIdCandidates_prefersPrimaryBeforeSecondaryRoots() {
        List<String> candidates = BRouterProfileDirectoryCandidates.buildFallbackPickerDocumentIdCandidates(
                Collections.singletonList("0000-0000")
        );

        assertEquals(MEDIA_PROFILES_DIR_ID, candidates.get(0));
        assertEquals(LEGACY_PROFILES_DIR_ID, candidates.get(1));
        assertEquals("0000-0000:" + MEDIA_PROFILES_DIR_ID.substring(PRIMARY_ROOT.length()), candidates.get(2));
        assertEquals("0000-0000:" + LEGACY_PROFILES_DIR_ID.substring(PRIMARY_ROOT.length()), candidates.get(3));
    }
}
