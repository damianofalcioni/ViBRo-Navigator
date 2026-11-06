package vibro.navigator.nav.location;

import org.junit.Test;
import vibro.navigator.nav.kalman.LatLonKalmanFilter;
import static org.junit.Assert.*;

/** Compare the recorded stream with the previous GPS-first tie break. */
public class LiveLocationHysteresisReplayTest {
    private static final String GPS = "gps";
    private static final String NETWORK = "network";

    @Test
    public void recordedHandoversReduceWithoutRetainingMotionAcrossProviderOffsets() throws Exception {
        Replay previous = replay(false);
        Replay current = replay(true);
        assertEquals(9, previous.switches);
        assertEquals(7, current.switches);
        assertTrue(current.switches < previous.switches);
        assertEquals(16, current.accepted);
        assertEquals(previous.maxGapMs, current.maxGapMs);
        assertEquals(9_790, current.maxRapidGapMs);
        assertTrue("Accepted measurements remain fresh", current.maxAgeMs <= 1_000);
        assertTrue("Actual handovers still reset motion", current.unknownHandovers == current.switches);
        System.out.println("handover replay previous=" + previous + " current=" + current);
    }

    @Test
    public void otherRecordedGpsAndFusedStreamsKeepTheirAcceptedMeasurements() throws Exception {
        for (String name : new String[] {"gnss-departure", "gnss-poor-approach", "fused-departure"}) {
            Replay previous = replay(name, false);
            Replay current = replay(name, true);
            assertEquals(name, previous.accepted, current.accepted);
            assertEquals(name, previous.switches, current.switches);
            assertEquals(name, previous.maxAgeMs, current.maxAgeMs);
        }
    }

