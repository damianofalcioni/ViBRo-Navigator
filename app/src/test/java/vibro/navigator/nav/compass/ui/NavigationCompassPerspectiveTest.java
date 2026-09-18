package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.Collections;
import java.util.Arrays;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.CompassRoutePoint;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationCompassPerspectiveTest {
    @Test
    public void tiltKeepsCurrentPositionFixedAndCompressesForwardDistance() {
        NavigationCompassPerspective perspective = new NavigationCompassPerspective();
        float[] point = new float[2];

        assertTrue(perspective.configure(150f, 150f, 100f, 0f));
        perspective.mapPoint(250f, 50f, point);
        assertEquals(250f, point[0], 0.01f);
        assertEquals(50f, point[1], 0.01f);

        assertTrue(perspective.configure(150f, 150f, 100f, 1f));
        perspective.mapPoint(150f, 150f, point);
        assertEquals(150f, point[0], 0.01f);
        assertEquals(150f, point[1], 0.01f);
        perspective.mapPoint(250f, 50f, point);
        assertTrue(point[0] > 150f && point[0] < 250f);
        assertTrue(point[1] > 90f && point[1] < 150f);
        perspective.mapPoint(150f, 250f, point);
        assertTrue(point[1] < 250f);
    }

    @Test
    public void expandedPerspectiveViewportReachesTheForwardEdge() {
        NavigationCompassPerspective perspective = new NavigationCompassPerspective();
        float[] point = new float[2];
        float screenRadius = 100f;
        float sourceRadius = screenRadius * CompassPerspectiveScale.maximumViewportMultiplier();

        for (float progress : new float[] {0f, 0.5f, 1f}) {
            assertTrue(perspective.configure(150f, 150f, sourceRadius, progress));
            perspective.mapPoint(150f,
                    150f - screenRadius * CompassPerspectiveScale.viewportMultiplier(progress), point);
            assertEquals(50f, point[1], 0.01f);
        }
    }

    @Test
    public void perspectiveRendersInCompactAndFullscreenPortraitViews() {
        assertPerspectiveDraws(300, 300, false);
        assertPerspectiveDraws(300, 500, true);
    }

    @Test
    @Config(qualifiers = "land")
    public void perspectiveRendersInLandscapeFullscreenView() {
        assertPerspectiveDraws(500, 300, true);
    }

    @Test
    public void degenerateProjectionIsRejected() {
        NavigationCompassPerspective perspective = new NavigationCompassPerspective();

        assertFalse(perspective.configure(0f, 0f, 0f, 1f));
        assertFalse(perspective.configure(0f, 0f, Float.NaN, 1f));
    }

    @Test
    public void perspectiveDrawsSurroundingStreetsInCompactAndFullscreenViews() {
        NavCompassState base = stateWithStreetOverlay().withDisplayMode(
                true, 100f * CompassPerspectiveScale.maximumViewportMultiplier());
        assertStreetPixelsVisible(base, 300, 300, false);
        assertStreetPixelsVisible(base, 300, 500, true);
    }

    @Test
    public void perspectiveRouteAndStreetsReachTopEdgeInCompactAndFullscreenViews() {
        float visibleRadius = 100f * CompassPerspectiveScale.viewportMultiplier(1f);
        NavCompassState streetState = stateWithStreetOverlay().withDisplayMode(true, visibleRadius);
        NavCompassState routeState = stateWithForwardRoute(visibleRadius, true);
        NavCompassState emptyRouteState = stateWithForwardRoute(visibleRadius, false);

        assertTopEdgePixelsDiffer(streetState.withStreetOverlay(CompassStreetOverlay.EMPTY), streetState,
                300, 300, false);
        assertTopEdgePixelsDiffer(emptyRouteState, routeState, 300, 300, false);
        assertTopEdgePixelsDiffer(streetState.withStreetOverlay(CompassStreetOverlay.EMPTY), streetState,
                300, 500, true);
        assertTopEdgePixelsDiffer(emptyRouteState, routeState, 300, 500, true);
    }

    @Test
    @Config(qualifiers = "land")
    public void perspectiveRouteAndStreetsReachTopEdgeInLandscapeFullscreenView() {
        float visibleRadius = 100f * CompassPerspectiveScale.viewportMultiplier(1f);
        NavCompassState streetState = stateWithStreetOverlay().withDisplayMode(true, visibleRadius);
        assertTopEdgePixelsDiffer(streetState.withStreetOverlay(CompassStreetOverlay.EMPTY), streetState,
                500, 300, true);
        assertTopEdgePixelsDiffer(stateWithForwardRoute(visibleRadius, false),
                stateWithForwardRoute(visibleRadius, true), 500, 300, true);
    }

    @Test
    public void projectedHeadingAccuracyLinesStayInsideCompactCompass() {
        float visibleRadius = 100f * CompassPerspectiveScale.viewportMultiplier(1f);
        NavCompassState withoutAccuracy = stateWithHeadingAccuracy(visibleRadius, null);
        NavCompassState withAccuracy = stateWithHeadingAccuracy(visibleRadius, 20f);
        Bitmap without = drawState(withoutAccuracy, 300, 300, false);
        Bitmap with = drawState(withAccuracy, 300, 300, false);

        assertTrue(countDifferentPixels(without, with) > 20);
        assertEquals(0, countDifferentPixelsOutsideCompass(without, with));
    }

    private static int countDifferentPixelsOutsideCompass(Bitmap without, Bitmap with) {
        int differentPixels = 0;
        for (int y = 0; y < 300; y++) {
            for (int x = 0; x < 300; x++) {
                if (Math.hypot(x - 150, y - 150) > 130f
                        && without.getPixel(x, y) != with.getPixel(x, y)) {
                    differentPixels++;
                }
            }
        }
        return differentPixels;
    }

    private static int countDifferentPixels(Bitmap without, Bitmap with) {
        int differentPixels = 0;
        for (int y = 0; y < without.getHeight(); y++) {
            for (int x = 0; x < without.getWidth(); x++) {
                if (without.getPixel(x, y) != with.getPixel(x, y)) {
                    differentPixels++;
                }
            }
        }
        return differentPixels;
    }

    private static void assertPerspectiveDraws(int width, int height, boolean fullscreen) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.setPerspectiveProgress(1f);
        view.setCompassState(NavCompassState.fromProjectedPoints(
                0f,
                null,
                1f,
                80f * CompassPerspectiveScale.maximumViewportMultiplier(),
                0f,
                true,
                0f,
                Collections.emptyList(),
                Arrays.asList(new CompassRoutePoint(0f, 0f), new CompassRoutePoint(0f, 20f),
                        new CompassRoutePoint(15f, 45f), new CompassRoutePoint(0f, 80f)),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));
        view.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        view.draw(new Canvas(bitmap));

        boolean hasVisiblePixels = false;
        for (int y = 0; y < height && !hasVisiblePixels; y++) {
            for (int x = 0; x < width; x++) {
                if (bitmap.getPixel(x, y) != 0) {
                    hasVisiblePixels = true;
                    break;
                }
            }
        }
        assertTrue(hasVisiblePixels);
    }

    private static void assertStreetPixelsVisible(NavCompassState withStreets, int width, int height, boolean fullscreen) {
        Bitmap without = drawState(withStreets.withStreetOverlay(CompassStreetOverlay.EMPTY), width, height, fullscreen);
        Bitmap with = drawState(withStreets, width, height, fullscreen);
        int differentPixels = countDifferentPixels(without, with);
        assertTrue("Street pixels=" + differentPixels, differentPixels > 20);
    }

    private static void assertTopEdgePixelsDiffer(
            NavCompassState without,
            NavCompassState with,
            int width,
            int height,
            boolean fullscreen
    ) {
        Bitmap emptyBitmap = drawState(without, width, height, fullscreen);
        Bitmap filledBitmap = drawState(with, width, height, fullscreen);
        int differentPixels = 0;
        for (int y = 18; y < 48; y++) {
            for (int x = 0; x < width; x++) {
                if (emptyBitmap.getPixel(x, y) != filledBitmap.getPixel(x, y)) {
                    differentPixels++;
                }
            }
        }
        assertTrue("Top-edge geometry pixels=" + differentPixels, differentPixels > 3);
    }

    private static NavCompassState stateWithForwardRoute(float visibleRadius, boolean includeRoute) {
        return NavCompassState.fromProjectedPoints(
                0f, null, 1f, visibleRadius, 0f, true, 0f,
                Collections.emptyList(),
                includeRoute ? Arrays.asList(
                        new CompassRoutePoint(15f, 0f),
                        new CompassRoutePoint(15f, 300f)
                ) : Collections.emptyList(),
                Collections.emptyList(), 0f, 0f, false
        );
    }

    private static NavCompassState stateWithHeadingAccuracy(float visibleRadius, Float accuracyDegrees) {
        return NavCompassState.fromProjectedPoints(
                0f, accuracyDegrees, 1f, visibleRadius, 0f, true, 0f,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                0f, 0f, false
        );
    }

    private static Bitmap drawState(NavCompassState state, int width, int height, boolean fullscreen) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.setPerspectiveProgress(1f);
        view.setCompassState(state);
        view.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static NavCompassState stateWithStreetOverlay() {
        CompassRouteGeometry geometry = new CompassRouteGeometry(Arrays.asList(
                new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.001), 100.0)
        ), Collections.emptyList());
        NavCompassState state = NavCompassState.fromRouteGeometry(
                0f, null, 1f, 1f, 1f,
                100f, 1_000f, 100f, 5f, true, 0f,
                geometry, 0.0, 0.0, 0,
                100f, 0f, 5f, true
        );
        CompassStreetSegment street = new CompassStreetSegment(Arrays.asList(
                new LatLon(-0.001, -0.00015),
                new LatLon(0.003, -0.00015)
        ));
        return state.withStreetOverlay(new CompassStreetOverlay(Collections.singletonList(street)));
    }
}
