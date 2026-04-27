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
    public void tapWhileStationaryTogglesBetweenFullRouteAndSixtySecondView() {
        NavigationCompassModeController controller = new NavigationCompassModeController();
        NavCompassState automaticState = stationaryState();

        NavCompassState initialState = controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState sixtySecondState = controller.resolve(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 2_000L);
        NavCompassState restoredState = controller.resolve(automaticState, 2_000L);

        assertFalse(initialState.movingScaleActive);
        assertEquals(2_000f, initialState.visibleRadiusMeters, 0.01f);
        assertTrue(sixtySecondState.movingScaleActive);
        assertEquals(300f, sixtySecondState.visibleRadiusMeters, 0.01f);
        assertFalse(restoredState.movingScaleActive);
        assertEquals(2_000f, restoredState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void tapWhileMovingShowsFullRouteTemporarilyThenRestoresSixtySecondView() {
        NavigationCompassModeController controller = new NavigationCompassModeController();
        NavCompassState automaticState = movingState();

        controller.onCompassTapped(automaticState, 1_000L);
        NavCompassState temporaryFullRouteState = controller.resolve(automaticState, 1_000L);
        NavCompassState beforeExpiryState = controller.resolve(automaticState, 5_999L);
        NavCompassState restoredState = controller.resolve(automaticState, 6_000L);

        assertFalse(temporaryFullRouteState.movingScaleActive);
        assertEquals(2_000f, temporaryFullRouteState.visibleRadiusMeters, 0.01f);
        assertFalse(beforeExpiryState.movingScaleActive);
        assertTrue(restoredState.movingScaleActive);
        assertEquals(300f, restoredState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void secondTapWhileMovingClearsTemporaryFullRouteOverrideImmediately() {
        NavigationCompassModeController controller = new NavigationCompassModeController();
        NavCompassState automaticState = movingState();

        controller.onCompassTapped(automaticState, 1_000L);
        controller.onCompassTapped(automaticState, 2_000L);
        NavCompassState resolvedState = controller.resolve(automaticState, 2_000L);

        assertTrue(resolvedState.movingScaleActive);
        assertEquals(300f, resolvedState.visibleRadiusMeters, 0.01f);
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
