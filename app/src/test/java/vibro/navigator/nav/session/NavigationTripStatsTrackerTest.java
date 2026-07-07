package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.location.NavigationLocation;

public class NavigationTripStatsTrackerTest {
    @Test
    public void recordAcceptedLocation_tracksTravelMovingAndStationaryTime() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L);

        tracker.recordAcceptedLocation(location(48.0, 16.0), 1_000L, 0f, true, false);
        tracker.recordAcceptedLocation(location(48.0, 16.0), 61_000L, 0f, true, false);
        tracker.recordAcceptedLocation(location(48.0, 16.001), 121_000L, 2f, false, false);

        NavTripStatus status = tracker.snapshot();

        assertEquals(60_000L, status.stationaryDurationMs);
        assertEquals(60_000L, status.movingDurationMs);
        assertEquals(2f, status.maxSpeedMps, 0.0f);
        assertEquals(3, status.acceptedFixCount);
        assertTrue(status.travelledDistanceMeters > 70.0);
    }

    @Test
    public void recordAcceptedLocation_skipsDistanceAndTimeAcrossReacquisitionSegment() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L);

        tracker.recordAcceptedLocation(location(48.0, 16.0), 1_000L, 2f, false, false);
        tracker.recordAcceptedLocation(location(49.0, 17.0), 601_000L, 2f, false, true);

        NavTripStatus status = tracker.snapshot();

        assertEquals(0L, status.movingDurationMs);
        assertEquals(0.0, status.travelledDistanceMeters, 0.0);
    }

    @Test
    public void snapshot_addsOpenStationaryIntervalToLiveDuration() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L);
        tracker.recordAcceptedLocation(location(48.0, 16.0), 1_000L, 0f, true, false);

        NavTripStatus status = tracker.snapshot();

        assertEquals(30_000L, status.stationaryDurationMs(31_000L));
    }

    private static NavigationLocation location(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(5f);
        return location;
    }
}
