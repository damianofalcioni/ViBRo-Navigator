package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.nav.compass.NavCompassState;

public class SurroundingStreetViewportPolicyTest {
    private final SurroundingStreetViewportPolicy policy = new SurroundingStreetViewportPolicy();

    @Test
    public void shouldShow_onlyForZoomedMovingScaleViewport() {
        assertFalse(policy.shouldShow(null));
        assertFalse(policy.shouldShow(compassState(false, 90f)));
        assertFalse(policy.shouldShow(compassState(true, 520f)));
        assertTrue(policy.shouldShow(compassState(true, 90f)));
    }

    @Test
    public void extractionRadiusAddsSmallVisibleEdgePadding() {
        assertEquals(
                114.0d,
                policy.extractionRadiusMeters(compassState(true, 90f)),
                0.0d
        );
    }

    private static NavCompassState compassState(boolean movingScaleActive, float visibleRadiusMeters) {
        return NavCompassState.fromProjectedPoints(
                0f,
                null,
                1f,
                visibleRadiusMeters,
                5f,
                movingScaleActive,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0f,
                0f,
                true
        );
    }
}
