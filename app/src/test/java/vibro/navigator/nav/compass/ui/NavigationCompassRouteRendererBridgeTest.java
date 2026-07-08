package vibro.navigator.nav.compass.ui;

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
import vibro.navigator.nav.compass.NavCompassState;

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
}
