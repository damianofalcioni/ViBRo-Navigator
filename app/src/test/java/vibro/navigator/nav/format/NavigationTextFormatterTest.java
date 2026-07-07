package vibro.navigator.nav.format;


import static org.junit.Assert.assertEquals;

import java.util.TimeZone;

import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
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
    public void formatGpsMeasurements_hidesInvalidAccuracySentinel() {
        assertEquals("--", NavigationMeasurementFormatter.formatAccuracy(METRIC, Float.MAX_VALUE));
    }

    @Test
    public void buildGpsStatusLine_hidesMissingStartupAccuracy() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(48.2082);
        location.setLongitude(16.3738);

        String line = NavCompassStateFactory.buildGpsStatusLine(
                0f,
                location,
                Float.MAX_VALUE,
                null,
                1,
                METRIC
        );

        assertEquals("0 km/h ↑-- • -- • (--)", line);
    }

    @Test
    public void buildGpsStatusTelemetry_keepsDetailedFixDataOffCompactLine() {
        TimeZone previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            NavigationLocation location = new NavigationLocation("gps");
            location.setLatitude(48.2082);
            location.setLongitude(16.3738);
            location.setTime(1_234L);
            location.setAltitude(245.4);
            location.setBearing(182.2f);
            location.setBearingAccuracyDegrees(9.4f);

            String details = NavigationGpsTelemetryFormatter.formatDetails(
                    METRIC,
                    NavigationGpsTelemetryFormatter.format(
                            METRIC,
                            4.5f,
                            location,
                            5.2f,
                            7,
                            3
                    ),
                    "8 s"
            );

            assertEquals(
                    "Speed: 16 km/h\nAltitude: 245 m\nAccuracy: ±5 m\nGPS obtained: 00:00:01\n"
                            + "Satellites: 7\nInterval: 8 s\nGPS fixes: #3\nGPS bearing: 182°\n"
                            + "Bearing accuracy: 9°",
                    details
            );
        } finally {
            TimeZone.setDefault(previousTimeZone);
        }
    }

    @Test
    public void buildGpsStatusTelemetry_formatsAccuracyInMetersForImperialGpsLine() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(48.2082);
        location.setLongitude(16.3738);
        location.setAltitude(245.4);

        String line = NavCompassStateFactory.buildGpsStatusLine(
                4.5f,
                location,
                5f,
                null,
                1,
                IMPERIAL
        );

        assertEquals("10 mph ↑805 ft • ±5 m • (--)", line);
    }

    @Test
    public void formatOffRouteNotification_formatsOffTrackDetails() {
        NavigationRerouteNotice notice = NavigationRerouteNotice.fromDecision(
                new RouteDeviationPolicy().evaluate(25.0, 8f, 90.0, 90.0)
        );
        String message = NavigationTextFormatterRules.formatOffRouteNotification(
                METRIC,
                notice
        );

        assertEquals("Off-track detected. Distance 25 m, threshold 16 m. Recalculating route.", message);
        assertEquals(
                "Off-track detected. Distance 25 m, threshold 16 m.",
                NavigationTextFormatterRules.formatOffRouteNotification(METRIC, notice.asNotificationOnly())
        );
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
    public void formatWrongDirectionNotification_formatsTargetAndActualBearings() {
        String message = NavigationTextFormatterRules.formatWrongDirectionNotification(
                METRIC,
                new NavigationWrongDirectionNotice(90.0, 270.0, 180.0)
        );

        assertEquals("Wrong direction detected. Target 90°, actual 270°, diff 180°.", message);
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