    @Test
    public void selectedNetworkSurvivesNewerButWorseGpsAndYieldsToMaterialImprovement() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        NavigationLocation network = fix(NETWORK, 1_000, 20);
        coordinator.remember(network);
        coordinator.markDispatched(network, 1_000);
        coordinator.remember(fix(GPS, 4_000, 40));
        assertEquals(NETWORK, coordinator.selectBestLiveLocation(4_000).getProvider());
        coordinator.remember(fix(GPS, 5_000, 10));
        assertEquals(GPS, coordinator.selectBestLiveLocation(5_000).getProvider());
    }

    @Test
    public void silenceDisabledProviderAndResetReleaseThePreference() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        NavigationLocation network = fix(NETWORK, 1_000, 20);
        coordinator.remember(network);
        coordinator.markDispatched(network, 1_000);
        coordinator.remember(fix(GPS, 8_999, 40));
        assertEquals(NETWORK, coordinator.selectBestLiveLocation(8_999).getProvider());
        coordinator.remember(fix(GPS, 9_000, 40));
        assertEquals(GPS, coordinator.selectBestLiveLocation(9_000).getProvider());
        coordinator.clearProvider(NETWORK);
        assertEquals(GPS, coordinator.selectBestLiveLocation(9_000).getProvider());
        coordinator.reset();
        coordinator.remember(network);
        coordinator.remember(fix(GPS, 1_000, 22));
        assertEquals(GPS, coordinator.selectBestLiveLocation(1_000).getProvider());
    }

    @Test
    public void accuracyCannotPromoteAnOlderMeasurementOrCreateAnEarlyMarginalHandover() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        NavigationLocation network = fix(NETWORK, 10_000, 20);
        coordinator.remember(network);
        coordinator.markDispatched(network, 10_000);
        coordinator.remember(fix(GPS, 9_500, 5));
        LiveLocationCoordinator.Selection older = coordinator.selectBestLiveSelection(10_100);
        assertEquals(GPS, older.location.getProvider());
        assertEquals("older_than_last_accepted", coordinator.dispatchRejectionReason(older, 10_100, 3_000));
        assertFalse(coordinator.shouldDispatch(fix(GPS, 11_000, 18), 11_000, 3_000));
        assertTrue(coordinator.shouldDispatch(fix(GPS, 11_000, 5), 11_000, 3_000));
    }

    private static Replay replay(boolean current) throws Exception {
        return replay("legacy-handover-20261030", current);
    }

    private static Replay replay(String name, boolean current) throws Exception {
        Replay result = new Replay();
        for (LocationLogReplay.Sample sample : LocationLogReplay.load(name)) {
            result.accept(sample, current);
        }
        return result;
    }

    private static NavigationLocation fix(String provider, long time, float accuracy) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(time, time);
        location.setAccuracy(accuracy);
        return location;
    }

    private static final class Replay {
        private final LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        private final LatLonKalmanFilter filter = new LatLonKalmanFilter();
        private final NavigationLocationMotionModel motion = new NavigationLocationMotionModel();
        private NavigationLocation gps;
        private NavigationLocation network;
        private NavigationLocation fused;
        private NavigationLocation last;
        private long lastReceiptMs;
        private int switches;
        private int accepted;
        private int unknownHandovers;
        private long maxGapMs;
        private long maxAgeMs;
        private int movingUpdates;
        private long maxRapidGapMs;

        void accept(LocationLogReplay.Sample sample, boolean current) {
            coordinator.remember(sample.location);
            remember(sample.location);
            NavigationLocation selected = current ? coordinator.selectBestLiveLocation(sample.receivedAtMs)
                    : previousSelection(sample.receivedAtMs);
            if (selected == null || !coordinator.shouldDispatch(selected, sample.receivedAtMs, sample.requestedIntervalMs)) {
                return;
            }
            NavigationLocation filtered = filter.update(selected);
            if (filtered == null) {
                return;
            }
            motion.recordLocation(selected, filtered);
            recordHandover(selected, sample);
            if (motion.motionState() == NavigationMotionState.MOVING) {
                movingUpdates++;
            }
            accepted++;
            maxAgeMs = Math.max(maxAgeMs, sample.receivedAtMs - selected.getElapsedRealtimeOrTimeMs());
            lastReceiptMs = sample.receivedAtMs;
            last = selected;
            coordinator.markDispatched(selected, sample.receivedAtMs);
        }

        private void recordHandover(NavigationLocation selected, LocationLogReplay.Sample sample) {
            if (last == null) {
                return;
            }
            assertTrue(selected.getElapsedRealtimeOrTimeMs() > last.getElapsedRealtimeOrTimeMs());
            maxGapMs = Math.max(maxGapMs, sample.receivedAtMs - lastReceiptMs);
            if (sample.requestedIntervalMs <= 3_000) {
                maxRapidGapMs = Math.max(maxRapidGapMs, sample.receivedAtMs - lastReceiptMs);
            }
            if (last.getProvider().equals(selected.getProvider())) {
                return;
            }
            switches++;
            if (motion.motionState() == NavigationMotionState.UNKNOWN) {
                unknownHandovers++;
            }
        }

        private void remember(NavigationLocation location) {
            if (GPS.equals(location.getProvider())) {
                gps = location;
            } else if (NETWORK.equals(location.getProvider())) {
                network = location;
            } else {
                fused = location;
            }
        }

        private NavigationLocation previousSelection(long nowMs) {
            NavigationLocationFix selected = LiveLocationPolicy.selectBestFix(asFix(gps), asFix(network), asFix(fused), nowMs);
            if (selected == null) {
                return null;
            }
            return GPS.equals(selected.provider) ? gps : NETWORK.equals(selected.provider) ? network : fused;
        }

        private static NavigationLocationFix asFix(NavigationLocation location) {
            return location == null ? null : NavigationLocationFix.from(location);
        }

        @Override public String toString() {
            return "switches=" + switches + ",accepted=" + accepted + ",maxGapMs=" + maxGapMs + ",maxAgeMs=" + maxAgeMs
                    + ",moving=" + movingUpdates + ",maxRapidGapMs=" + maxRapidGapMs;
        }
    }
}
