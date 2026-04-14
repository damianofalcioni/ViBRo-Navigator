package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationTextFormatterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

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
}
