package vibro.navigator.nav.session;

import java.util.Collections;
import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NavigationStartupSeedIntegrationTest {
    @Test
    public void fusedNavigationCanCalculateItsFirstRouteFromAValidatedGpsSeed() {
        NavigationSession session = new NavigationSession();
        var text = TestNavigationTextResources.metric();
        session.loadRequest(new NavigationRequest("trekking", "Destination", new LatLon(0, 0.01), Collections.emptyList()));
        NavigationSessionResourceAdapter.start(session, text, 1_000);
        session.setActiveLocationProviders("fused");
        NavigationLocation seed = new NavigationLocation("gps");
        seed.setTime(1_000, 1_000);
        seed.setAccuracy(5);
        var result = session.components.locationEvaluator.onStartupLocationSeed(
                text, session.currentRequest, seed, 1_000, 1_000);
        assertFalse(result.isDropped());
        assertTrue(result.shouldRecalculateRoute());
        assertNotNull(session.prepareRouteRequest(false, 1_000));
        seed.setTime(2_000, 2_000);
        assertTrue(NavigationSessionResourceAdapter.onRawLocationChanged(session, text, seed, 2_000).isDropped());
    }
}
