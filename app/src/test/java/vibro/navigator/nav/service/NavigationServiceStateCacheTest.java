package vibro.navigator.nav.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;

public class NavigationServiceStateCacheTest {

    @Test
    public void headingRefreshRequiresCachedStructuralStateAndUnchangedCue() {
        NavigationServiceStateCache cache = new NavigationServiceStateCache();
        CompassOrientationCue cue = new CompassOrientationCue(90f);

        assertFalse(cache.canRefreshHeadingOnly(cue));

        cache.storeStructuralState(state(), cue);

        assertTrue(cache.canRefreshHeadingOnly(new CompassOrientationCue(90f)));
        assertFalse(cache.canRefreshHeadingOnly(null));
        assertFalse(cache.canRefreshHeadingOnly(new CompassOrientationCue(180f)));

        cache.clear();

        assertFalse(cache.canRefreshHeadingOnly(cue));
    }

    private static NavState state() {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus("Turn left", "Continue"),
                        new NavProgressStatus("Destination", "Stop", ""),
                        null
                ),
                new NavGpsStatus("5 km/h", 1_000L),
                new NavPauseStatus(false)
        );
    }
}
