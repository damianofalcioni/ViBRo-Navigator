package vibro.navigator.nav.compass.ui;


import vibro.navigator.nav.compass.NavCompassState;
import android.app.Activity;
import android.graphics.Paint;
import android.os.Looper;
import android.view.View;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void pausedLayerVisibilityFollowsNavigationPausedState() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView compassView = new NavigationCompassView(activity);

        assertFalse(compassView.isNavigationPausedForTest());

        compassView.setNavigationPaused(true);

        assertTrue(compassView.isNavigationPausedForTest());

        compassView.setNavigationPaused(false);

        assertFalse(compassView.isNavigationPausedForTest());
    }

    @Test
    public void pausedAndCalibrationLayersUseOuterCompassGeometry() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView compassView = new NavigationCompassView(activity);
        Field pausedRingPaintField = NavigationCompassView.class.getDeclaredField("pausedRingPaint");
        pausedRingPaintField.setAccessible(true);
        Paint pausedRingPaint = (Paint) pausedRingPaintField.get(compassView);

        assertEquals(91f, compassView.outerCompassLayerRadius(100f), 0.01f);
        assertEquals(0.18f, NavigationCompassView.OUTER_COMPASS_LAYER_STROKE_SCALE, 0.01f);
        assertEquals(NavigationCompassCalibrationRing.BACKGROUND_ALPHA, pausedRingPaint.getAlpha());
    }

    @Test
    public void orientationCueSweepUsesShortestSignedTurn() {
        NavigationCompassOrientationCueRenderer renderer = new NavigationCompassOrientationCueRenderer();

        assertEquals(20f, renderer.signedSweepDegrees(350f, 10f), 0.01f);
        assertEquals(-20f, renderer.signedSweepDegrees(10f, 350f), 0.01f);
        assertEquals(180f, renderer.signedSweepDegrees(0f, 180f), 0.01f);
    }

    @Test
    public void orientationCueArcExtendsToTargetMarker() {
        NavigationCompassOrientationCueRenderer renderer = new NavigationCompassOrientationCueRenderer();

        assertEquals(20f, renderer.arcSweepToMarker(20f), 0.01f);
        assertEquals(-20f, renderer.arcSweepToMarker(-20f), 0.01f);
        assertEquals(4f, renderer.arcSweepToMarker(4f), 0.01f);
    }

    @Test
    public void headingCalibrationNeededOnlyWhenAccuracyIsExplicitlyPoor() {
        assertFalse(NavigationCompassCalibrationRing.needsHeadingCalibration(null));
        assertFalse(NavigationCompassCalibrationRing.needsHeadingCalibration(compassStateWithHeadingAccuracy(null)));
        assertTrue(NavigationCompassCalibrationRing.needsHeadingCalibration(compassStateWithHeadingAccuracy(35f)));
        assertFalse(NavigationCompassCalibrationRing.needsHeadingCalibration(compassStateWithHeadingAccuracy(20f)));
    }

    @Test
    public void calibrationRingStaysRedUntilHeadingAccuracyRecoversThenHidesGreen() {
        NavigationCompassCalibrationRing ring = calibrationRing();

        ring.update(compassStateWithHeadingAccuracy(35f));

        assertTrue(ring.isVisibleForTest());
        assertTrue(ring.isCalibrationNeededForTest());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS);
        assertTrue(ring.isVisibleForTest());
        assertTrue(ring.isCalibrationNeededForTest());

        ring.update(compassStateWithHeadingAccuracy(10f));

        assertTrue(ring.isVisibleForTest());
        assertFalse(ring.isCalibrationNeededForTest());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS);
        assertFalse(ring.isVisibleForTest());
    }

    private static NavigationCompassCalibrationRing calibrationRing() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        View owner = new View(activity);
        activity.setContentView(owner);
        NavigationCompassCalibrationRing ring = new NavigationCompassCalibrationRing(owner);
        ring.init();
        return ring;
    }

    private static NavCompassState compassStateWithHeadingAccuracy(Float headingAccuracyDegrees) {
        return NavCompassState.fromProjectedPoints(
                0f,
                headingAccuracyDegrees,
                1f,
                120f,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        );
    }
}
