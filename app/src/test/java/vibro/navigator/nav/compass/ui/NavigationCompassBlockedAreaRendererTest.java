package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassBlockedArea;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.compass.NavCompassStateInput;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassBlockedAreaRendererTest {
    @Test
    public void draw_usesEachBlockedAreaRadiusAtCompassScale() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassBlockedAreaRenderer renderer = new NavigationCompassBlockedAreaRenderer();
        CompassBlockedArea smallArea = new CompassBlockedArea(20f, 0f, 10f);
        CompassBlockedArea largeArea = new CompassBlockedArea(0f, 30f, 25f);
        NavCompassState state = compassState(Arrays.asList(smallArea, largeArea));
        Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));

        renderer.draw(canvas, activity, state, 120f, 120f, 100f, 0f);

        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        float scale = 100f / state.radiusState.visibleRadiusMeters;
        assertEquals(2, shadowCanvas.getCirclePaintHistoryCount());
        assertCircle(shadowCanvas.getDrawnCircle(0), 120f + 20f * scale, 120f, 10f * scale, activity);
        assertCircle(shadowCanvas.getDrawnCircle(1), 120f, 120f - 30f * scale, 25f * scale, activity);
    }

    @Test
    public void blockedAreaPaintUsesTransparentDarkerRed() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassBlockedAreaRenderer renderer = new NavigationCompassBlockedAreaRenderer();

        Paint paint = renderer.blockedAreaPaintForTest(activity);

        assertEquals(ContextCompat.getColor(activity, R.color.compass_blocked_area), paint.getColor());
        assertEquals(NavigationCompassBlockedAreaRenderer.BLOCKED_AREA_ALPHA, paint.getAlpha());
    }

    private static NavCompassState compassState(List<CompassBlockedArea> blockedAreas) {
        List<LatLon> track = Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001));
        GeoJsonRoute route = new GeoJsonRoute(track, Collections.emptyList(), 0.0, 111.0);
        NavCompassState state = NavCompassStateFactory.buildCompassState(
                NavCompassStateInput.builder(route, new PolylineIndex(track), location())
                        .routeProgress(0.0)
                        .motion(0f, true, 5f)
                        .heading(0.0, null)
                        .blockedAreas(blockedAreas)
                        .nowMs(0L)
                        .build()
        );
        assertNotNull(state);
        return state;
    }

    private static void assertCircle(
            ShadowCanvas.CirclePaintHistoryEvent circle,
            float centerX,
            float centerY,
            float radius,
            Activity activity
    ) {
        assertEquals(centerX, circle.centerX, 0.01f);
        assertEquals(centerY, circle.centerY, 0.01f);
        assertEquals(radius, circle.radius, 0.01f);
        assertEquals(ContextCompat.getColor(activity, R.color.compass_blocked_area), circle.paint.getColor());
        assertEquals(NavigationCompassBlockedAreaRenderer.BLOCKED_AREA_ALPHA, circle.paint.getAlpha());
    }

    private static NavigationLocation location() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        location.setTime(0L);
        location.setAccuracy(5f);
        return location;
    }
}
