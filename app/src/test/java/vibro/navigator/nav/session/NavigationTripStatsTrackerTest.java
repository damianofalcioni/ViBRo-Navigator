package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.power.NavigationBatterySnapshot;

public class NavigationTripStatsTrackerTest {
    @Test
    public void motionWakeCreditsSleepOnlyUntilWakeAndDoesNotCountItTwice() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L);
        tracker.recordAcceptedLocation(location(0, 0), 1_000L, 0f, true, false);
        tracker.recordAcceptedLocation(location(0, 0), 22_000L, 0f, true, false);
        tracker.recordAcceptedLocation(location(0, 0.0001), 26_000L, 2f, false, false, 25_000L);
        assertEquals(24_000L, tracker.snapshot().stationaryDurationMs);
        assertEquals(1_000L, tracker.snapshot().movingDurationMs);
        tracker.recordAcceptedLocation(location(0, 0.0002), 29_000L, 2f, false, false);
        assertEquals(24_000L, tracker.snapshot().stationaryDurationMs);
        assertEquals(4_000L, tracker.snapshot().movingDurationMs);
    }

    @Test
    public void sleepCreditDoesNotTurnAnUnobservedRelocationIntoTravel() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L);
        tracker.recordAcceptedLocation(location(0, 0), 1_000L, 0f, true, false);
        tracker.recordAcceptedLocation(location(0, 0.01), 40_000L, 2f, false, true, 31_000L);
        assertEquals(30_000L, tracker.snapshot().stationaryDurationMs);
        assertEquals(0L, tracker.snapshot().movingDurationMs);
        assertEquals(0d, tracker.snapshot().travelledDistanceMeters, 0d);
    }

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

    @Test
    public void recordScreenInteractive_tracksScreenOnAndOffDurations() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L, true, NavigationBatterySnapshot.unavailable());

        tracker.recordScreenInteractive(false, 61_000L);
        tracker.recordScreenInteractive(true, 91_000L);

        NavTripStatus status = tracker.snapshot();

        assertEquals(80_000L, status.screenOnDurationMs(111_000L));
        assertEquals(30_000L, status.screenOffDurationMs(111_000L));
    }

    @Test
    public void snapshot_reportsBatteryUsedAndDropFromStartAndLatestSamples() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L, true, NavigationBatterySnapshot.of(3_000_000, 90));

        tracker.recordBatterySnapshot(NavigationBatterySnapshot.of(2_987_500, 87));

        NavTripStatus status = tracker.snapshot();

        assertEquals(12.5f, status.batteryUsedMilliAmpHours, 0.0f);
        assertEquals(3, status.batteryDropPercent);
    }

    @Test
    public void snapshot_reportsBatteryDropWhenChargeCounterIsUnavailable() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L, true, NavigationBatterySnapshot.of(0, 90));

        tracker.recordBatterySnapshot(NavigationBatterySnapshot.of(0, 88));

        NavTripStatus status = tracker.snapshot();

        assertTrue(Float.isNaN(status.batteryUsedMilliAmpHours));
        assertEquals(2, status.batteryDropPercent);
    }

    @Test
    public void snapshot_reportsUnavailableBatteryWhenLatestSampleIsUnavailable() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000L, true, NavigationBatterySnapshot.of(3_000_000, 90));

        tracker.recordBatterySnapshot(NavigationBatterySnapshot.unavailable());

        NavTripStatus status = tracker.snapshot();

        assertTrue(Float.isNaN(status.batteryUsedMilliAmpHours));
        assertEquals(NavTripStatus.UNKNOWN_BATTERY_DROP_PERCENT, status.batteryDropPercent);
    }

    private static NavigationLocation location(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(5f);
        return location;
    }

    @Test
    public void risingDischargeCounterStaysUnreliableAfterLaterDecrease() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000, true, NavigationBatterySnapshot.of(3_000_000, 90));
        tracker.recordBatterySnapshot(NavigationBatterySnapshot.of(3_000_100, 89));
        tracker.recordBatterySnapshot(NavigationBatterySnapshot.of(2_999_000, 88));
        assertTrue(Float.isNaN(tracker.snapshot().batteryUsedMilliAmpHours));
        assertEquals(2, tracker.snapshot().batteryDropPercent);
    }

    @Test
    public void tinyVendorCounterCannotBecomeAnEnergyEstimate() {
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        tracker.start(1_000, true, NavigationBatterySnapshot.of(390, 75));
        tracker.recordBatterySnapshot(NavigationBatterySnapshot.of(248, 56));
        assertTrue(Float.isNaN(tracker.snapshot().batteryUsedMilliAmpHours));
        assertEquals(19, tracker.snapshot().batteryDropPercent);
    }
}
