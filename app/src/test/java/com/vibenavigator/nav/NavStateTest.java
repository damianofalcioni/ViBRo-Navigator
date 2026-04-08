package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavStateTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void from_skipsHintsInsideAccuracyRadius() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.00005),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(2, 5, 0, 0.0, 0),
                        new VoiceHint(3, 3, 0, 0.0, 0)
                ),
                180.0,
                222.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                0,
                1f,
                20f,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 222.0)),
                context
        );

        assertTrue(state.nextLine.contains("111 m"));
        assertTrue(state.afterNextLine.contains("222 m"));
    }
}
