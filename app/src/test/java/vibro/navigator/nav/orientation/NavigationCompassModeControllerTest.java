package vibro.navigator.nav.orientation;


import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

public class NavigationCompassModeControllerTest {

    @Test
    public void stationaryTapsCycleThroughFullRoute2d3d2dAndFullRoute() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = stationaryState();

        NavCompassState initialState = controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState immediateMovingScaleState = controller.resolve(automaticState, 1_000L);
        NavCompassState midMovingScaleState = controller.resolve(automaticState, 2_000L);
        controller.resolve(automaticState, 7_000L);
        NavCompassState settledMovingScaleState = controller.resolve(automaticState, 12_000L);
        controller.onCompassTapped(automaticState, 12_000L);
        NavCompassState perspectiveState = controller.resolve(automaticState, 12_000L);
        boolean perspectiveEnabled = controller.isPerspectiveViewEnabled();
        boolean perspectiveTransitioning = controller.isTransitionInProgress();
        NavCompassState settledPerspectiveState = controller.resolve(automaticState, 12_320L);
        controller.onCompassTapped(automaticState, 13_000L);
        NavCompassState immediateSecond2dState = controller.resolve(automaticState, 13_000L);
        NavCompassState settledSecond2dState = controller.resolve(automaticState, 13_320L);
        controller.onCompassTapped(automaticState, 14_000L);
        NavCompassState immediateRestoredState = controller.resolve(automaticState, 14_000L);
        NavCompassState midRestoredState = controller.resolve(automaticState, 15_000L);
        controller.resolve(automaticState, 19_000L);
        NavCompassState settledRestoredState = controller.resolve(automaticState, 24_000L);

