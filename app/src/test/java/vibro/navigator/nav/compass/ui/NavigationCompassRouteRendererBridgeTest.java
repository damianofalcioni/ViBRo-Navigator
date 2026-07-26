package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
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

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassRouteGeometryFactory;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassRouteRendererBridgeTest {

    @Test
    public void drawRouteLayer_recalculationBridgeUsesDottedRedDirectRoutePaint() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();
        Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));

        renderer.drawRouteLayer(canvas, activity, compassStateWithBridge(), 120f, 120f, 100f, 0f);

        assertTrue(hasDottedRedPath(activity, Shadows.shadowOf(canvas)));
    }

    @Test
    public void drawRouteLayer_movingScaleUsesFullResolutionRoutePoints() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();
        Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));

        renderer.drawRouteLayer(canvas, activity, movingStateWithLocalFullRoute(), 120f, 120f, 100f, 0f);

        assertTrue(hasSolidRedPath(activity, Shadows.shadowOf(canvas)));
    }

    @Test
    public void drawRouteLayer_movingScaleDoesNotFallBackToOverviewShortcut() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();
        Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));

        renderer.drawRouteLayer(canvas, activity, movingStateWithDistantDetour(), 120f, 120f, 100f, 0f);

        assertFalse(hasSolidRedPath(activity, Shadows.shadowOf(canvas)));
    }

    @Test
    public void drawRouteLayer_beelineSegmentIsDottedAndHasNoSolidThresholdPath() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();
        Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));

        renderer.drawRouteLayer(canvas, activity, beelineOnlyState(), 120f, 120f, 100f, 0f);

        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        assertTrue(hasDottedRedPath(activity, shadowCanvas));
        assertFalse(hasSolidRedPath(activity, shadowCanvas));
    }

    private static NavCompassState compassStateWithBridge() {
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                220f,
                220f,
                220f,
                5f,
                false,
                0f,
                bridgeGeometry(),
                0.0,
                0.0,
                1,
                0f,
                111f,
                10f,
                true
        );
    }

    private static NavCompassState beelineOnlyState() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(0, 16, 0, 111.0, 0)),
                80.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);
        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(route, index);
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                220f,
                220f,
                220f,
                5f,
                false,
                13f,
                geometry,
                0.0,
                0.0,
                1,
                0f,
                111f,
                10f,
                true
        );
    }

    private static CompassRouteGeometry bridgeGeometry() {
        return new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.001), 111.0)
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(Arrays.asList(
                        new LatLon(0.0, -0.002),
                        new LatLon(0.0, -0.001)
                )),
                Collections.singletonList(Arrays.asList(
                        new LatLon(0.0, -0.001),
                        new LatLon(0.0, 0.0)
                ))
        );
    }

    private static NavCompassState movingStateWithLocalFullRoute() {
        CompassRouteGeometry geometry = new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.01), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.02), 1_111.0)
                ),
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0005), 55.0)
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                100f,
                2_500f,
                100f,
                5f,
                true,
                13f,
                geometry,
                0.0,
                0.0,
                1,
                0f,
                0f,
                10f,
                true
        );
    }

    private static NavCompassState movingStateWithDistantDetour() {
        CompassRouteGeometry.SamplePoint first =
                new CompassRouteGeometry.SamplePoint(new LatLon(0.0, -0.01), 0.0);
        CompassRouteGeometry.SamplePoint last =
                new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.01), 3_333.0);
        CompassRouteGeometry geometry = new CompassRouteGeometry(
                Arrays.asList(first, last),
                Arrays.asList(
                        first,
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.01, -0.01), 1_111.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.01, 0.01), 2_222.0),
                        last
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                100f,
                2_500f,
                100f,
                5f,
                true,
                13f,
                geometry,
                0.0,
                0.0,
                1,
                0f,
                0f,
                10f,
                true
        );
    }

    private static boolean hasDottedRedPath(Activity activity, ShadowCanvas shadowCanvas) {
        int routeColor = ContextCompat.getColor(activity, R.color.compass_route);
        for (int i = 0; i < shadowCanvas.getPathPaintHistoryCount(); i++) {
            Paint paint = shadowCanvas.getDrawnPathPaint(i);
            if (paint.getColor() == routeColor
                    && paint.getAlpha() == 220
                    && paint.getPathEffect() instanceof DashPathEffect) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSolidRedPath(Activity activity, ShadowCanvas shadowCanvas) {
        int routeColor = ContextCompat.getColor(activity, R.color.compass_route);
        for (int i = 0; i < shadowCanvas.getPathPaintHistoryCount(); i++) {
            Paint paint = shadowCanvas.getDrawnPathPaint(i);
            if (paint.getColor() == routeColor && !(paint.getPathEffect() instanceof DashPathEffect)) {
                return true;
            }
        }
        return false;
    }
}
