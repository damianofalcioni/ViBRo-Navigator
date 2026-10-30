package vibro.navigator.nav.session;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.LocationLogReplay;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import static org.junit.Assert.*;

/** Recorded callback evidence through selection/filter/motion/guidance, with an explicit synthetic target. */
public class RecordedLocationSessionReliabilityTest {
    @Test
    public void recordedDeparturesRetainChronologyAndCannotClaimPoorObservationsAreStops() throws Exception {
        replay("gnss-departure", "gps+network");
        replay("fused-departure", "fused");
    }

    @Test
    public void recordedPoorApproachCannotDeclareArrivalAtAnUncertainTarget() throws Exception {
        replay("gnss-poor-approach", "gps+network");
    }

    private static void replay(String name, String providers) throws Exception {
        List<LocationLogReplay.Sample> samples = LocationLogReplay.load(name);
        NavigationLocation last = samples.get(samples.size() - 1).location;
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE, null, "Replay target",
                new LatLon(last.getLatitude(), last.getLongitude()), Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders(providers);
        long previousMeasurementMs = -1;
        int accepted = 0;
        int rejected = 0;
        for (LocationLogReplay.Sample sample : samples) {
            NavigationLocationUpdateResult result = accept(session, sample);
            if (result.isDropped()) {
                rejected++;
                continue;
            }
            accepted++;
            NavigationLocation filtered = session.getLastFilteredLocation();
            assertTrue(filtered.getElapsedRealtimeOrTimeMs() > previousMeasurementMs);
            previousMeasurementMs = filtered.getElapsedRealtimeOrTimeMs();
            verifyUncertainSample(session, sample, result);
            assertTrue("Duplicate callbacks must not add confidence", accept(session, sample).isDropped());
        }
        assertTrue("Replay must exercise accepted evidence", accepted >= 5);
        if (providers.contains("network")) {
            assertTrue("Replay must exercise selection/rejection", rejected > 0);
        }
    }

    private static void verifyUncertainSample(NavigationSession session, LocationLogReplay.Sample sample,
            NavigationLocationUpdateResult result) {
        if (result.filteredLocation.getProviderAccuracy() > 25) {
            assertFalse(session.isLikelyStationaryForOrientation(sample.receivedAtMs));
            assertTrue(result.turnEvents.stream().noneMatch(event -> event.hint != null && event.hint.command == 100));
        }
        if (!session.components.locationState.isMotionKnown(sample.receivedAtMs)) {
            assertTrue("Unknown motion retains fast verification", result.getSuggestedUpdateIntervalMs() <= 3_000);
        }
    }

    private static NavigationLocationUpdateResult accept(NavigationSession session, LocationLogReplay.Sample sample) {
        return NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                sample.location, sample.receivedAtMs, sample.requestedIntervalMs);
    }
}
