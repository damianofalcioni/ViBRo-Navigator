package vibro.navigator.nav.compass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class NavCompassStateRefreshTest {

    @Test
    public void radiusChangeRemainsStructuralUntilVisibleRadiusReachesTarget() {
        NavCompassState changing = state(100f, 300f);
        NavCompassState settled = state(300f, 300f);

        assertTrue(NavCompassHeadingRefresh.hasPendingRadiusChange(changing));
        assertFalse(NavCompassHeadingRefresh.hasPendingRadiusChange(settled));
    }

    private static NavCompassState state(float visibleRadiusMeters, float targetRadiusMeters) {
        return NavCompassState.fromProjectedPoints(new NavCompassProjectedPointsInput(
                new CompassDisplayMetrics(0f, null, 1f, 1f, 1f, false),
                new CompassRadiusMetrics(
                        visibleRadiusMeters,
                        targetRadiusMeters,
                        100f,
                        5f,
                        10f
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                new CompassDestinationProjection(0f, 0f, 10f, true)
        ));
    }
}
