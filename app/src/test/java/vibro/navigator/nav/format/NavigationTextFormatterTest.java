package vibro.navigator.nav.format;


import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import static org.junit.Assert.assertEquals;

import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

public class NavigationTextFormatterTest {

    private static final TestNavigationTextResources METRIC = TestNavigationTextResources.metric();
    private static final TestNavigationTextResources IMPERIAL = TestNavigationTextResources.imperial();

    @Test
    public void formatDistance_usesMetricUnitsByDefault() {
        assertEquals("120 m", NavigationMeasurementFormatter.formatDistance(METRIC, 120.0));
        assertEquals("1.2 km", NavigationMeasurementFormatter.formatDistance(METRIC, 1234.0));
    }

    @Test
    public void formatDistance_usesImperialUnitsWhenEnabled() {
        assertEquals("394 ft", NavigationMeasurementFormatter.formatDistance(IMPERIAL, 120.0));
        assertEquals("1.0 mi", NavigationMeasurementFormatter.formatDistance(IMPERIAL, 1609.344));
    }

    @Test
    public void formatGpsMeasurements_useImperialUnitsWhenEnabled() {
        assertEquals("10 mph", NavigationMeasurementFormatter.formatSpeed(IMPERIAL, 4.5f));
        assertEquals("805 ft", NavigationMeasurementFormatter.formatElevation(IMPERIAL, 245.4));
        assertEquals("±16 ft", NavigationMeasurementFormatter.formatAccuracy(IMPERIAL, 5f));
    }

    @Test
    public void formatOffRouteNotification_formatsOffTrackDetails() {
        String message = NavigationTextFormatterRules.formatOffRouteNotification(
                METRIC,
                NavigationRerouteNotice.fromDecision(
                        new RouteDeviationPolicy().evaluate(25.0, 8f, 90.0, 90.0)
                )
        );

        assertEquals("Off-track detected. Distance 25 m, threshold 16 m. Recalculating route.", message);
    }

    @Test
    public void formatOffRouteNotification_formatsBearingMismatchDetails() {
        String message = NavigationTextFormatterRules.formatOffRouteNotification(
                METRIC,
                NavigationRerouteNotice.fromDecision(
                        new RouteDeviationPolicy().evaluate(5.0, 5f, 90.0, 180.0)
                )
        );

        assertEquals(
                "Bearing mismatch detected. Diff 90°, expected 180°, actual 90°. Recalculating route.",
                message
        );
    }

    @Test
    public void formatStationaryOrientationNotification_formatsRelativeTurn() {
        String message = NavigationTextFormatterRules.formatStationaryOrientationNotification(
                METRIC,
                new StationaryOrientationAdvisor.Decision(-42.0)
        );

        assertEquals("Turn yourself 42° left to face the route.", message);
    }

    @Test
    public void formatTurnNotification_includesRoundaboutExitNumberInSymbolAndText() {
        String message = NavigationTextFormatterRules.formatTurnNotification(
                METRIC,
                new VoiceHint(0, 13, 3, 0, 0),
                120.0,
                8.0
        );

        assertEquals("○>3 120 m - 8 s - Roundabout, exit 3", message);
    }

    @Test
    public void formatTurnNotification_formatsArrivalWithoutDistanceAndCountdown() {
        String message = NavigationTextFormatterRules.formatTurnNotification(
                METRIC,
                new VoiceHint(1, 100, 0, 0.0, 0),
                0.0,
                0.0
        );

        assertEquals("■ Destination reached", message);
    }

    @Test
    public void formatTurnNotification_formatsUpcomingArrivalWithDistanceAndCountdown() {
        String message = NavigationTextFormatterRules.formatTurnNotification(
                METRIC,
                new VoiceHint(1, 100, 0, 0.0, 0),
                120.0,
                8.0
        );

        assertEquals("■ 120 m - 8 s - Destination reached", message);
    }
}
