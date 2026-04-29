package vibro.navigator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import vibro.navigator.nav.NavCompassState;

import java.util.Collections;

public class NavigationCompassLegendRendererTest {

    @Test
    public void resolveLegendRingDistanceMeters_normalizesAgainstOuterRing() {
        NavCompassState state = compassState(60f, null);

        assertEquals(
                60f,
                NavigationCompassLegendRenderer.resolveLegendRingDistanceMeters(state, 0.82f, 0.82f),
                0.01f
        );
        assertEquals(
                40.24f,
                NavigationCompassLegendRenderer.resolveLegendRingDistanceMeters(state, 0.55f, 0.82f),
                0.01f
        );
        assertEquals(
                20.49f,
                NavigationCompassLegendRenderer.resolveLegendRingDistanceMeters(state, 0.28f, 0.82f),
                0.01f
        );
    }

    @Test
    public void resolveLegendRingDistanceMeters_returnsZeroWithoutVisibleCompassState() {
        assertEquals(
                0f,
                NavigationCompassLegendRenderer.resolveLegendRingDistanceMeters(null, 0.82f, 0.82f),
                0.01f
        );
        assertEquals(
                0f,
                NavigationCompassLegendRenderer.resolveLegendRingDistanceMeters(compassState(0f, null), 0.82f, 0.82f),
                0.01f
        );
    }

    @Test
    public void resolvedVisibleHeadingAccuracyDegrees_clampsToVisibleRange() {
        assertNull(NavigationCompassLegendRenderer.resolvedVisibleHeadingAccuracyDegrees(
                compassState(60f, null),
                5f,
                85f
        ));
        assertNull(NavigationCompassLegendRenderer.resolvedVisibleHeadingAccuracyDegrees(
                compassState(60f, 0f),
                5f,
                85f
        ));
        assertEquals(5f, NavigationCompassLegendRenderer.resolvedVisibleHeadingAccuracyDegrees(
                compassState(60f, 2f),
                5f,
                85f
        ), 0.01f);
        assertEquals(85f, NavigationCompassLegendRenderer.resolvedVisibleHeadingAccuracyDegrees(
                compassState(60f, 120f),
                5f,
                85f
        ), 0.01f);
    }

    private static NavCompassState compassState(float visibleRadiusMeters, Float headingAccuracyDegrees) {
        return new NavCompassState(
                0f,
                headingAccuracyDegrees,
                1f,
                visibleRadiusMeters,
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
