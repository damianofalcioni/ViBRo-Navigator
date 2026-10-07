package vibro.navigator.nav.location;

import org.junit.Test;

import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavGpsTelemetry;

import static org.junit.Assert.assertEquals;

public class NavigationGpsFixAgeTest {
    private static final String ZERO_AGE_TEXT = "0 s";
    @Test
    public void fixAgeIsVisibleImmediatelyAndUpdatesWithoutNewLocationOrWallClock() {
        assertEquals(ZERO_AGE_TEXT, ageText(1000, 1000));
        assertEquals(ZERO_AGE_TEXT, ageText(1000, 1999));
        assertEquals("1 s", ageText(1000, 2000));
        assertEquals("4 s", ageText(1000, 5999));
        assertEquals("5 s", ageText(1000, 6000));
        assertEquals("59 s", ageText(1000, 60999));
        assertEquals("60 s", ageText(1000, 61000));
    }

    @Test
    public void unavailableOrFutureMonotonicTimeDoesNotProduceFictitiousAge() {
        assertEquals("--", ageText(-1, 61000));
        assertEquals("--", ageText(62000, 61000));
    }

    @Test
    public void detailsKeepFixedAgeRowAfterIntervalWithoutChangingGpsTime() {
        NavGpsTelemetry telemetry = NavigationGpsTelemetryFormatter.format(
                TestNavigationTextResources.metric(), 0, (NavigationLocation) null, 5, 15, 1)
                .withObtainedTimeText("08:44:00").withFixElapsedRealtimeMs(1000);
        assertDetailsRow(telemetry, 1000, ZERO_AGE_TEXT);
        assertDetailsRow(telemetry, 4999, "3 s");
        assertDetailsRow(telemetry, 8000, "7 s");
        assertDetailsRow(telemetry, 61000, "60 s");
        assertDetailsRow(telemetry.withFixElapsedRealtimeMs(61000), 61000, ZERO_AGE_TEXT);
        assertDetailsRow(telemetry.withFixElapsedRealtimeMs(-1), 61000, "--");
    }

    @Test
    public void telemetryRetainsMonotonicFixTimeAcrossCopies() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1700000000000L, 1000);
        location.setAccuracy(5);
        NavGpsTelemetry telemetry = NavigationGpsTelemetryFormatter.format(
                TestNavigationTextResources.metric(), 0, location, 5, 15, 1);
        assertEquals(1000, telemetry.fixElapsedRealtimeMs);
        assertEquals(1000, telemetry.withCompactLine("updated").fixElapsedRealtimeMs);
        assertEquals(1000, telemetry.withObtainedTimeText("updated time").fixElapsedRealtimeMs);
        location.setTime(1700000000000L);
        assertEquals(-1, NavigationGpsTelemetryFormatter.format(
                TestNavigationTextResources.metric(), 0, location, 5, 15, 1).fixElapsedRealtimeMs);
    }

    private static String ageText(long fixTime, long now) {
        return NavigationGpsTextFormatter.formatFixAge(TestNavigationTextResources.metric(), fixTime, now);
    }

    private static void assertDetailsRow(NavGpsTelemetry telemetry, long now, String expectedAge) {
        String[] lines = NavigationGpsTelemetryFormatter.formatDetails(
                TestNavigationTextResources.metric(), telemetry, "60 s", now).split("\n");
        assertEquals("GPS obtained: 08:44:00", lines[3]);
        assertEquals("Interval: 60 s", lines[5]);
        assertEquals("Last fix age: " + expectedAge, lines[6]);
    }
}
