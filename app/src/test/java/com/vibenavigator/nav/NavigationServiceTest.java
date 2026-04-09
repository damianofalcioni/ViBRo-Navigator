package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.Surface;

import org.junit.Test;

public class NavigationServiceTest {

    @Test
    public void shouldDispatchCompassUiRequiresActiveRouteVisibleUiAndInteractiveScreen() {
        assertTrue(NavigationService.shouldDispatchCompassUi(true, true, true));
        assertFalse(NavigationService.shouldDispatchCompassUi(false, true, true));
        assertFalse(NavigationService.shouldDispatchCompassUi(true, false, true));
        assertFalse(NavigationService.shouldDispatchCompassUi(true, true, false));
    }

    @Test
    public void shouldEvaluateStationaryOrientationRequiresActiveRouteAndNoReroute() {
        assertTrue(NavigationService.shouldEvaluateStationaryOrientation(true, false));
        assertFalse(NavigationService.shouldEvaluateStationaryOrientation(false, false));
        assertFalse(NavigationService.shouldEvaluateStationaryOrientation(true, true));
    }

    @Test
    public void remapHeadingDegreesForDisplayRotationCompensatesLandscapeAndNormalizes() {
        assertEquals(0.0, NavigationService.remapHeadingDegreesForDisplayRotation(0.0, Surface.ROTATION_0), 0.001);
        assertEquals(90.0, NavigationService.remapHeadingDegreesForDisplayRotation(0.0, Surface.ROTATION_90), 0.001);
        assertEquals(180.0, NavigationService.remapHeadingDegreesForDisplayRotation(0.0, Surface.ROTATION_180), 0.001);
        assertEquals(270.0, NavigationService.remapHeadingDegreesForDisplayRotation(0.0, Surface.ROTATION_270), 0.001);
        assertEquals(135.0, NavigationService.remapHeadingDegreesForDisplayRotation(45.0, Surface.ROTATION_90), 0.001);
    }
}
