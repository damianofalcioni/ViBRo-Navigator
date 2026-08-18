package vibro.navigator.nav.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;

public class NavStateComposerHeadingTest {

    @Test
    public void headingRefreshReusesNonHeadingNavigationState() {
        NavCompassState compassState = NavCompassState.fromProjectedPoints(
                10f,
                5f,
                2f,
                100f,
                4f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                20f,
                30f,
                true
        );
        NavState state = new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus("Turn left", "Continue"),
                        new NavProgressStatus("Destination", "Stop", ""),
                        compassState
                ),
                new NavGpsStatus("5 km/h", 1_000L),
                new NavPauseStatus(false)
        );

        NavState refreshed = NavStateComposer.withCompassHeading(state, 95.0, 8f);

        assertNotSame(state, refreshed);
        assertSame(state.routeStatus.guidance, refreshed.routeStatus.guidance);
        assertSame(state.routeStatus.progress, refreshed.routeStatus.progress);
        assertSame(state.gpsStatus, refreshed.gpsStatus);
        assertSame(state.pauseStatus, refreshed.pauseStatus);
        assertSame(state.tripStatus, refreshed.tripStatus);
        assertSame(compassState.routePoints, refreshed.routeStatus.compassState.routePoints);
        assertEquals(95f, refreshed.routeStatus.compassState.displayMode.headingDegrees, 0f);
        assertEquals(8f, refreshed.routeStatus.compassState.displayMode.headingAccuracyDegrees, 0f);
    }
}
