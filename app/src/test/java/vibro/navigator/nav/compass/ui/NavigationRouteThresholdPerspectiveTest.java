package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassRoutePoint;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationRouteThresholdPerspectiveTest {
    @Test
    public void thresholdUsesFilledGroundGeometryInCompactAndFullscreenPortrait() {
        assertViewCorridors(300, 300, false);
        assertViewCorridors(300, 500, true);
    }

    @Test
    @Config(qualifiers = "land")
    public void thresholdUsesFilledGroundGeometryInFullscreenLandscape() {
        assertViewCorridors(500, 300, true);
    }

    @Test
    public void corridorRetainsItsGroundWidthThroughCentralPerspective() {
        Bitmap bitmap = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        NavigationCompassPerspective perspective = new NavigationCompassPerspective();
        perspective.configure(300f, 400f, 600f, 1.25f, 0f, true);
        perspective.concat(canvas);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setAlpha(51);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(40f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        NavigationRoutePathRenderer renderer = new NavigationRoutePathRenderer();
        renderer.drawProjectedRouteSegment(canvas, 300f, 400f, 1f, 0, 2,
                600f, 20f, new Object(), 0, 0f, paint, true,
                (index, out) -> {
                    out.set(0f, index == 0 ? -550f : 550f);
                    return true;
                });
        float[] screen = new float[2];
        for (float groundY : new float[] {-450f, 0f, 450f}) {
            perspective.mapPoint(316f, 400f + groundY, screen);
            assertEquals("Corridor interior at ground y=" + groundY, 51,
                    Color.alpha(bitmap.getPixel(Math.round(screen[0]), Math.round(screen[1]))));
            perspective.mapPoint(324f, 400f + groundY, screen);
            assertEquals("Corridor exterior at ground y=" + groundY, 0,
                    Color.alpha(bitmap.getPixel(Math.round(screen[0]), Math.round(screen[1]))));
        }
    }

    @Test
    public void corridorOutlineIsReusedUntilStrokeGeometryChanges() {
        Path path = new Path();
        path.moveTo(50f, 0f);
        path.lineTo(50f, 100f);
        NavigationRoutePathCache.Entry entry = new NavigationRoutePathCache()
                .remember(new Object(), 0, 0, 2, path, true);
        CorridorCanvas canvas = new CorridorCanvas(100, 100);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAlpha(51);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(40f);
        entry.drawCorridor(canvas, paint);
        Path first = canvas.corridor;
        paint.setAlpha(255);
        entry.draw(canvas, paint);
        paint.setAlpha(60);
        entry.drawCorridor(canvas, paint);
        assertSame(first, canvas.corridor);
        paint.setStrokeWidth(20f);
        entry.drawCorridor(canvas, paint);
        assertNotSame(first, canvas.corridor);
    }

    @Test
    public void oversizedFullscreenCorridorUsesSmallShapesWithoutOpacitySeams() {
        Path path = new Path();
        path.moveTo(300f, 0f);
        path.lineTo(300f, 10_000f);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAlpha(51);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(40f);
        Bitmap bitmap = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888);
        CorridorCanvas canvas = new CorridorCanvas(bitmap);
        new NavigationRouteCorridor().draw(canvas, path, paint);
        assertTrue("Large corridor must be split", canvas.corridorCount > 1);
        for (int y : new int[] {254, 255, 256, 257, 510, 511, 512, 513}) {
            assertEquals("Continuous corridor alpha at y=" + y, 51, Color.alpha(bitmap.getPixel(300, y)));
            assertEquals(0, Color.alpha(bitmap.getPixel(330, y)));
        }
    }

    private static void assertViewCorridors(int width, int height, boolean fullscreen) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AppCompassSettings.setDistanceCirclesEnabled(activity, false);
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.layout(0, 0, width, height);
        for (boolean central : new boolean[] {false, true}) {
            AppCompassSettings.setCentralPerspectiveEnabled(activity, central);
            float radius = 100f * CompassPerspectiveScale.maximumViewportMultiplier(central);
            for (float tilt : new float[] {0f, 0.25f, 1f, 1.25f}) {
                view.setPerspectiveProgress(tilt);
                assertSourcesDrawCorridor(view, width, height, radius);
            }
        }
    }

    private static void assertSourcesDrawCorridor(NavigationCompassView view, int width, int height, float radius) {
        for (boolean moving : new boolean[] {false, true}) {
            for (boolean geographic : new boolean[] {false, true}) {
                view.setCompassState(state(radius, moving, geographic));
                CorridorCanvas canvas = new CorridorCanvas(width, height);
                view.draw(canvas);
                assertTrue("Threshold must be drawn for moving=" + moving + ", geographic=" + geographic,
                        canvas.corridor != null);
            }
        }
    }

    private static NavCompassState state(float radius, boolean moving, boolean geographic) {
        if (geographic) {
            CompassRouteGeometry geometry = new CompassRouteGeometry(Arrays.asList(
                    new CompassRouteGeometry.SamplePoint(new LatLon(-0.002, 0.00015), 0d),
                    new CompassRouteGeometry.SamplePoint(new LatLon(0.01, 0.00015), 1_300d)
            ), Collections.emptyList());
            return NavCompassState.fromRouteGeometry(0f, null, 1f, 1f, 1f,
                    radius, radius, radius, 5f, moving, 15f, geometry,
                    0d, 0d, 0, 0f, 0f, 0f, false);
        }
        return NavCompassState.fromProjectedPoints(0f, null, 1f, radius, 5f, moving, 15f,
                Collections.emptyList(), Arrays.asList(
                        new CompassRoutePoint(15f, -200f), new CompassRoutePoint(15f, 1_000f)),
                Collections.emptyList(), 0f, 0f, false);
    }

    private static final class CorridorCanvas extends Canvas {
        private Path corridor;
        private int corridorCount;

        private CorridorCanvas(int width, int height) {
            this(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
        }

        private CorridorCanvas(Bitmap bitmap) {
            super(bitmap);
        }

        @Override
        public void drawPath(Path path, Paint paint) {
            if (Color.red(paint.getColor()) > Color.green(paint.getColor()) && paint.getAlpha() < 100) {
                assertEquals("Threshold must be filled before perspective projection", Paint.Style.FILL, paint.getStyle());
                RectF bounds = new RectF();
                path.computeBounds(bounds, false);
                assertTrue("Corridor shape must fit hardware texture limits",
                        bounds.width() <= 258f && bounds.height() <= 258f);
                corridor = path;
                corridorCount++;
            }
            super.drawPath(path, paint);
        }
    }
}
