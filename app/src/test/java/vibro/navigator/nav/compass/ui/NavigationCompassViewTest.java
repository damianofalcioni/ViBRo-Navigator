package vibro.navigator.nav.compass.ui;


import vibro.navigator.nav.compass.NavCompassState;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassViewTest {

    @Test
    public void destinationPositionIsHiddenWhenOutsideVisibleRadius() {
        NavigationCompassRouteMarkerRenderer renderer = new NavigationCompassRouteMarkerRenderer();

        assertNull(renderer.resolveDestinationPosition(NavCompassState.fromProjectedPoints(
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
        ), 100f, 100f, 80f, 0f));
    }

    @Test
    public void destinationPositionIsVisibleWhenWithinVisibleRadius() {
        NavigationCompassRouteMarkerRenderer renderer = new NavigationCompassRouteMarkerRenderer();

        assertNotNull(renderer.resolveDestinationPosition(NavCompassState.fromProjectedPoints(
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
        ), 100f, 100f, 80f, 0f));
    }

    @Test
    public void routeThresholdStrokeWidthRepresentsThresholdWhenMoving() {
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();

        assertEquals(16f, renderer.resolveRouteThresholdStrokeWidthPx(NavCompassState.fromProjectedPoints(
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
        ), 100f, 3f), 0.01f);
    }

    @Test
    public void routeThresholdOverlayIsEnabledInFullRouteOverview() {
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();

        assertEquals(true, renderer.shouldDrawRouteThresholdOverlay(NavCompassState.fromProjectedPoints(
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
        )));
    }

    @Test
    public void routeThresholdOverlayIsDisabledWhenAccuracyAlreadyCoversThreshold() {
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();
        NavCompassState state = NavCompassState.fromProjectedPoints(
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
        );

        assertEquals(false, renderer.shouldDrawRouteThresholdOverlay(state));
        assertEquals(0f, renderer.resolveRouteThresholdStrokeWidthPx(state, 100f, 3f), 0.01f);
    }

    @Test
    public void routeThresholdUsesEightyPercentTransparencyInMovingMode() {
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();

        assertEquals(51, renderer.resolveRouteThresholdPaintAlpha(NavCompassState.fromProjectedPoints(
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
        )));
    }

    @Test
    public void routeKeepsOpaqueBaseAlphaInMovingMode() {
        NavigationCompassRouteRenderer renderer = new NavigationCompassRouteRenderer();

        assertEquals(255, renderer.resolveRoutePaintAlpha());
    }

    @Test
    public void routeSegmentNearVisibleAreaIncludesCrossingOffscreenSegments() {
        assertTrue(RouteDrawingMath.isRouteSegmentNearVisibleArea(
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
        assertFalse(RouteDrawingMath.isRouteSegmentNearVisibleArea(
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
        assertEquals(114f, RouteDrawingMath.clampRouteCoordinate(1_000f, 114f), 0.01f);
        assertEquals(-114f, RouteDrawingMath.clampRouteCoordinate(-1_000f, 114f), 0.01f);
        assertEquals(40f, RouteDrawingMath.clampRouteCoordinate(40f, 114f), 0.01f);
    }

}
