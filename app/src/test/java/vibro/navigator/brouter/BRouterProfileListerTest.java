package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class BRouterProfileListerTest {

    @Test
    public void mergeProfileNames_mergesDiscoveredExternalTreesWithBundledProfiles() {
        List<String> profiles = BRouterProfileLister.mergeProfileNames(
                Arrays.asList("trekking", "fastbike", "gravel", "trekking"),
                Arrays.asList("car-eco", "fastbike")
        );

        assertEquals(Arrays.asList("car-eco", "fastbike", "gravel", "trekking"), profiles);
    }
}
