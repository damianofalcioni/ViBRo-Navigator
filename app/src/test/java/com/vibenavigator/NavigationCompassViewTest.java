package com.vibenavigator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.nav.NavCompassState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassViewTest {

    @Test
    public void destinationPositionIsHiddenWhenOutsideVisibleRadius() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
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
        ));

        assertNull(invokeResolveDestinationPosition(view));
    }

    @Test
    public void destinationPositionIsVisibleWhenWithinVisibleRadius() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
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
        ));

        assertNotNull(invokeResolveDestinationPosition(view));
    }

    @Test
    public void legendRingDistancesAreNormalizedToOuterVisibleRing() throws Exception {
        NavigationCompassView view = new NavigationCompassView(ApplicationProvider.getApplicationContext());
        view.setCompassState(new NavCompassState(
                0f,
                null,
                1f,
                60f,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        ));

        assertEquals(60f, invokeResolveLegendRingDistanceMeters(view, 0.82f), 0.01f);
        assertEquals(40.24f, invokeResolveLegendRingDistanceMeters(view, 0.55f), 0.01f);
        assertEquals(20.49f, invokeResolveLegendRingDistanceMeters(view, 0.28f), 0.01f);
    }

    private static float[] invokeResolveDestinationPosition(NavigationCompassView view) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod(
                "resolveDestinationPosition",
                float.class,
                float.class,
                float.class,
                float.class
        );
        method.setAccessible(true);
        return (float[]) method.invoke(view, 100f, 100f, 80f, 0f);
    }

    private static float invokeResolveLegendRingDistanceMeters(NavigationCompassView view, float ringScale) throws Exception {
        Method method = NavigationCompassView.class.getDeclaredMethod(
                "resolveLegendRingDistanceMeters",
                float.class
        );
        method.setAccessible(true);
        return (float) method.invoke(view, ringScale);
    }
}