        assertFalse(initialState.displayMode.movingScaleActive);
        assertEquals(2_000f, initialState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(immediateMovingScaleState.displayMode.movingScaleActive);
        assertEquals(2_000f, immediateMovingScaleState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(midMovingScaleState.displayMode.movingScaleActive);
        assertEquals(
                NavCompassStateFactory.smoothVisibleRadiusMeters(300f, 2_000f, 1_000L),
                midMovingScaleState.radiusState.visibleRadiusMeters,
                0.01f
        );
        assertTrue(settledMovingScaleState.displayMode.movingScaleActive);
        assertEquals(300f, settledMovingScaleState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(perspectiveEnabled);
        assertTrue(perspectiveTransitioning);
        assertTrue(perspectiveState.displayMode.movingScaleActive);
        assertEquals(300f, perspectiveState.radiusState.visibleRadiusMeters, 0.01f);
        assertEquals(300f * CompassPerspectiveScale.maximumViewportMultiplier(),
                settledPerspectiveState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(controller.isPerspectiveViewEnabled());
        assertTrue(immediateSecond2dState.displayMode.movingScaleActive);
        assertEquals(settledPerspectiveState.radiusState.visibleRadiusMeters,
                immediateSecond2dState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(settledSecond2dState.displayMode.movingScaleActive);
        assertEquals(300f, settledSecond2dState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(immediateRestoredState.displayMode.movingScaleActive);
        assertEquals(300f, immediateRestoredState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(midRestoredState.displayMode.movingScaleActive);
        assertEquals(
                NavCompassStateFactory.smoothVisibleRadiusMeters(2_000f, 300f, 1_000L),
                midRestoredState.radiusState.visibleRadiusMeters,
                0.01f
        );
        assertFalse(settledRestoredState.displayMode.movingScaleActive);
        assertEquals(2_000f, settledRestoredState.radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void movingTapsShow3dSecond2dThenTemporaryFullRouteAndRestore2d() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = movingState();

        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState perspectiveState = controller.resolve(automaticState, 1_000L);
        boolean perspectiveEnabled = controller.isPerspectiveViewEnabled();
        controller.resolve(automaticState, 1_320L);
        controller.onCompassTapped(automaticState, 2_000L);
        NavCompassState second2dState = controller.resolve(automaticState, 2_320L);
        controller.onCompassTapped(automaticState, 3_000L);
        NavCompassState immediateFullRouteState = controller.resolve(automaticState, 3_000L);
        NavCompassState beforeExpiryState = controller.resolve(automaticState, 7_999L);
        NavCompassState restoreStartState = controller.resolve(automaticState, 8_000L);
        NavCompassState restoringState = controller.resolve(automaticState, 9_000L);
        controller.resolve(automaticState, 14_000L);
        NavCompassState restoredState = controller.resolve(automaticState, 19_000L);

        assertTrue(perspectiveEnabled);
        assertTrue(perspectiveState.displayMode.movingScaleActive);
        assertEquals(300f, perspectiveState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(second2dState.displayMode.movingScaleActive);
        assertEquals(300f, second2dState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(immediateFullRouteState.displayMode.movingScaleActive);
        assertEquals(300f, immediateFullRouteState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(beforeExpiryState.displayMode.movingScaleActive);
        assertTrue(beforeExpiryState.radiusState.visibleRadiusMeters > 1_900f);
        assertTrue(restoreStartState.displayMode.movingScaleActive);
        assertTrue(restoreStartState.radiusState.visibleRadiusMeters > 1_900f);
        assertEquals(5f, restoreStartState.displayMode.referenceSpeedMps, 0.01f);
        assertTrue(restoringState.displayMode.movingScaleActive);
        assertTrue(restoringState.radiusState.visibleRadiusMeters > 300f);
        assertTrue(restoringState.radiusState.visibleRadiusMeters < restoreStartState.radiusState.visibleRadiusMeters);
        assertEquals(5f, restoringState.displayMode.referenceSpeedMps, 0.01f);
        assertTrue(restoredState.displayMode.movingScaleActive);
        assertEquals(300f, restoredState.radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void tapAfterSecond2dShowsTemporaryFullRouteAndNextTapRestoresMovingView() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = movingState();

        controller.onCompassTapped(automaticState, 1_000L);
        controller.resolve(automaticState, 1_320L);
        controller.onCompassTapped(automaticState, 2_000L);
        controller.resolve(automaticState, 2_320L);
        controller.onCompassTapped(automaticState, 3_000L);
        NavCompassState temporaryFullRouteState = controller.resolve(automaticState, 3_500L);
        controller.onCompassTapped(automaticState, 4_000L);
        NavCompassState restoreStartState = controller.resolve(automaticState, 4_000L);
        controller.resolve(automaticState, 9_000L);
        NavCompassState restoredState = controller.resolve(automaticState, 14_000L);

        assertFalse(temporaryFullRouteState.displayMode.movingScaleActive);
        assertTrue(temporaryFullRouteState.radiusState.visibleRadiusMeters > 300f);
        assertTrue(restoreStartState.displayMode.movingScaleActive);
        assertEquals(temporaryFullRouteState.radiusState.visibleRadiusMeters, restoreStartState.radiusState.visibleRadiusMeters, 0.01f);
        assertEquals(5f, restoreStartState.displayMode.referenceSpeedMps, 0.01f);
        assertTrue(restoredState.displayMode.movingScaleActive);
        assertEquals(300f, restoredState.radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void manualStationary2dOverrideClearsOnceAutomaticMovingViewMatchesIt() {
        NavigationCompassModeController controller = newController();

        controller.onCompassTapped(stationaryState(), 1_000L);
        NavCompassState resolvedState = controller.resolve(movingState(), 2_000L);

        assertTrue(resolvedState.displayMode.movingScaleActive);
        assertEquals(300f, resolvedState.radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void perspectiveModePersistsAcrossAutomaticScaleChangesUntilTapped() {
        NavigationCompassModeController controller = newController();

        controller.onCompassTapped(movingState(), 1_000L);
        controller.resolve(movingState(), 1_000L);
        NavCompassState stationaryPerspective = controller.resolve(stationaryState(), 2_000L);

        assertTrue(controller.isPerspectiveViewEnabled());
        assertTrue(stationaryPerspective.displayMode.movingScaleActive);
        controller.onCompassTapped(stationaryState(), 3_000L);
        NavCompassState second2d = controller.resolve(stationaryState(), 3_000L);
        assertFalse(controller.isPerspectiveViewEnabled());
        assertTrue(second2d.displayMode.movingScaleActive);
        controller.resolve(stationaryState(), 3_320L);
        controller.onCompassTapped(stationaryState(), 4_000L);
        NavCompassState fullRoute = controller.resolve(stationaryState(), 4_000L);
        assertFalse(fullRoute.displayMode.movingScaleActive);
    }

    @Test
    public void perspectiveOverrideRetainsLoadedSurroundingStreets() {
        NavigationCompassModeController controller = newController();
        CompassStreetOverlay streets = new CompassStreetOverlay(Collections.singletonList(
                new CompassStreetSegment(Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.001, 0.0)))
        ));
        NavCompassState automaticState = stationaryState().withStreetOverlay(streets);

        controller.onCompassTapped(automaticState, 1_000L);
        controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 2_000L);
        NavCompassState perspectiveState = controller.resolve(automaticState, 2_320L);

        assertTrue(perspectiveState.displayMode.movingScaleActive);
        assertSame(streets, perspectiveState.streetOverlay);
    }

    @Test
    public void perspectiveTiltAnimatesQuicklyAndReversesOnNextTap() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = movingState();

        controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState initialState = controller.resolve(automaticState, 1_000L);
        float initial = controller.perspectiveProgress();
        NavCompassState halfwayState = controller.resolve(automaticState, 1_160L);
        float halfway = controller.perspectiveProgress();
        NavCompassState settledState = controller.resolve(automaticState, 1_320L);
        float settled = controller.perspectiveProgress();
        boolean transitionFinished = !controller.isTransitionInProgress();

        controller.onCompassTapped(automaticState, 1_320L);
        NavCompassState immediateSecond2d = controller.resolve(automaticState, 1_320L);
        controller.resolve(automaticState, 1_480L);
        float reversing = controller.perspectiveProgress();
        controller.resolve(automaticState, 1_640L);

        assertEquals(0f, initial, 0.001f);
        assertEquals(300f, initialState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(halfway > 0f && halfway < 1f);
        assertEquals(300f * CompassPerspectiveScale.maximumViewportMultiplier(),
                halfwayState.radiusState.visibleRadiusMeters, 0.01f);
        assertSame(halfwayState, settledState);
        assertEquals(1f, settled, 0.001f);
        assertEquals(300f * CompassPerspectiveScale.viewportMultiplier(1f),
                settledState.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(transitionFinished);
        assertTrue(immediateSecond2d.displayMode.movingScaleActive);
        assertEquals(settledState.radiusState.visibleRadiusMeters,
                immediateSecond2d.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(reversing > 0f && reversing < 1f);
        assertEquals(0f, controller.perspectiveProgress(), 0.001f);
    }

    @Test
    public void rapidTapsKeepTheSecond2dStepAndAvoidAnExtraZoomJump() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = movingState();

        controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 1_000L);
        controller.resolve(automaticState, 1_160L);
        float partialTilt = controller.perspectiveProgress();
        controller.onCompassTapped(automaticState, 1_160L);
        NavCompassState second2d = controller.resolve(automaticState, 1_160L);
        controller.onCompassTapped(automaticState, 1_160L);
        NavCompassState fullRoute = controller.resolve(automaticState, 1_160L);

        assertTrue(partialTilt > 0f && partialTilt < 1f);
        assertTrue(second2d.displayMode.movingScaleActive);
        assertFalse(fullRoute.displayMode.movingScaleActive);
        assertEquals(300f * CompassPerspectiveScale.maximumViewportMultiplier(),
                fullRoute.radiusState.visibleRadiusMeters, 0.01f);
        assertEquals(partialTilt, controller.perspectiveProgress(), 0.001f);
    }

    @Test
    public void tapWhileStationaryWithAnimationDisabledSwitchesRadiusImmediately() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = stationaryState();

        controller.onCompassTapped(automaticState, 1_000L, false);
        NavCompassState immediateMovingScaleState = controller.resolve(automaticState, 1_000L, false);

        assertTrue(immediateMovingScaleState.displayMode.movingScaleActive);
        assertEquals(300f, immediateMovingScaleState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(controller.isTransitionInProgress());
    }

    @Test
    public void disablingAnimationDuringTransitionJumpsToTargetRadius() {
        NavigationCompassModeController controller = newController();
        NavCompassState automaticState = stationaryState();

        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState animatedState = controller.resolve(automaticState, 1_500L);
        NavCompassState instantState = controller.resolve(automaticState, 1_600L, false);

        assertTrue(animatedState.radiusState.visibleRadiusMeters > 300f);
        assertTrue(instantState.displayMode.movingScaleActive);
        assertEquals(300f, instantState.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(controller.isTransitionInProgress());
    }

    private static NavCompassState stationaryState() {
        return compassState(false, 2_000f, 300f, 1f, 5f);
    }

    private static NavCompassState movingState() {
        return compassState(true, 2_000f, 300f, 5f, 5f);
    }

    private static NavigationCompassModeController newController() {
        return new NavigationCompassModeController(() -> 0L);
    }

    private static NavCompassState compassState(
            boolean movingScaleView,
            float fullRouteRadiusMeters,
            float movingScaleRadiusMeters,
            float fullRouteReferenceSpeedMps,
            float movingScaleReferenceSpeedMps
    ) {
        CompassRouteGeometry routeGeometry = new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.01), 1_000.0)
                ),
                Collections.emptyList()
        );
        float visibleRadiusMeters = movingScaleView ? movingScaleRadiusMeters : fullRouteRadiusMeters;
        float referenceSpeedMps = movingScaleView ? movingScaleReferenceSpeedMps : fullRouteReferenceSpeedMps;
        return NavCompassState.fromRouteGeometry(
                90f,
                8f,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                movingScaleReferenceSpeedMps,
                visibleRadiusMeters,
                fullRouteRadiusMeters,
                movingScaleRadiusMeters,
                5f,
                movingScaleView,
                13f,
                routeGeometry,
                0.0,
                0.0,
                1,
                0f,
                1_500f,
                5f,
                !movingScaleView
        );
    }
}

