package vibro.navigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;

public class NavigationCompassModeControllerTest {

    @Test
    public void tapWhileStationarySmoothlyTogglesBetweenFullRouteAndSixtySecondView() {
        NavigationCompassModeController controller = new NavigationCompassModeController();
        NavCompassState automaticState = stationaryState();

        NavCompassState initialState = controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState immediateSixtySecondState = controller.resolve(automaticState, 1_000L);
        NavCompassState midSixtySecondState = controller.resolve(automaticState, 2_000L);
        controller.resolve(automaticState, 7_000L);
        NavCompassState settledSixtySecondState = controller.resolve(automaticState, 12_000L);
        controller.onCompassTapped(automaticState, 12_000L);
        NavCompassState immediateRestoredState = controller.resolve(automaticState, 12_000L);
        NavCompassState midRestoredState = controller.resolve(automaticState, 13_000L);
        controller.resolve(automaticState, 18_000L);
        NavCompassState settledRestoredState = controller.resolve(automaticState, 23_000L);

        assertFalse(initialState.movingScaleActive);
        assertEquals(2_000f, initialState.visibleRadiusMeters, 0.01f);
        assertTrue(immediateSixtySecondState.movingScaleActive);
        assertEquals(2_000f, immediateSixtySecondState.visibleRadiusMeters, 0.01f);
        assertTrue(midSixtySecondState.movingScaleActive);
        assertEquals(
                NavState.smoothVisibleRadiusMeters(300f, 2_000f, 1_000L),
                midSixtySecondState.visibleRadiusMeters,
                0.01f
        );
        assertTrue(settledSixtySecondState.movingScaleActive);
        assertEquals(300f, settledSixtySecondState.visibleRadiusMeters, 0.01f);
        assertFalse(immediateRestoredState.movingScaleActive);
        assertEquals(300f, immediateRestoredState.visibleRadiusMeters, 0.01f);
        assertFalse(midRestoredState.movingScaleActive);
        assertEquals(
                NavState.smoothVisibleRadiusMeters(2_000f, 300f, 1_000L),
                midRestoredState.visibleRadiusMeters,
                0.01f
        );
        assertFalse(settledRestoredState.movingScaleActive);
        assertEquals(2_000f, settledRestoredState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void tapWhileMovingShowsFullRouteTemporarilyThenSmoothlyRestoresSixtySecondView() {
        NavigationCompassModeController controller = new NavigationCompassModeController();
        NavCompassState automaticState = movingState();

        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState immediateFullRouteState = controller.resolve(automaticState, 1_000L);
        NavCompassState beforeExpiryState = controller.resolve(automaticState, 5_999L);
        NavCompassState restoreStartState = controller.resolve(automaticState, 6_000L);
        NavCompassState restoringState = controller.resolve(automaticState, 7_000L);
        controller.resolve(automaticState, 12_000L);
        NavCompassState restoredState = controller.resolve(automaticState, 17_000L);

        assertFalse(immediateFullRouteState.movingScaleActive);
        assertEquals(300f, immediateFullRouteState.visibleRadiusMeters, 0.01f);
        assertFalse(beforeExpiryState.movingScaleActive);
        assertTrue(beforeExpiryState.visibleRadiusMeters > 1_900f);
        assertTrue(restoreStartState.movingScaleActive);
        assertTrue(restoreStartState.visibleRadiusMeters > 1_900f);
        assertTrue(restoringState.movingScaleActive);
        assertTrue(restoringState.visibleRadiusMeters > 300f);
        assertTrue(restoringState.visibleRadiusMeters < restoreStartState.visibleRadiusMeters);
        assertTrue(restoredState.movingScaleActive);
        assertEquals(300f, restoredState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void secondTapWhileMovingClearsTemporaryFullRouteOverrideWithSmoothRadiusRestore() {
        NavigationCompassModeController controller = new NavigationCompassModeController();
        NavCompassState automaticState = movingState();

        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState temporaryFullRouteState = controller.resolve(automaticState, 1_500L);
        controller.onCompassTapped(automaticState, 2_000L);
        NavCompassState restoreStartState = controller.resolve(automaticState, 2_000L);
        controller.resolve(automaticState, 7_000L);
        NavCompassState restoredState = controller.resolve(automaticState, 12_000L);

        assertFalse(temporaryFullRouteState.movingScaleActive);
        assertTrue(temporaryFullRouteState.visibleRadiusMeters > 300f);
        assertTrue(restoreStartState.movingScaleActive);
        assertEquals(temporaryFullRouteState.visibleRadiusMeters, restoreStartState.visibleRadiusMeters, 0.01f);
        assertTrue(restoredState.movingScaleActive);
        assertEquals(300f, restoredState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void manualStationarySixtySecondOverrideClearsOnceAutomaticMovingViewMatchesIt() {
        NavigationCompassModeController controller = new NavigationCompassModeController();

        controller.onCompassTapped(stationaryState(), 1_000L);
        NavCompassState resolvedState = controller.resolve(movingState(), 2_000L);

        assertTrue(resolvedState.movingScaleActive);
        assertEquals(300f, resolvedState.visibleRadiusMeters, 0.01f);
    }

    private static NavCompassState stationaryState() {
        return compassState(false, 2_000f, 300f, 1f, 5f);
    }

    private static NavCompassState movingState() {
        return compassState(true, 2_000f, 300f, 5f, 5f);
    }

    private static NavCompassState compassState(
            boolean sixtySecondView,
            float fullRouteRadiusMeters,
            float sixtySecondRadiusMeters,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps
    ) {
        CompassRouteGeometry routeGeometry = new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.01), 1_000.0)
                ),
                Collections.emptyList()
        );
        float visibleRadiusMeters = sixtySecondView ? sixtySecondRadiusMeters : fullRouteRadiusMeters;
        float referenceSpeedMps = sixtySecondView ? sixtySecondReferenceSpeedMps : fullRouteReferenceSpeedMps;
        return new NavCompassState(
                90f,
                8f,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                visibleRadiusMeters,
                fullRouteRadiusMeters,
                sixtySecondRadiusMeters,
                5f,
                sixtySecondView,
                13f,
                routeGeometry,
                0.0,
                0.0,
                1,
                0f,
                1_500f,
                !sixtySecondView
        );
    }
}
