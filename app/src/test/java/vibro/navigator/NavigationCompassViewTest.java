package vibro.navigator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.nav.NavCompassState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassViewTest {

    @Test
    public void destinationPositionIsHiddenWhenOutsideVisibleRadius() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                120f,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                300f,
                0f,
                false
        ));

        assertNull(invokeResolveDestinationPosition(view));
    }

    @Test
    public void destinationPositionIsVisibleWhenWithinVisibleRadius() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                120f,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertNotNull(invokeResolveDestinationPosition(view));
    }

    @Test
    public void legendRingDistancesAreNormalizedToOuterVisibleRing() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                60f,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(60f, invokeResolveLegendRingDistanceMeters(view, 0.82f), 0.01f);
        assertEquals(40.24f, invokeResolveLegendRingDistanceMeters(view, 0.55f), 0.01f);
        assertEquals(20.49f, invokeResolveLegendRingDistanceMeters(view, 0.28f), 0.01f);
    }

    @Test
    public void routeThresholdStrokeWidthRepresentsThresholdWhenMoving() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                100f,
                7f,
                true,
                15f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(16f, invokeResolveRouteThresholdStrokeWidthPx(view, 100f), 0.01f);
    }

    @Test
    public void routeThresholdOverlayIsEnabledInFullRouteOverview() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                150f,
                0f,
                false,
                15f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(true, invokeShouldDrawRouteThresholdOverlay(view));
    }

    @Test
    public void routeThresholdOverlayIsDisabledWhenAccuracyAlreadyCoversThreshold() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                150f,
                15f,
                true,
                15f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(false, invokeShouldDrawRouteThresholdOverlay(view));
        assertEquals(0f, invokeResolveRouteThresholdStrokeWidthPx(view, 100f), 0.01f);
    }

    @Test
    public void routeThresholdUsesEightyPercentTransparencyInMovingMode() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                150f,
                0f,
                true,
                15f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(51, invokeResolveRouteThresholdPaintAlpha(view));
    }

    @Test
    public void routeKeepsOpaqueBaseAlphaInMovingMode() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                150f,
                0f,
                true,
                15f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(255, invokeResolveRoutePaintAlpha(view));
    }

    @Test
    public void routeSegmentNearVisibleAreaIncludesCrossingOffscreenSegments() {
        assertTrue(NavigationCompassView.isRouteSegmentNearVisibleArea(
                -1_000f,
                0f,
                1_000f,
                0f,
                90f,
                24f
        ));
    }

    @Test
    public void routeSegmentNearVisibleAreaExcludesDistantSegments() {
        assertFalse(NavigationCompassView.isRouteSegmentNearVisibleArea(
                1_000f,
                1_000f,
                2_000f,
                1_000f,
                90f,
                24f
        ));
    }

    @Test
    public void routeCoordinateClampLimitsOffscreenCanvasPathCoordinates() {
        assertEquals(114f, NavigationCompassView.clampRouteCoordinate(1_000f, 114f), 0.01f);
        assertEquals(-114f, NavigationCompassView.clampRouteCoordinate(-1_000f, 114f), 0.01f);
        assertEquals(40f, NavigationCompassView.clampRouteCoordinate(40f, 114f), 0.01f);
    }

    private static Object invokeResolveDestinationPosition(NavigationCompassView view) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod(
                "resolveDestinationPosition",
                float.class,
                float.class,
                float.class,
                float.class
        );
        method.setAccessible(true);
        return method.invoke(view, 100f, 100f, 80f, 0f);
    }

    private static float invokeResolveLegendRingDistanceMeters(NavigationCompassView view, float ringScale) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod(
                "resolveLegendRingDistanceMeters",
                float.class
        );
        method.setAccessible(true);
        return (float) method.invoke(view, ringScale);
    }

    private static float invokeResolveRouteThresholdStrokeWidthPx(NavigationCompassView view, float routeRadius) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod(
                "resolveRouteThresholdStrokeWidthPx",
                float.class
        );
        method.setAccessible(true);
        return (float) method.invoke(view, routeRadius);
    }

    private static int invokeResolveRoutePaintAlpha(NavigationCompassView view) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod("resolveRoutePaintAlpha");
        method.setAccessible(true);
        return (int) method.invoke(view);
    }

    private static int invokeResolveRouteThresholdPaintAlpha(NavigationCompassView view) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod("resolveRouteThresholdPaintAlpha");
        method.setAccessible(true);
        return (int) method.invoke(view);
    }

    private static boolean invokeShouldDrawRouteThresholdOverlay(NavigationCompassView view) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod("shouldDrawRouteThresholdOverlay");
        method.setAccessible(true);
        return (boolean) method.invoke(view);
    }
}
