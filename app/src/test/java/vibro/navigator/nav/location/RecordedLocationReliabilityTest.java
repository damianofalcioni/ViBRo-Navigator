package vibro.navigator.nav.location;

import org.junit.Test;
import vibro.navigator.nav.kalman.LatLonKalmanFilter;
import static org.junit.Assert.*;

public class RecordedLocationReliabilityTest {
    @Test
    public void onePreciseZeroSpeedFixCannotEstablishAStop() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        model.recordFilteredLocation(fix(1_000, 0, 5, 0));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        model.recordFilteredLocation(fix(4_000, 0, 5, 0));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
    }

    @Test
    public void recordedDeparturesDoNotRetainConfidentStopThroughTheWholeExcerpt() throws Exception {
        assertDeparture("gnss-departure");
        assertDeparture("fused-departure");
    }

    private static void assertDeparture(String name) throws Exception {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        int nonStationary = 0;
        for (LocationLogReplay.Sample sample : LocationLogReplay.load(name)) {
            NavigationLocation filtered = filter.update(sample.location);
            if (filtered != null) {
                model.recordLocation(sample.location, filtered);
                if (!model.isLikelyStationary()) {
                    nonStationary++;
                }
            }
        }
        assertTrue("Ambiguous observations must not all claim a confirmed stop: " + name, nonStationary >= 3);
    }

    @Test
    public void coherentWalkingUsesRawQualityDespiteAddedFilteredLag() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (int i = 0; i < 8; i++) {
            NavigationLocation raw = fix(i * 3_000L, i * 4, 38, 1.3f);
            NavigationLocation filtered = new NavigationLocation(raw);
            filtered.setLatitude(Math.max(0, i * 4 - 20) / 111_320.0);
            filtered.setFilteredAccuracy(60);
            model.recordLocation(raw, filtered);
        }
        assertEquals(NavigationMotionState.MOVING, model.motionState());
        assertFalse(model.needsDepartureChecks());
    }

    @Test
    public void poorDriftAndOnePreciseSpeedlessJumpCannotConfirmMovementOrAStop() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        model.recordFilteredLocation(fix(0, 0, 5, 0));
        for (int i = 1; i <= 6; i++) {
            model.recordFilteredLocation(fix(i * 3_000L, i % 2 == 0 ? -12 : 15, 60, 0.6f));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
            assertTrue(model.needsDepartureChecks());
        }
    }

    @Test
    public void walkingRemainsResponsiveAcrossSparseCadenceBoundaries() {
        for (long interval : new long[] {12_000, 15_000, 20_000, 60_000}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            for (int i = 0; i < 8; i++) {
                NavigationLocation raw = fix(i * interval, i * interval * 1.2 / 1_000, 30, 1.2f);
                NavigationLocation filtered = filter.update(raw);
                assertNotNull(filtered);
                if (i >= 3) {
                    assertTrue("Added lag at " + interval + " ms", raw.distanceTo(filtered) <= 3.1);
                }
            }
        }
    }

    @Test
    public void walkingFilterAddsAtMostTwoSecondsOrThreeMetersOfLagAfterCorroboration() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        for (int i = 0; i < 15; i++) {
            NavigationLocation raw = fix(i * 3_000L, i * 3, 20, 1);
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            if (i >= 4) {
                assertTrue(raw.distanceTo(filtered) <= 3.1);
            }
        }
    }

    private static NavigationLocation fix(long time, double north, float accuracy, float speed) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLatitude(north / 111_320);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        return location;
    }
}
