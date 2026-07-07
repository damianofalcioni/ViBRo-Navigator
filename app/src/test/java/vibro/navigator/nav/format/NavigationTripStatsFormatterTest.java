package vibro.navigator.nav.format;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.model.NavTripStatus;

public class NavigationTripStatsFormatterTest {
    @Test
    public void formatDetails_formatsTripMetrics() {
        NavTripStatus status = new NavTripStatus(
                true,
                0L,
                120_000L,
                1_000.0,
                60_000L,
                60_000L,
                10f,
                3,
                false,
                false
        );

        String details = NavigationTripStatsFormatter.formatDetails(
                TestNavigationTextResources.metric(),
                status,
                120_000L
        );

        assertTrue(details.contains("Elapsed: 2 min 00 s"));
        assertTrue(details.contains("Distance: 1.0 km"));
        assertTrue(details.contains("Stationary time: 1 min 00 s"));
        assertTrue(details.contains("Average speed: 30 km/h"));
        assertTrue(details.contains("Moving average: 60 km/h"));
        assertTrue(details.contains("Max speed: 36 km/h"));
    }

    @Test
    public void formatDetails_updatesOpenStationaryIntervalFromCurrentTime() {
        NavTripStatus status = new NavTripStatus(
                true,
                0L,
                90_000L,
                0.0,
                0L,
                90_000L,
                0f,
                2,
                false,
                true
        );

        String details = NavigationTripStatsFormatter.formatDetails(
                TestNavigationTextResources.metric(),
                status,
                120_000L
        );

        assertTrue(details.contains("Elapsed: 2 min 00 s"));
        assertTrue(details.contains("Stationary time: 2 min 00 s"));
    }

    @Test
    public void formatDetails_usesUnavailableValuesBeforeTripStarts() {
        String details = NavigationTripStatsFormatter.formatDetails(
                TestNavigationTextResources.metric(),
                NavTripStatus.unavailable(),
                120_000L
        );

        assertTrue(details.contains("Elapsed: --"));
        assertTrue(details.contains("Distance: --"));
        assertTrue(details.contains("Moving time: --"));
        assertTrue(details.contains("Stationary time: --"));
        assertTrue(details.contains("Average speed: --"));
        assertTrue(details.contains("Moving average: --"));
        assertTrue(details.contains("Max speed: --"));
    }
}
