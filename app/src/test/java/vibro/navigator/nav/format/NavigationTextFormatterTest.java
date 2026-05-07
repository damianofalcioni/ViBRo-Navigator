package vibro.navigator.nav.format;


import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.settings.AppSettings;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationTextFormatterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AppSettings.setImperialUnitsEnabled(context, false);
    }

    @After
    public void tearDown() {
        AppSettings.setImperialUnitsEnabled(context, false);
    }

    @Test
    public void formatDistance_usesMetricUnitsByDefault() {
        assertEquals("120 m", NavigationTextFormatter.formatDistance(context, 120.0));
        assertEquals("1.2 km", NavigationTextFormatter.formatDistance(context, 1234.0));
    }

    @Test
    public void formatDistance_usesImperialUnitsWhenEnabled() {
        AppSettings.setImperialUnitsEnabled(context, true);

        assertEquals("394 ft", NavigationTextFormatter.formatDistance(context, 120.0));
        assertEquals("1.0 mi", NavigationTextFormatter.formatDistance(context, 1609.344));
    }

    @Test
    public void formatGpsMeasurements_useImperialUnitsWhenEnabled() {
        AppSettings.setImperialUnitsEnabled(context, true);

        assertEquals("10 mph", NavigationTextFormatter.formatSpeed(context, 4.5f));
        assertEquals("805 ft", NavigationTextFormatter.formatElevation(context, 245.4));
        assertEquals("±16 ft", NavigationTextFormatter.formatAccuracy(context, 5f));
    }

    @Test
    public void formatOffRouteNotification_formatsOffTrackDetails() {
        String message = NavigationTextFormatter.formatOffRouteNotification(
                context,
                NavigationRerouteNotice.fromDecision(
                        new RouteDeviationPolicy().evaluate(25.0, 8f, 90.0, 90.0)
                )
        );

        assertEquals("Off-track detected. Distance 25 m, threshold 16 m. Recalculating route.", message);
    }

    @Test
    public void formatOffRouteNotification_formatsBearingMismatchDetails() {
        String message = NavigationTextFormatter.formatOffRouteNotification(
                context,
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
        String message = NavigationTextFormatter.formatStationaryOrientationNotification(
                context,
                new StationaryOrientationAdvisor.Decision(-42.0)
        );

        assertEquals("Turn yourself 42° left to face the route.", message);
    }

    @Test
    public void formatTurnNotification_includesRoundaboutExitNumberInSymbolAndText() {
        String message = NavigationTextFormatter.formatTurnNotification(
                context,
                new VoiceHint(0, 13, 3, 0, 0),
                120.0,
                8.0
        );

        assertEquals("○>3 120 m - 8 s - Roundabout, exit 3", message);
    }

    @Test
    public void formatTurnNotification_formatsArrivalWithoutDistanceAndCountdown() {
        String message = NavigationTextFormatter.formatTurnNotification(
                context,
                new VoiceHint(1, 100, 0, 0.0, 0),
                0.0,
                0.0
        );

        assertEquals("■ Destination reached", message);
    }

    @Test
    public void formatTurnNotification_formatsUpcomingArrivalWithDistanceAndCountdown() {
        String message = NavigationTextFormatter.formatTurnNotification(
                context,
                new VoiceHint(1, 100, 0, 0.0, 0),
                120.0,
                8.0
        );

        assertEquals("■ 120 m - 8 s - Destination reached", message);
    }
}
