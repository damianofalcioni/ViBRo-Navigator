package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.compass.NavCompassStateInput;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassTravelTimeTest {
    @Test
    @Config(qualifiers = "port")
    public void walkingTimesUseResolvedSpeedInCompactAndFullscreenPortrait() {
        assertWalkingSurfaces();
    }

    @Test
    @Config(qualifiers = "land")
    public void walkingTimesUseResolvedSpeedInCompactAndFullscreenLandscape() {
        assertWalkingSurfaces();
    }

    @Test
    public void screenshotDistanceTakesSevenMinutesAtFourKmh() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavCompassState state = movingState(4f / 3.6f).withDisplayMode(true, 468f);
        assertRenderedPairs(activity, state, false, 0f, false);
        assertEquals("7 min", NavigationTextFormatter.formatTimeSeconds(activity,
                Math.round(468f / state.displayMode.referenceSpeedMps)));
    }

    @Test
    public void unavailableSpeedsKeepFallbackAndVerySlowSpeedsKeepTimingFloor() {
        assertEquals(0.2f, movingState(0.1f).displayMode.referenceSpeedMps, 0.001f);
        for (float speed : new float[]{0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(1f, movingState(speed).displayMode.referenceSpeedMps, 0.001f);
        }
    }

    private static void assertWalkingSurfaces() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (float speed : new float[]{4f / 3.6f, 0.5f}) {
            NavCompassState state = movingState(speed);
            assertEquals(90f, state.radiusState.movingScaleVisibleRadiusMeters, 0.01f);
            assertEquals(speed, state.displayMode.referenceSpeedMps, 0.001f);
            assertSurfaces(activity, state);
            // Enlarged manual/transition radii must preserve the same travel speed.
            assertSurfaces(activity, state.withDisplayMode(true, 180f));
            assertSurfaces(activity, state.withDisplayMode(false, 180f));
        }
    }

    private static void assertSurfaces(Activity activity, NavCompassState state) {
        for (boolean fullscreen : new boolean[]{false, true}) {
            assertRenderedPairs(activity, state, fullscreen, 0f, false);
            assertRenderedPairs(activity, state, fullscreen, 1f, false);
            assertRenderedPairs(activity, state, fullscreen, 1f, true);
        }
    }

    private static void assertRenderedPairs(
            Activity activity, NavCompassState state, boolean fullscreen, float tilt, boolean central
    ) {
        AppCompassSettings.setCentralPerspectiveEnabled(activity, central);
        float sourceScale = tilt > 0f ? CompassPerspectiveScale.maximumViewportMultiplier(central) : 1f;
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.setCompassState(state.withDisplayMode(state.displayMode.movingScaleActive,
                state.radiusState.visibleRadiusMeters * sourceScale));
        view.setPerspectiveProgress(tilt);
        view.layout(0, 0, 300, 300);
        Canvas canvas = new Canvas(Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888));
        view.draw(canvas);
        assertTravelPairs(activity, Shadows.shadowOf(canvas), state.displayMode.referenceSpeedMps);
    }

    private static void assertTravelPairs(Activity activity, ShadowCanvas canvas, float speed) {
        int pairCount = 0;
        for (int i = 0; i < canvas.getTextHistoryCount() - 1; i++) {
            ShadowCanvas.TextHistoryEvent distance = canvas.getDrawnTextEvent(i);
            if (distance.paint.getTextAlign() == Paint.Align.LEFT && distance.text.endsWith(" m")) {
                float meters = Float.parseFloat(distance.text.substring(0, distance.text.length() - 2));
                assertEquals(NavigationTextFormatter.formatTimeSeconds(activity, Math.round(meters / speed)),
                        canvas.getDrawnTextEvent(i + 1).text);
                pairCount++;
            }
        }
        assertTrue(pairCount > 0);
    }

    private static NavCompassState movingState(float speed) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        location.setSpeed(speed);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.01)),
                Collections.emptyList(), 800.0, 1_111.0);
        NavCompassState state = NavCompassStateFactory.buildCompassState(
                NavCompassStateInput.builder(route, new PolylineIndex(route.track), location)
                        .motion(speed, false, 5f).build());
        assertNotNull(state);
        return state;
    }
}
