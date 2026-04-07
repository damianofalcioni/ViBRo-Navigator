package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

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

        assertEquals("Off-track detected. Distance 25 m, threshold 18 m. Recalculating route.", message);
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
}
