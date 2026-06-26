package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.NavCompassState;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassStreetRendererTest {
    @Test
    public void surroundingStreetOverlayUsesEnabledSwitchColor() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();
        Paint streetPaint = renderer.paintForTest(activity);

        assertEquals(platformThemeColor(activity, android.R.attr.colorControlActivated), streetPaint.getColor());
        assertEquals(Paint.Style.STROKE, streetPaint.getStyle());
        assertEquals(180, streetPaint.getAlpha());
        assertNull(streetPaint.getPathEffect());
    }

    @Test
    public void surroundingStreetOverlayDrawsOnlyInZoomedMovingScaleView() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();

        assertEquals(0, pathDrawCountAfterDraw(activity, renderer, compassState(false)));
        assertEquals(1, pathDrawCountAfterDraw(activity, renderer, compassState(true)));
    }

    private static int pathDrawCountAfterDraw(
            Activity activity,
            NavigationCompassStreetRenderer renderer,
            NavCompassState state
    ) {
        Canvas canvas = new Canvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888));
        renderer.draw(canvas, activity, state, 100f, 100f, 80f, 0f);
        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        return shadowCanvas.getPathPaintHistoryCount();
    }

    private static NavCompassState compassState(boolean movingScaleActive) {
        CompassRouteGeometry routeGeometry = new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.001), 100.0)
                ),
                Collections.emptyList()
        );
        NavCompassState state = NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                100f,
                1_000f,
                100f,
                5f,
                movingScaleActive,
                0f,
                routeGeometry,
                0.0,
                0.0,
                0,
                0f,
                100f,
                5f,
                true
        );
        return state.withStreetOverlay(streetOverlay());
    }

    private static CompassStreetOverlay streetOverlay() {
        CompassStreetSegment streetSegment = new CompassStreetSegment(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0005, 0.0)
        ));
        return new CompassStreetOverlay(Collections.singletonList(streetSegment));
    }

    private static int platformThemeColor(Activity activity, int attrResId) {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(attrResId, value, true);
        if (value.resourceId != 0) {
            return ContextCompat.getColor(activity, value.resourceId);
        }
        return value.data;
    }
}
