package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class BRouterProfileListerTest {
    private static final String PROFILE_TREKKING = "trekking";
    private static final String PROFILE_FASTBIKE = "fastbike";

    @Test
    public void mergeProfileNames_mergesDiscoveredExternalTreesWithBundledProfiles() {
        List<String> profiles = BRouterProfileLister.mergeProfileNames(
                Arrays.asList(PROFILE_TREKKING, PROFILE_FASTBIKE, "gravel", PROFILE_TREKKING),
                Arrays.asList("car-eco", PROFILE_FASTBIKE)
        );

        assertEquals(Arrays.asList("car-eco", PROFILE_FASTBIKE, "gravel", PROFILE_TREKKING), profiles);
    }

    @Test
    public void mergeProfileNames_excludesTemporaryRemoteProfileFromBothSources() {
        List<String> profiles = BRouterProfileLister.mergeProfileNames(
                Arrays.asList("remote", "remote-bike", PROFILE_TREKKING),
                Arrays.asList("REMOTE", PROFILE_FASTBIKE)
        );

        assertEquals(Arrays.asList(PROFILE_FASTBIKE, "remote-bike", PROFILE_TREKKING), profiles);
    }
}
