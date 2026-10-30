package vibro.navigator.nav.session;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavTripStatus;
import static org.junit.Assert.*;
import static vibro.navigator.nav.location.NavigationLocationProviders.GPS_PROVIDER;

public class NavigationTripMotionExpiryTest {
    private final NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();

    @Test
    public void expiredAndFutureSpeedReadingsCannotSetTheTripMaximum() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, false);
        NavigationLocation delayed = new NavigationLocation(GPS_PROVIDER);
        delayed.setTime(4_000, 4_000);
        tracker.recordAcceptedLocation(delayed, 15_000, 20, false, false, -1, true);
        assertEquals(1f, tracker.snapshot().maxSpeedMps, 0);
        delayed.setTime(18_000, 18_000);
        tracker.recordAcceptedLocation(delayed, 17_000, 30, false, false, -1, true);
        assertEquals(1f, tracker.snapshot().maxSpeedMps, 0);
        delayed.setTime(20_000, 20_000);
        tracker.recordAcceptedLocation(delayed, 20_000, 2, false, false, -1, true);
        assertEquals(2f, tracker.snapshot().maxSpeedMps, 0);
    }

    @Test
    public void theFreshnessBoundaryStillAllowsAUsableMaximumSpeed() {
        tracker.start(1_000);
        NavigationLocation location = new NavigationLocation(GPS_PROVIDER);
        location.setTime(1_000, 1_000);
        tracker.recordAcceptedLocation(location, 11_000, 2, false, false, -1, true);
        assertEquals(2f, tracker.snapshot().maxSpeedMps, 0);
        tracker.recordAcceptedLocation(location, 11_001, 20, false, false, -1, true);
        assertEquals(2f, tracker.snapshot().maxSpeedMps, 0);
    }

    @Test
    public void unknownMotionSettlesTheSupportedPrefixAndCannotReopenIt() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, false);
        assertEquals(10_000, tracker.snapshot().movingDurationMs(12_000));
        NavigationLocation uncertain = new NavigationLocation(GPS_PROVIDER);
        uncertain.setTime(12_000, 12_000);
        tracker.recordAcceptedLocation(uncertain, 12_000, 0, false, false, -1, false);
        assertEquals(10_000, tracker.snapshot().movingDurationMs(31_000));
        accept(31_000, 31_000, 30, false);
        assertEquals(10_000, tracker.snapshot().movingDurationMs);
        assertEquals(0d, tracker.snapshot().travelledDistanceMeters, 0d);
    }

    @Test
    public void reacquisitionRetainsSupportedStationaryTimeWithoutConnectingDistance() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, true);
        assertEquals(10_000, tracker.snapshot().stationaryDurationMs(61_000));
        NavigationLocation relocated = new NavigationLocation(GPS_PROVIDER);
        relocated.setTime(61_000, 61_000);
        relocated.setLatitude(0.01);
        tracker.recordAcceptedLocation(relocated, 61_000, 1, false, true, -1, true);
        assertEquals(10_000, tracker.snapshot().stationaryDurationMs);
        assertEquals(0, tracker.snapshot().movingDurationMs);
        assertEquals(0d, tracker.snapshot().travelledDistanceMeters, 0d);
    }

    @Test
    public void staleFixStillClosesTheEarlierSupportedStationaryPrefix() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, true);
        accept(2_000, 15_000, 0, false);
        assertEquals(10_000, tracker.snapshot().stationaryDurationMs);
        assertEquals(0, tracker.snapshot().movingDurationMs(31_000));
    }

    @Test
    public void aStopAfterSilenceCannotRelabelTheEarlierSupportedMovingPrefix() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, false);
        accept(31_000, 31_000, 0, true);
        assertEquals(10_000, tracker.snapshot().movingDurationMs);
        assertEquals(0, tracker.snapshot().stationaryDurationMs);
        assertEquals(10_000, tracker.snapshot().stationaryDurationMs(61_000));
    }

    @Test
    public void providerOffsetsCannotBecomeDistanceWhileMeasuredMotionRemainsUsable() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, false);
        NavigationLocation fix = new NavigationLocation("network");
        fix.setTime(4_000, 4_000);
        fix.setLatitude(100 / 111_320.0);
        tracker.recordAcceptedLocation(fix, 4_000, 1, false, false, -1, true);
        assertEquals(3_000, tracker.snapshot().movingDurationMs);
        assertEquals(0d, tracker.snapshot().travelledDistanceMeters, 0d);
    }

    @Test
    public void silentIntervalExpiresInLiveAndClosedMovingTotalsWithoutAnUnknownCallback() {
        tracker.start(1_000);
        accept(1_000, 1_000, 0, false);
        NavTripStatus status = tracker.snapshot();
        assertEquals(10_000, status.movingDurationMs(31_000));
        assertEquals(30_000, status.elapsedDurationMs(31_000));
        accept(61_000, 61_000, 60, false);
        assertEquals(10_000, tracker.snapshot().movingDurationMs);
        assertEquals(0d, tracker.snapshot().travelledDistanceMeters, 0d);
        accept(64_000, 64_000, 63, false);
        assertEquals(13_000, tracker.snapshot().movingDurationMs);
        assertTrue(tracker.snapshot().travelledDistanceMeters < 3.1);
    }

    @Test
    public void observationAgeReducesTheRemainingLiveStationaryWindow() {
        tracker.start(1_000);
        accept(1_000, 5_000, 0, true);
        assertEquals(6_000, tracker.snapshot().stationaryDurationMs(31_000));
        accept(31_000, 31_000, 0, true);
        assertEquals(6_000, tracker.snapshot().stationaryDurationMs);
    }

    @Test
    public void staleAndFutureObservationsCannotOpenOrBridgeKnownIntervals() {
        tracker.start(1_000);
        accept(1_000, 12_000, 0, false);
        assertEquals(0, tracker.snapshot().movingDurationMs(15_000));
        accept(20_000, 19_000, 10, false);
        assertEquals(0, tracker.snapshot().movingDurationMs(20_000));
        accept(22_000, 22_000, 20, false);
        assertEquals(0d, tracker.snapshot().travelledDistanceMeters, 0d);
    }

    private void accept(long measured, long received, double north, boolean stationary) {
        NavigationLocation fix = new NavigationLocation(GPS_PROVIDER);
        fix.setTime(measured, measured);
        fix.setLatitude(north / 111_320.0);
        tracker.recordAcceptedLocation(fix, received, stationary ? 0f : 1f, stationary, false, -1, true);
    }
}
